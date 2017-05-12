/*
 * Copyright (C) 2017 Sirius, Inc.
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


import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;

import static java.util.Collections.unmodifiableList;
import static renovate.Utils.checkNotNull;


/**
 * Renovate is based on object oriented ,Http request and response ibrary
 *
 * @author Sirius. contact : email:baby2431@gmail.com QQ:243107006
 * @version 0.1
 */
public class Renovate {

    private okhttp3.Call.Factory callFactory;
    private HttpUrl baseUrl;
    private List<Converter.Factory> converterFactories;
    private Executor callbackExecutor;
    private boolean validateEagerly;
    private WeakHashMap<Class, ObjectParser> clazzOP = new WeakHashMap<>();

    Renovate(okhttp3.Call.Factory callFactory, HttpUrl baseUrl,
             List<Converter.Factory> converterFactories,
             Executor callbackExecutor, boolean validateEagerly, Platform platform) {
        this.callFactory = callFactory;
        this.baseUrl = baseUrl;
        this.converterFactories = unmodifiableList(converterFactories); // Defensive copy at call site.
        this.callbackExecutor = callbackExecutor;
        this.validateEagerly = validateEagerly;
    }

    public okhttp3.Call.Factory callFactory() {
        return callFactory;
    }


    ObjectParser initObject(final Object object) {

        if (clazzOP.containsKey(object.getClass())) {
            return clazzOP.get(object.getClass());
        } else {
            clazzOP.put(object.getClass(), new ObjectParser.Builder(Renovate.this, object).build());
        }

        return clazzOP.get(object.getClass());
    }

    public Request request(Object object) {
        ObjectParser objectParser = initObject(object);
        return new Request(this, objectParser, object);
    }

    public Request request() {
        return new Request(this);
    }

    public List<Converter.Factory> converterFactories() {
        return converterFactories;
    }

    <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(type, annotations);
    }


    <T> Converter<ResponseBody, T> nextResponseBodyConverter(
            Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        int start = 0;
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
        builder.append("  Skipped:");
        for (int i = 0; i < start; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        builder.append('\n');
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    <T> Converter<T, RequestBody> requestBodyConverter(Type type,
                                                       Annotation[] parameterAnnotations) {
        return nextRequestBodyConverter(null, type, parameterAnnotations);
    }

    <T> Converter<T, RequestBody> nextRequestBodyConverter(Converter.Factory skipPast,
                                                           Type type, Annotation[] parameterAnnotations) {
        checkNotNull(type, "type == null");
        checkNotNull(parameterAnnotations, "parameterAnnotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter.Factory factory = converterFactories.get(i);
            Converter<?, RequestBody> converter =
                    factory.requestBodyConverter(type, parameterAnnotations, this);
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

    public HttpUrl baseUrl() {
        return baseUrl;
    }

    <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        for (Converter.Factory converterFactory : converterFactories) {
            Converter<?, String> converter =
                    converterFactory.stringConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, String>) converter;
            }
        }

        // Nothing matched. Resort to default converter which just calls toString().
        //noinspection unchecked
        return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }

    public static final class Builder {
        private final Platform platform;
        private final List<Converter.Factory> converterFactories = new ArrayList<>();
        private okhttp3.Call.Factory callFactory;
        private HttpUrl baseUrl;
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

        Builder(Renovate retrofit) {
            platform = Platform.get();
            callFactory = retrofit.callFactory;
            baseUrl = retrofit.baseUrl;
            converterFactories.addAll(retrofit.converterFactories);
            // Remove the default, platform-aware call adapter added by build().
            callbackExecutor = retrofit.callbackExecutor;
            validateEagerly = retrofit.validateEagerly;
        }

        public Builder client(OkHttpClient client) {
            return callFactory(checkNotNull(client, "client == null"));
        }

        public Builder callFactory(okhttp3.Call.Factory factory) {
            this.callFactory = checkNotNull(factory, "factory == null");
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null");
            if (!baseUrl.endsWith("\\")) {
                baseUrl += "\\";
            }
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

        public Builder addConverterFactory(Converter.Factory factory) {
            converterFactories.add(checkNotNull(factory, "factory == null"));
            return this;
        }


        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = checkNotNull(executor, "executor == null");
            return this;
        }


        public Builder validateEagerly(boolean validateEagerly) {
            this.validateEagerly = validateEagerly;
            return this;
        }


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

            // Make a defensive copy of the converters.
            List<Converter.Factory> converterFactories = new ArrayList<>(this.converterFactories);

            return new Renovate(callFactory, baseUrl, converterFactories,
                    callbackExecutor, validateEagerly, platform);
        }
    }

}
