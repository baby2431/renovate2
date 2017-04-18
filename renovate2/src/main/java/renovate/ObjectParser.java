package renovate;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import renovate.call.CallAdapter;
import renovate.http.Body;
import renovate.http.FormUrlEncoded;
import renovate.http.HTTP;
import renovate.http.Header;
import renovate.http.HeaderMap;
import renovate.http.Ignore;
import renovate.http.Multipart;
import renovate.http.Params;
import renovate.http.ParamsMap;
import renovate.http.Part;
import renovate.http.PartMap;
import renovate.http.Path;
import renovate.http.Query;
import renovate.http.QueryMap;
import renovate.http.QueryName;
import renovate.http.Url;

/**
 * Created by xmmc on 2017/3/24.
 */

public class ObjectParser<R, T> {
    // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
    static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
    static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
    static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);
    // FIXME: 2017/4/11 pojo
    final okhttp3.Call.Factory callFactory;
    final CallAdapter<R, T> callAdapter;

    private final HttpUrl baseUrl;
    private final Converter<ResponseBody, R> responseConverter;
    private final String httpMethod;
    private final String relativeUrl;
    private final Headers headers;
    private final MediaType contentType;
    private final boolean hasBody;
    private final boolean isFormEncoded;
    private final boolean isMultipart;
    Map<Field,ParameterHandler> fieldParameterHandlerMap = new HashMap<>();

    ObjectParser(Builder<R, T> builder) {
        this.callFactory = builder.renovate.callFactory();
        this.callAdapter = builder.callAdapter;
        this.baseUrl = builder.renovate.baseUrl();
        this.responseConverter = builder.responseConverter;
        this.httpMethod = builder.httpMethod;
        this.relativeUrl = builder.relativeUrl;
        this.headers = builder.headers;
        this.contentType = builder.contentType;
        this.hasBody = builder.hasBody;
        this.isFormEncoded = builder.isFormEncoded;
        this.isMultipart = builder.isMultipart;
        this.fieldParameterHandlerMap = builder.fieldParameterHandlerMap;
    }
    // FIXME: 2017/4/11 访问属性
    /** Builds an HTTP request from method arguments. */
    public Request toRequest(Object args) throws IOException {
        OKHttpRequestBuilder requestBuilder = new OKHttpRequestBuilder(httpMethod, baseUrl, relativeUrl, headers,
                contentType, hasBody, isFormEncoded, isMultipart);

        //FIXME
//        int argumentCount = args != null ? args. : 0;
//        if (argumentCount != handlers.length) {
//            throw new IllegalArgumentException("Argument count (" + argumentCount
//                    + ") doesn't match expected count (" + handlers.length + ")");
//        }
//        String str = args.getClass().getFields();
//        int argumentCount = args.getClass().getFields().length;
//        for (int p = 0; p < argumentCount; p++) {
//            handlers[p].apply(requestBuilder, args[p]);
//        }
        try {
            //FIXME
            for (Map.Entry<Field, ParameterHandler> handlerEntry : fieldParameterHandlerMap.entrySet()) {
                ParameterHandler parameterHandler = handlerEntry.getValue();
                handlerEntry.getKey().setAccessible(true);
                parameterHandler.apply(requestBuilder, handlerEntry.getKey().get(args));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return requestBuilder.build();
    }

    public okhttp3.Call.Factory getCallFactory(){
        return callFactory;
    }


    /** Builds a method return value from an HTTP response body. */
    // FIXME: 2017/4/11 访问属性
    public R toResponse(ResponseBody body) throws IOException {
        return responseConverter.convert(body);
    }
    static final class Builder<T, R>  {
        Renovate renovate;
        Class clazz;
        Object object;
        Annotation[] objectAnnotations;
        java.lang.reflect.Field[] fields;

        Type responseType;
        boolean gotField;
        boolean gotPart;
        boolean gotBody;
        boolean gotPath;
        boolean gotQuery;
        boolean gotUrl;
        String httpMethod;
        boolean hasBody;
        boolean isFormEncoded;
        boolean isMultipart;
        String relativeUrl;
        Headers headers;
        MediaType contentType;
        Set<String> relativeUrlParamNames;

        Map<Field,ParameterHandler> fieldParameterHandlerMap = new HashMap<>();

        Converter<ResponseBody, T> responseConverter;
        CallAdapter<T, R> callAdapter;

        public Builder(Renovate renovate, Object object) {
            this.renovate = renovate;
            clazz = object.getClass();
            objectAnnotations = clazz.getAnnotations();
            fields = clazz.getDeclaredFields();
            this.object = object;

        }

        /**
         * 解析对象的配置和字段
         * @return
         */
        public ObjectParser build() {
            callAdapter = createCallAdapter();
            responseType = callAdapter.responseType();
            if (responseType == Response.class || responseType == okhttp3.Response.class) {
                throw objectError("'"
                        + Utils.getRawType(responseType).getName()
                        + "' is not a valid response body type. Did you mean ResponseBody?");
            }
            responseConverter = createResponseConverter();
            for (Annotation annotation : objectAnnotations) {
                parseHttpAnnotation(annotation);
            }
            if (httpMethod == null) {
                throw objectError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
            }
            if (!hasBody) {
                if (isMultipart) {
                    throw objectError(
                            "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
                }
                if (isFormEncoded) {
                    throw objectError("FormUrlEncoded can only be specified on HTTP methods with "
                            + "request body (e.g., @POST).");
                }
            }

            int parameterCount = fields.length;
            for (int p = 0; p < parameterCount; p++) {
//                Type parameterType = parameterTypes[p];
//                if (Utils.hasUnresolvableType(parameterType)) {
//                    throw objectError(p, "Parameter type must not include a type variable or wildcard: %s",
//                            parameterType);
//                }

                java.lang.reflect.Field field = fields[p];
                Annotation[] annotations = field.getDeclaredAnnotations();
                if (annotations == null||annotations.length == 0) {
                    System.out.println(String.format("field %s no renovate annotation found",field.getName()));
//                    throw objectError(p, "No Retrofit annotation found.");
                    continue;
                }else{
                    if(field.isAnnotationPresent(Ignore.class)){
                        System.out.println(String.format("field %s,%s is ignored",clazz.getName(),field.getName()));
                        continue;
                    }
                }
                fieldParameterHandlerMap.put(field,parseParameter(p, field.getType(), annotations,field));
            }
            if (relativeUrl == null && !gotUrl) {
                throw objectError("Missing either @%s URL or @Url parameter.", httpMethod);
            }
            if (!isFormEncoded && !isMultipart && !hasBody && gotBody) {
                throw objectError("Non-body HTTP method cannot contain @Body.");
            }
            if (isFormEncoded && !gotField) {
                throw objectError("Form-encoded method must contain at least one @Params.");
            }
            if (isMultipart && !gotPart) {
                throw objectError("Multipart method must contain at least one @Part.");
            }

            return new ObjectParser(this);
        }

        /**
         * @param p             位置
         * @param parameterType 参数类型
         * @param annotations   包含的注解
         * @return
         */
        private ParameterHandler<?> parseParameter(
                int p, Type parameterType, Annotation[] annotations,Field field) {
            ParameterHandler<?> result = null;
            for (Annotation annotation : annotations) {
                ParameterHandler<?> annotationAction = parseParameterAnnotation(
                        p, parameterType, annotations, annotation,field);
                if (annotationAction == null) {
                    continue;
                }
                if (result != null) {
                    throw objectError(p, "Multiple Renovate annotations found, only one allowed.");
                }
                result = annotationAction;
            }

            //其他的字段应该为 参数 或者是 请求体 中的内容
//            if (result == null) {
//                throw objectError(p, "No Retrofit annotation found.");
//            }

            return result;
        }

        private Converter<ResponseBody, T> createResponseConverter() {
            Annotation[] annotations = objectAnnotations; // method.getAnnotations();
            try {
                return renovate.responseBodyConverter(responseType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw objectError(e, "Unable to create converter for %s", responseType);
            }
        }

        //// FIXME: 2017/4/11
        public CallAdapter<T, R> createCallAdapter() {
            Type returnType = null;//method.getGenericReturnType();
            //// FIXME: 2017/4/11
            Method method = null;
            try {
                method = Test.class.getMethod("call");
                returnType = Test.class.getMethod("call").getGenericReturnType();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            if (Utils.hasUnresolvableType(returnType)) {
                throw objectError(
                        "Method return type must not include a type variable or wildcard: %s", returnType);
            }
            if (returnType == void.class) {
                throw objectError("Service methods cannot return void.");
            }
            Annotation[] annotations = method.getAnnotations();
            try {
                //noinspection unchecked
                return (CallAdapter<T, R>) renovate.callAdapter(returnType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw objectError(e, "Unable to create call adapter for %s", returnType);
            }
        }
        private ParameterHandler<?> parseParameterAnnotation(
                int p, Type type, Annotation[] annotations, Annotation annotation,Field field) {
            if (annotation instanceof Url) {
                if (gotUrl) {
                    throw objectError(p, "Multiple @Url method annotations found.");
                }
                if (gotPath) {
                    throw objectError(p, "@Path parameters may not be used with @Url.");
                }
                if (gotQuery) {
                    throw objectError(p, "A @Url parameter must not come after a @Query");
                }
                if (relativeUrl != null) {
                    throw objectError(p, "@Url cannot be used with @%s URL", httpMethod);
                }

                gotUrl = true;

                if (type == HttpUrl.class
                        || type == String.class
                        || type == URI.class
                        || (type instanceof Class && "android.net.Uri".equals(((Class<?>) type).getName()))) {
                    return new ParameterHandler.RelativeUrl();
                } else {
                    throw objectError(p,
                            "@Url must be okhttp3.HttpUrl, String, java.net.URI, or android.net.Uri type.");
                }

            } else if (annotation instanceof Path) {
                if (gotQuery) {
                    throw objectError(p, "A @Path parameter must not come after a @Query.");
                }
                if (gotUrl) {
                    throw objectError(p, "@Path parameters may not be used with @Url.");
                }
                if (relativeUrl == null) {
                    throw objectError(p, "@Path can only be used with relative url on @%s", httpMethod);
                }
                gotPath = true;

                Path path = (Path) annotation;
                String name = path.value();
                if("".equals(name)){
                    name = field.getName();
                }
                validatePathName(p, name);

                Converter<?, String> converter = renovate.stringConverter(type, annotations);
                return new ParameterHandler.Path<>(name, converter, path.encoded());

            } else if (annotation instanceof Query) {
                Query query = (Query) annotation;
                String name = query.value();
                if("".equals(name)){
                    name = field.getName();
                }
                boolean encoded = query.encoded();

                Class<?> rawParameterType = Utils.getRawType(type);
                gotQuery = true;
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw objectError(p, rawParameterType.getSimpleName()
                                + " must include generic type (e.g., "
                                + rawParameterType.getSimpleName()
                                + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter =
                            renovate.stringConverter(iterableType, annotations);
                    return new ParameterHandler.Query<>(name, converter, encoded).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            renovate.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.Query<>(name, converter, encoded).array();
                } else {
                    Converter<?, String> converter =
                            renovate.stringConverter(type, annotations);
                    return new ParameterHandler.Query<>(name, converter, encoded);
                }

            } else if (annotation instanceof QueryName) {
                QueryName query = (QueryName) annotation;
                boolean encoded = query.encoded();

                Class<?> rawParameterType = Utils.getRawType(type);
                gotQuery = true;
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw objectError(p, rawParameterType.getSimpleName()
                                + " must include generic type (e.g., "
                                + rawParameterType.getSimpleName()
                                + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter =
                            renovate.stringConverter(iterableType, annotations);
                    return new ParameterHandler.QueryName<>(converter, encoded).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            renovate.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.QueryName<>(converter, encoded).array();
                } else {
                    Converter<?, String> converter =
                            renovate.stringConverter(type, annotations);
                    return new ParameterHandler.QueryName<>(converter, encoded);
                }

            } else if (annotation instanceof QueryMap) {
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw objectError(p, "@QueryMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw objectError(p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw objectError(p, "@QueryMap keys must be of type String: " + keyType);
                }
                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                Converter<?, String> valueConverter =
                        renovate.stringConverter(valueType, annotations);

                return new ParameterHandler.QueryMap<>(valueConverter, ((QueryMap) annotation).encoded());

            } else if (annotation instanceof Header) {
                Header header = (Header) annotation;
                String name = header.value();
                if("".equals(name)){
                    name = field.getName();
                }
                Class<?> rawParameterType = Utils.getRawType(type);
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw objectError(p, rawParameterType.getSimpleName()
                                + " must include generic type (e.g., "
                                + rawParameterType.getSimpleName()
                                + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter =
                            renovate.stringConverter(iterableType, annotations);
                    return new ParameterHandler.Header<>(name, converter).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            renovate.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.Header<>(name, converter).array();
                } else {
                    Converter<?, String> converter =
                            renovate.stringConverter(type, annotations);
                    return new ParameterHandler.Header<>(name, converter);
                }

            } else if (annotation instanceof HeaderMap) {
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw objectError(p, "@HeaderMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw objectError(p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw objectError(p, "@HeaderMap keys must be of type String: " + keyType);
                }
                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                Converter<?, String> valueConverter =
                        renovate.stringConverter(valueType, annotations);

                return new ParameterHandler.HeaderMap<>(valueConverter);

            } else if (annotation instanceof Params) {
                if (!isFormEncoded) {
                    throw objectError(p, "@Params parameters can only be used with form encoding.");
                }
                Params params = (Params) annotation;
                String name = params.value();
                boolean encoded = params.encoded();
                if("".equals(name)){
                    name = field.getName();
                }
                gotField = true;

                Class<?> rawParameterType = Utils.getRawType(type);
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw objectError(p, rawParameterType.getSimpleName()
                                + " must include generic type (e.g., "
                                + rawParameterType.getSimpleName()
                                + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter =
                            renovate.stringConverter(iterableType, annotations);
                    return new ParameterHandler.Params<>(name, converter, encoded).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            renovate.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.Params<>(name, converter, encoded).array();
                } else {
                    Converter<?, String> converter =
                            renovate.stringConverter(type, annotations);
                    return new ParameterHandler.Params<>(name, converter, encoded);
                }

            } else if (annotation instanceof ParamsMap) {
                if (!isFormEncoded) {
                    throw objectError(p, "@ParamsMap parameters can only be used with form encoding.");
                }
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw objectError(p, "@ParamsMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw objectError(p,
                            "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;
                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw objectError(p, "@ParamsMap keys must be of type String: " + keyType);
                }
                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                Converter<?, String> valueConverter =
                        renovate.stringConverter(valueType, annotations);

                gotField = true;
                return new ParameterHandler.ParamsMap<>(valueConverter, ((ParamsMap) annotation).encoded());

            } else if (annotation instanceof Part) {
                if (!isMultipart) {
                    throw objectError(p, "@Part parameters can only be used with multipart encoding.");
                }
                Part part = (Part) annotation;
                gotPart = true;

                //// FIXME: 2017/4/10
                String partName = part.value();
                if("".equals(partName)){
                    partName = field.getName();
                }
                Class<?> rawParameterType = Utils.getRawType(type);
                if (partName.isEmpty()) {
                    if (Iterable.class.isAssignableFrom(rawParameterType)) {
                        if (!(type instanceof ParameterizedType)) {
                            throw objectError(p, rawParameterType.getSimpleName()
                                    + " must include generic type (e.g., "
                                    + rawParameterType.getSimpleName()
                                    + "<String>)");
                        }
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                        if (!MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
                            throw objectError(p,
                                    "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                        }
                        return ParameterHandler.RawPart.INSTANCE.iterable();
                    } else if (rawParameterType.isArray()) {
                        Class<?> arrayComponentType = rawParameterType.getComponentType();
                        if (!MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
                            throw objectError(p,
                                    "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                        }
                        return ParameterHandler.RawPart.INSTANCE.array();
                    } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
                        return ParameterHandler.RawPart.INSTANCE;
                    } else {
                        throw objectError(p,
                                "@Part annotation must supply a name or use MultipartBody.Part parameter type.");
                    }
                } else {
                    Headers headers =
                            Headers.of("Content-Disposition", "form-data; name=\"" + partName + "\"",
                                    "Content-Transfer-Encoding", part.encoding());

                    if (Iterable.class.isAssignableFrom(rawParameterType)) {
                        if (!(type instanceof ParameterizedType)) {
                            throw objectError(p, rawParameterType.getSimpleName()
                                    + " must include generic type (e.g., "
                                    + rawParameterType.getSimpleName()
                                    + "<String>)");
                        }
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                        if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(iterableType))) {
                            throw objectError(p, "@Part parameters using the MultipartBody.Part must not "
                                    + "include a part name in the annotation.");
                        }
                        Converter<?, RequestBody> converter =
                                renovate.requestBodyConverter(iterableType, annotations, new Annotation[]{annotation});
                        return new ParameterHandler.Part<>(headers, converter).iterable();
                    } else if (rawParameterType.isArray()) {
                        Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                        if (MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
                            throw objectError(p, "@Part parameters using the MultipartBody.Part must not "
                                    + "include a part name in the annotation.");
                        }
                        Converter<?, RequestBody> converter =
                                renovate.requestBodyConverter(arrayComponentType, annotations, new Annotation[]{annotation});
                        return new ParameterHandler.Part<>(headers, converter).array();
                    } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
                        throw objectError(p, "@Part parameters using the MultipartBody.Part must not "
                                + "include a part name in the annotation.");
                    } else {
                        Converter<?, RequestBody> converter =
                                renovate.requestBodyConverter(type, annotations, new Annotation[]{annotation});
                        return new ParameterHandler.Part<>(headers, converter);
                    }
                }

            } else if (annotation instanceof PartMap) {
                if (!isMultipart) {
                    throw objectError(p, "@PartMap parameters can only be used with multipart encoding.");
                }
                gotPart = true;
                Class<?> rawParameterType = Utils.getRawType(type);
                if (!Map.class.isAssignableFrom(rawParameterType)) {
                    throw objectError(p, "@PartMap parameter type must be Map.");
                }
                Type mapType = Utils.getSupertype(type, rawParameterType, Map.class);
                if (!(mapType instanceof ParameterizedType)) {
                    throw objectError(p, "Map must include generic types (e.g., Map<String, String>)");
                }
                ParameterizedType parameterizedType = (ParameterizedType) mapType;

                Type keyType = Utils.getParameterUpperBound(0, parameterizedType);
                if (String.class != keyType) {
                    throw objectError(p, "@PartMap keys must be of type String: " + keyType);
                }

                Type valueType = Utils.getParameterUpperBound(1, parameterizedType);
                if (MultipartBody.Part.class.isAssignableFrom(Utils.getRawType(valueType))) {
                    throw objectError(p, "@PartMap values cannot be MultipartBody.Part. "
                            + "Use @Part List<Part> or a different value type instead.");
                }

                Converter<?, RequestBody> valueConverter =
                        renovate.requestBodyConverter(valueType, annotations, new Annotation[]{annotation});

                PartMap partMap = (PartMap) annotation;
                return new ParameterHandler.PartMap<>(valueConverter, partMap.encoding());

            } else if (annotation instanceof Body) {
                if (isFormEncoded || isMultipart) {
                    throw objectError(p,
                            "@Body parameters cannot be used with form or multi-part encoding.");
                }
                if (gotBody) {
                    throw objectError(p, "Multiple @Body method annotations found.");
                }

                Converter<?, RequestBody> converter;
                try {
                    converter = renovate.requestBodyConverter(type, annotations, new Annotation[]{annotation});
                } catch (RuntimeException e) {
                    // Wide exception range because factories are user code.
                    throw objectError(e, p, "Unable to create @Body converter for %s", type);
                }
                gotBody = true;
                return new ParameterHandler.Body<>(converter);
            }

            return null; // Not a renovate annotation.
        }


        /**
         * 效验跟路径当中的restful匹配
         * @param p
         * @param name
         */
        private void validatePathName(int p, String name) {
            if (!PARAM_NAME_REGEX.matcher(name).matches()) {
                throw objectError(p, "@Path parameter name must match %s. Found: %s",
                        PARAM_URL_REGEX.pattern(), name);
            }
            // Verify URL replacement name is actually present in the URL path.
            if (!relativeUrlParamNames.contains(name)) {
                throw objectError(p, "URL \"%s\" does not contain \"{%s}\".", relativeUrl, name);
            }
        }

        /** 解析 link{ renovate.Http}
         * @param annotation
         */
        private void parseHttpAnnotation(Annotation annotation) {
            if (annotation instanceof HTTP) {
                HTTP http = (HTTP) annotation;
                parseHttpMethodAndPath(http.method().name(), http.path(), http.hasBody());
            } else if (annotation instanceof renovate.http.Headers) {
                //// FIXME: 2017/4/10 具体设计方式不明，headers
                String[] headersToParse = ((renovate.http.Headers) annotation).value();
                if (headersToParse.length == 0) {
                    throw objectError("@Headers annotation is empty.");
                }
                headers = parseHeaders(headersToParse);
            } else if (annotation instanceof Multipart) {
                if (isFormEncoded) {
                    throw objectError("Only one encoding annotation is allowed.");
                }
                isMultipart = true;
            } else if (annotation instanceof FormUrlEncoded) {
                if (isMultipart) {
                    throw objectError("Only one encoding annotation is allowed.");
                }
                isFormEncoded = true;
            }
        }

        /**
         * 设置Http的方法和路径
         * 设置请求方法和请求方式 获取到url当中的参数
         *
         * @param httpMethod
         * @param value
         * @param hasBody
         */
        private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
            if (this.httpMethod != null) {
                throw objectError("Only one HTTP method is allowed. Found: %s and %s.",
                        this.httpMethod, httpMethod);
            }
            this.httpMethod = httpMethod;
            this.hasBody = hasBody;
            if (value == null || value.equals("")) {
                return;
            }
            // Get the relative URL path and existing query string, if present.
            int question = value.indexOf('?');
            if (question != -1 && question < value.length() - 1) {
                // Ensure the query string does not have any named parameters.
                String queryParams = value.substring(question + 1);
                Matcher queryParamMatcher = PARAM_URL_REGEX.matcher(queryParams);
                if (queryParamMatcher.find()) {
                    throw objectError("URL query string \"%s\" must not have replace block. "
                            + "For dynamic query parameters use @Query.", queryParams);
                }
            }

            this.relativeUrl = value;
            this.relativeUrlParamNames = parsePathParameters(value);
        }


        /**
         * Gets the set of unique path parameters used in the given URI. If a parameter is used twice
         * in the URI, it will only show up once in the set.
         */
        static Set<String> parsePathParameters(String path) {
            Matcher m = PARAM_URL_REGEX.matcher(path);
            Set<String> patterns = new LinkedHashSet();
            while (m.find()) {
                patterns.add(m.group(1));
            }
            return patterns;
        }


        /** 解析头部
         * @param headers
         * @return
         */
        private Headers parseHeaders(String[] headers) {
            Headers.Builder builder = new Headers.Builder();
            for (String header : headers) {
                int colon = header.indexOf(':');
                if (colon == -1 || colon == 0 || colon == header.length() - 1) {
                    throw objectError(
                            "@Headers value must be in the form \"Name: Value\". Found: \"%s\"", header);
                }
                String headerName = header.substring(0, colon);
                String headerValue = header.substring(colon + 1).trim();
                if ("Content-Type".equalsIgnoreCase(headerName)) {
                    MediaType type = MediaType.parse(headerValue);
                    if (type == null) {
                        throw objectError("Malformed content type: %s", headerValue);
                    }
                    contentType = type;
                } else {
                    builder.add(headerName, headerValue);
                }
            }
            return builder.build();
        }

        private RuntimeException objectError(String message, Object... args) {
            return objectError(null, message, args);
        }

        private RuntimeException objectError(Throwable cause, String message, Object... args) {
            message = String.format(message, args);
            return new IllegalArgumentException(message
                    + "\n    for object "
                    + clazz.getSimpleName()
                    + "."
                    + clazz.getName(), cause);
        }

        private RuntimeException objectError(
                Throwable cause, int p, String message, Object... args) {
            return objectError(cause, message + " (field #" + (p + 1) + ")", args);
        }

        private RuntimeException objectError(int p, String message, Object... args) {
            return objectError(message + " (field #" + (p + 1) + ")", args);
        }


    }


    static Class<?> boxIfPrimitive(Class<?> type) {
        if (boolean.class == type) return Boolean.class;
        if (byte.class == type) return Byte.class;
        if (char.class == type) return Character.class;
        if (double.class == type) return Double.class;
        if (float.class == type) return Float.class;
        if (int.class == type) return Integer.class;
        if (long.class == type) return Long.class;
        if (short.class == type) return Short.class;
        return type;
    }


}
