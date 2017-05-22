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

import okhttp3.Headers;
import okhttp3.*;
import okhttp3.Request;
import renovate.http.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ObjectParser {
    // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
    private static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
    private static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
    private static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);
    private final HttpUrl baseUrl;
    private final String httpMethod;
    private final String relativeUrl;
    private final Headers headers;
    private final MediaType contentType;
    private final boolean hasBody;
    private final boolean isFormEncoded;
    private final boolean isMultipart;
    private okhttp3.Call.Factory callFactory;
    private Map<Field, ParameterHandler> fieldParameterHandlerMap = new HashMap<>();
    private Class clazz;
    private Renovate renovate;

    ObjectParser(Builder builder) {
        this.callFactory = builder.renovate.callFactory();
        this.baseUrl = builder.renovate.baseUrl();
        this.httpMethod = builder.httpMethod;
        this.relativeUrl = builder.relativeUrl;
        this.headers = builder.headers;
        this.contentType = builder.contentType;
        this.hasBody = builder.hasBody;
        this.isFormEncoded = builder.isFormEncoded;
        this.isMultipart = builder.isMultipart;
        this.fieldParameterHandlerMap = builder.fieldParameterHandlerMap;
        this.clazz = builder.clazz;
        this.renovate = builder.renovate;
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

    Annotation[] getAnnotations() {
        return clazz.getAnnotations();
    }

    Request toRequest(Object args, Map<String, String> headerMap) throws IOException {


        OKHttpRequestBuilder requestBuilder = new OKHttpRequestBuilder(httpMethod, baseUrl, relativeUrl, headers,
                contentType, hasBody, isFormEncoded, isMultipart);
        try {
            for (Map.Entry<Field, ParameterHandler> handlerEntry : fieldParameterHandlerMap.entrySet()) {
                ParameterHandler parameterHandler = handlerEntry.getValue();
                handlerEntry.getKey().setAccessible(true);
                parameterHandler.apply(requestBuilder, handlerEntry.getKey().get(args));
            }
            if (headerMap == null) {
                headerMap = new HashMap<>();
            }
            headerMap.putAll(renovate.getHeader());
            for (String s : headerMap.keySet()) {
                requestBuilder.addHeader(s,headerMap.get(s));
            }


        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return requestBuilder.build();
    }

    okhttp3.Call.Factory getCallFactory() {
        return callFactory;
    }

    static final class Builder {
        Renovate renovate;
        Class clazz;
        Object object;
        Annotation[] objectAnnotations;
        Field[] fields;
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
        Map<Field, ParameterHandler> fieldParameterHandlerMap = new HashMap<>();

        Builder(Renovate renovate, Object object) {
            this.renovate = renovate;
            clazz = object.getClass();
            objectAnnotations = clazz.getAnnotations();
            Field[] totalField = new Field[clazz.getDeclaredFields().length + clazz.getFields().length];
            System.arraycopy(clazz.getDeclaredFields(), 0, totalField, 0, clazz.getDeclaredFields().length);
            System.arraycopy(clazz.getFields(), 0, totalField, clazz.getDeclaredFields().length, clazz.getFields().length);
            fields = totalField;
            this.object = object;
        }

        static Set<String> parsePathParameters(String path) {
            Matcher m = PARAM_URL_REGEX.matcher(path);
            Set<String> patterns = new LinkedHashSet<>();
            while (m.find()) {
                patterns.add(m.group(1));
            }
            return patterns;
        }

        ObjectParser build() {
            for (Annotation annotation : objectAnnotations) {
                parseHttpAnnotation(annotation);
            }
            if (httpMethod == null) {
                throw objectError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
            }
//            if hasBody is true then skip ,if false then isMultipart and isFromEncoded not true
            if (!hasBody) { //if false ,no body,but has multipart or fromEncoded append err
                if (isMultipart) {
                    throw objectError(
                            "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
                }
                if (isFormEncoded) {
                    throw objectError("FormUrlEncoded can only be specified on HTTP methods with "
                            + "request body (e.g., @POST).");
                }
            }

            int fieldCount = fields.length;
            for (int p = 0; p < fieldCount; p++) {
                Field field = fields[p];
                Annotation[] annotations = field.getDeclaredAnnotations();
                if (annotations == null || annotations.length == 0) {
                    System.out.println(String.format("field %s no renovate annotation found", field.getName()));
                    continue;
                } else if (field.isAnnotationPresent(Ignore.class)) {
                    System.out.println(String.format("field %s,%s is ignored", clazz.getName(), field.getName()));
                    continue;
                }
                fieldParameterHandlerMap.put(field, parseParameter(p, field.getType(), annotations, field));
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

        private ParameterHandler<?> parseParameter(
                int p, Type parameterType, Annotation[] annotations, Field field) {
            ParameterHandler<?> result = null;
            for (Annotation annotation : annotations) {
                ParameterHandler<?> annotationAction = parseClassAnnotation(
                        p, parameterType, annotations, annotation, field);
                if (annotationAction == null) {
                    continue;
                }
                if (result != null) {
                    throw objectError(p, "Multiple Renovate annotations found, only one allowed.");
                }
                result = annotationAction;
            }
            return result;
        }

        private ParameterHandler<?> parseClassAnnotation(
                int p, Type type, Annotation[] annotations, Annotation annotation, Field field) {
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
                if ("".equals(name)) {
                    name = field.getName();
                }
                validatePathName(p, name);

                Converter<?, String> converter = renovate.stringConverter(type, annotations);
                return new ParameterHandler.Path<>(name, converter, path.encoded());

            } else if (annotation instanceof Query) {
                Query query = (Query) annotation;
                String name = query.value();
                if ("".equals(name)) {
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
                if ("".equals(name)) {
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
                if (!isFormEncoded) { // need @FormEncoded
                    throw objectError(p, "@Params parameters can only be used with form encoding.");
                }
                Params params = (Params) annotation;
                String name = params.value();
                boolean encoded = params.encoded();
                if ("".equals(name)) {
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
                String partName = part.value();
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
                                renovate.requestBodyConverter(iterableType, annotations);
                        return new ParameterHandler.Part<>(headers, converter).iterable();
                    } else if (rawParameterType.isArray()) {
                        Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                        if (MultipartBody.Part.class.isAssignableFrom(arrayComponentType)) {
                            throw objectError(p, "@Part parameters using the MultipartBody.Part must not "
                                    + "include a part name in the annotation.");
                        }
                        Converter<?, RequestBody> converter =
                                renovate.requestBodyConverter(arrayComponentType, annotations);
                        return new ParameterHandler.Part<>(headers, converter).array();
                    } else if (MultipartBody.Part.class.isAssignableFrom(rawParameterType)) {
                        throw objectError(p, "@Part parameters using the MultipartBody.Part must not "
                                + "include a part name in the annotation.");
                    } else {
                        Converter<?, RequestBody> converter =
                                renovate.requestBodyConverter(type, annotations);
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
                        renovate.requestBodyConverter(valueType, annotations);

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
                    converter = renovate.requestBodyConverter(type, annotations);
                } catch (RuntimeException e) {
                    // Wide exception range because factories are user code.
                    throw objectError(e, p, "Unable to create @Body converter for %s", type);
                }
                gotBody = true;
                return new ParameterHandler.Body<>(converter);
            }

            return null; // Not a renovate annotation.
        }

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

        private void parseHttpAnnotation(Annotation annotation) {
            if (annotation instanceof HTTP) {
                HTTP http = (HTTP) annotation;
                parseHttpMethodAndPath(http.method(), http.path());
            } else if (annotation instanceof renovate.http.Headers) {
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

        private void parseHttpMethodAndPath(HTTP.Method httpMethod, String value) {
            if (this.httpMethod != null) {
                throw objectError("Only one HTTP method is allowed. Found: %s and %s.",
                        this.httpMethod, httpMethod);
            }
            this.httpMethod = httpMethod.name();
            switch (httpMethod) {
                case DELETE:
                    this.hasBody = false;
                    break;
                case GET:
                    this.hasBody = false;
                    break;
                case HEAD:
                    this.hasBody = false;
                    break;
                case PATCH:
                    this.hasBody = true;
                    break;
                case POST:
                    this.hasBody = true;
                    break;
                case PUT:
                    this.hasBody = true;
                    break;
                case OPTIONS:
                    this.hasBody = false;
                    break;
            }

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


}
