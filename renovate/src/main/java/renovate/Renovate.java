/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package renovate;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import static java.util.Collections.unmodifiableList;
import static renovate.Utils.checkNotNull;

/**
 * Renovate adapts a Java interface to HTTP calls by using annotations on the declared methods to
 * define how requests are made. Create instances using {@linkplain Builder
 * the builder} and pass your interface to {@link #create} to generate an implementation.
 * <p>
 * For example,
 * <pre><code>
 * Renovate renovate = new Renovate.Builder()
 *     .baseUrl("https://api.example.com/")
 *     .addConverterFactory(GsonConverterFactory.create())
 *     .build();
 *
 * MyApi api = renovate.create(MyApi.class);
 * Response&lt;User&gt; user = api.getUser().execute();
 * </code></pre>
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Jake Wharton (jw@squareup.com)
 */
public final class Renovate {
  private final Map<Method, ServiceMethod<?, ?>> serviceMethodCache = new ConcurrentHashMap<>();

  final okhttp3.Call.Factory callFactory;
  final HttpUrl baseUrl;
  final List<Converter.Factory> converterFactories;
  final List<CallAdapter.Factory> adapterFactories;
  final Executor callbackExecutor;
  final boolean validateEagerly;

  Renovate(okhttp3.Call.Factory callFactory, HttpUrl baseUrl,
           List<Converter.Factory> converterFactories, List<CallAdapter.Factory> adapterFactories,
           Executor callbackExecutor, boolean validateEagerly) {
    this.callFactory = callFactory;
    this.baseUrl = baseUrl;
    this.converterFactories = unmodifiableList(converterFactories); // Defensive copy at call site.
    this.adapterFactories = unmodifiableList(adapterFactories); // Defensive copy at call site.
    this.callbackExecutor = callbackExecutor;
    this.validateEagerly = validateEagerly;
  }

  @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
  public <T> T create(final Class<T> service) {
    Utils.validateServiceInterface(service);
    if (validateEagerly) {
      eagerlyValidateMethods(service);
    }
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();

          @Override public Object invoke(Object proxy, Method method, Object[] args)
              throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            if (platform.isDefaultMethod(method)) {
              return platform.invokeDefaultMethod(method, service, proxy, args);
            }
            ServiceMethod<Object, Object> serviceMethod =
                (ServiceMethod<Object, Object>) loadServiceMethod(method);
            OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
            return serviceMethod.callAdapter.adapt(okHttpCall);
          }
        });
  }

  private void eagerlyValidateMethods(Class<?> service) {
    Platform platform = Platform.get();
    for (Method method : service.getDeclaredMethods()) {
      if (!platform.isDefaultMethod(method)) {
        loadServiceMethod(method);
      }
    }
  }

  ServiceMethod<?, ?> loadServiceMethod(Method method) {
    ServiceMethod<?, ?> result = serviceMethodCache.get(method);
    if (result != null) return result;

    synchronized (serviceMethodCache) {
      result = serviceMethodCache.get(method);
      if (result == null) {
        result = new ServiceMethod.Builder<>(this, method).build();
        serviceMethodCache.put(method, result);
      }
    }
    return result;
  }

  public okhttp3.Call.Factory callFactory() {
    return callFactory;
  }

  public HttpUrl baseUrl() {
    return baseUrl;
  }
  public List<CallAdapter.Factory> callAdapterFactories() {
    return adapterFactories;
  }

  public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
    return nextCallAdapter(null, returnType, annotations);
  }

  public CallAdapter<?, ?> nextCallAdapter(CallAdapter.Factory skipPast, Type returnType,
      Annotation[] annotations) {
    checkNotNull(returnType, "returnType == null");
    checkNotNull(annotations, "annotations == null");

    int start = adapterFactories.indexOf(skipPast) + 1;
    for (int i = start, count = adapterFactories.size(); i < count; i++) {
      CallAdapter<?, ?> adapter = adapterFactories.get(i).get(returnType, annotations, this);
      if (adapter != null) {
        return adapter;
      }
    }

    StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
        .append(returnType)
        .append(".\n");
    if (skipPast != null) {
      builder.append("  Skipped:");
      for (int i = 0; i < start; i++) {
        builder.append("\n   * ").append(adapterFactories.get(i).getClass().getName());
      }
      builder.append('\n');
    }
    builder.append("  Tried:");
    for (int i = start, count = adapterFactories.size(); i < count; i++) {
      builder.append("\n   * ").append(adapterFactories.get(i).getClass().getName());
    }
    throw new IllegalArgumentException(builder.toString());
  }

  public List<Converter.Factory> converterFactories() {
    return converterFactories;
  }

  public <T> Converter<T, RequestBody> requestBodyConverter(Type type,
      Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
    return nextRequestBodyConverter(null, type, parameterAnnotations, methodAnnotations);
  }

  public <T> Converter<T, RequestBody> nextRequestBodyConverter(Converter.Factory skipPast,
      Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
    checkNotNull(type, "type == null");
    checkNotNull(parameterAnnotations, "parameterAnnotations == null");
    checkNotNull(methodAnnotations, "methodAnnotations == null");

    int start = converterFactories.indexOf(skipPast) + 1;
    for (int i = start, count = converterFactories.size(); i < count; i++) {
      Converter.Factory factory = converterFactories.get(i);
      Converter<?, RequestBody> converter =
          factory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, this);
      if (converter != null) {
        //noinspection unchecked
        return (Converter<T, RequestBody>) converter;
      }
    }

    StringBuilder builder = new StringBuilder("Could not locate RequestBody converter for ")
        .append(type)
        .append(".\n");
    if (skipPast != null) {
      builder.append("  Skipped:");
      for (int i = 0; i < start; i++) {
        builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
      }
      builder.append('\n');
    }
    builder.append("  Tried:");
    for (int i = start, count = converterFactories.size(); i < count; i++) {
      builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
    }
    throw new IllegalArgumentException(builder.toString());
  }

  /**
   * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
   * {@linkplain #converterFactories() factories}.
   *
   * @throws IllegalArgumentException if no converter available for {@code type}.
   */
  public <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
    return nextResponseBodyConverter(null, type, annotations);
  }

  public <T> Converter<ResponseBody, T> nextResponseBodyConverter(Converter.Factory skipPast,
      Type type, Annotation[] annotations) {
    checkNotNull(type, "type == null");
    checkNotNull(annotations, "annotations == null");

    int start = converterFactories.indexOf(skipPast) + 1;
    for (int i = start, count = converterFactories.size(); i < count; i++) {
      Converter<ResponseBody, ?> converter =
          converterFactories.get(i).responseBodyConverter(type, annotations, this);
      if (converter != null) {
        //noinspection unchecked
        return (Converter<ResponseBody, T>) converter;
      }
    }

    StringBuilder builder = new StringBuilder("Could not locate ResponseBody converter for ")
        .append(type)
        .append(".\n");
    if (skipPast != null) {
      builder.append("  Skipped:");
      for (int i = 0; i < start; i++) {
        builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
      }
      builder.append('\n');
    }
    builder.append("  Tried:");
    for (int i = start, count = converterFactories.size(); i < count; i++) {
      builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
    }
    throw new IllegalArgumentException(builder.toString());
  }

  /**
   * Returns a {@link Converter} for {@code type} to {@link String} from the available
   * {@linkplain #converterFactories() factories}.
   */
  public <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
    checkNotNull(type, "type == null");
    checkNotNull(annotations, "annotations == null");

    for (int i = 0, count = converterFactories.size(); i < count; i++) {
      Converter<?, String> converter =
          converterFactories.get(i).stringConverter(type, annotations, this);
      if (converter != null) {
        //noinspection unchecked
        return (Converter<T, String>) converter;
      }
    }

    // Nothing matched. Resort to default converter which just calls toString().
    //noinspection unchecked
    return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
  }

  /**
   * The executor used for {@link Callback} methods on a {@link Call}. This may be {@code null},
   * in which case callbacks should be made synchronously on the background thread.
   */
  public Executor callbackExecutor() {
    return callbackExecutor;
  }

  public Builder newBuilder() {
    return new Builder(this);
  }

  /**
   * Build a new {@link Renovate}.
   * <p>
   * Calling {@link #baseUrl} is required before calling {@link #build()}. All other methods
   * are optional.
   */
  public static final class Builder {
    private final Platform platform;
    private okhttp3.Call.Factory callFactory;
    private HttpUrl baseUrl;
    private final List<Converter.Factory> converterFactories = new ArrayList<>();
    private final List<CallAdapter.Factory> adapterFactories = new ArrayList<>();
    private Executor callbackExecutor;
    private boolean validateEagerly;

    Builder(Platform platform) {
      this.platform = platform;
      // Add the built-in converter factory first. This prevents overriding its behavior but also
      // ensures correct behavior when using converters that consume all types.
      converterFactories.add(new BuiltInConverters());
    }

    public Builder() {
      this(Platform.get());
    }

    Builder(Renovate renovate) {
      platform = Platform.get();
      callFactory = renovate.callFactory;
      baseUrl = renovate.baseUrl;
      converterFactories.addAll(renovate.converterFactories);
      adapterFactories.addAll(renovate.adapterFactories);
      // Remove the default, platform-aware call adapter added by build().
      adapterFactories.remove(adapterFactories.size() - 1);
      callbackExecutor = renovate.callbackExecutor;
      validateEagerly = renovate.validateEagerly;
    }

    /**
     * The HTTP client used for requests.
     * <p>
     * This is a convenience method for calling {@link #callFactory}.
     */
    public Builder client(OkHttpClient client) {
      return callFactory(checkNotNull(client, "client == null"));
    }

    /**
     * Specify a custom call factory for creating {@link Call} instances.
     * <p>
     * Note: Calling {@link #client} automatically sets this value.
     */
    public Builder callFactory(okhttp3.Call.Factory factory) {
      this.callFactory = checkNotNull(factory, "factory == null");
      return this;
    }

    /**
     * Set the API base URL.
     *
     * @see #baseUrl(HttpUrl)
     */
    public Builder baseUrl(String baseUrl) {
      checkNotNull(baseUrl, "baseUrl == null");
      HttpUrl httpUrl = HttpUrl.parse(baseUrl);
      if (httpUrl == null) {
        throw new IllegalArgumentException("Illegal URL: " + baseUrl);
      }
      return baseUrl(httpUrl);
    }

    public Builder baseUrl(HttpUrl baseUrl) {
      checkNotNull(baseUrl, "baseUrl == null");
      List<String> pathSegments = baseUrl.pathSegments();
      if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
        throw new IllegalArgumentException("baseUrl must end in /: " + baseUrl);
      }
      this.baseUrl = baseUrl;
      return this;
    }

    /** Add converter factory for serialization and deserialization of objects. */
    public Builder addConverterFactory(Converter.Factory factory) {
      converterFactories.add(checkNotNull(factory, "factory == null"));
      return this;
    }

    /**
     * Add a call adapter factory for supporting service method return types other than {@link
     * Call}.
     */
    public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
      adapterFactories.add(checkNotNull(factory, "factory == null"));
      return this;
    }

    /**
     * The executor on which {@link Callback} methods are invoked when returning {@link Call} from
     * your service method.
     * <p>
     * Note: {@code executor} is not used for {@linkplain #addCallAdapterFactory custom method
     * return types}.
     */
    public Builder callbackExecutor(Executor executor) {
      this.callbackExecutor = checkNotNull(executor, "executor == null");
      return this;
    }

    /**
     * When calling {@link #create} on the resulting {@link Renovate} instance, eagerly validate
     * the configuration of all methods in the supplied interface.
     */
    public Builder validateEagerly(boolean validateEagerly) {
      this.validateEagerly = validateEagerly;
      return this;
    }

    /**
     * Create the {@link Renovate} instance using the configured values.
     * <p>
     * Note: If neither {@link #client} nor {@link #callFactory} is called a default {@link
     * OkHttpClient} will be created and used.
     */
    public Renovate build() {
      if (baseUrl == null) {
        throw new IllegalStateException("Base URL required.");
      }

      okhttp3.Call.Factory callFactory = this.callFactory;
      if (callFactory == null) {
        callFactory = new OkHttpClient();
      }

      Executor callbackExecutor = this.callbackExecutor;
      if (callbackExecutor == null) {
        callbackExecutor = platform.defaultCallbackExecutor();
      }

      // Make a defensive copy of the adapters and add the default Call adapter.
      List<CallAdapter.Factory> adapterFactories = new ArrayList<>(this.adapterFactories);
      adapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));

      // Make a defensive copy of the converters.
      List<Converter.Factory> converterFactories = new ArrayList<>(this.converterFactories);

      return new Renovate(callFactory, baseUrl, converterFactories, adapterFactories,
          callbackExecutor, validateEagerly);
    }
  }
}
