package renovate;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import renovate.http.FormUrlEncoded;
import renovate.http.HTTP;
import renovate.http.Multipart;

/**
 * Created by xmmc on 2017/3/24.
 */

public class ObjectParser {
    private  HttpUrl baseUrl = null;
    private  String httpMethod = null;
    private  String relativeUrl = null;
    private  Headers headers = null;
    private  MediaType contentType = null;
    private  boolean hasBody = false;
    private  boolean isFormEncoded = false;
    private  boolean isMultipart = false;
    static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
    static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
    static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);
    Class clazz;
    Set<String> relativeUrlParamNames;







    static final class Build{
        Class clazz;
        Object object;
        Annotation[] objectAnnotations;
        String httpMethod;
        boolean hasBody;
        String relativeUrl;
        Set<String> relativeUrlParamNames;
        MediaType contentType;
        Headers headers;
        boolean isFormEncoded;
        boolean isMultipart;

        public Build(Object object){
            clazz = object.getClass();
            objectAnnotations = clazz.getAnnotations();
            this.object = object;
        }

        public ObjectParser build(){

            for(Annotation annotation:objectAnnotations){
                parseAnnotation(annotation);
            }

            return new ObjectParser();
        }

        private void parseAnnotation(Annotation annotation){
             if (annotation instanceof HTTP) {
                HTTP http = (HTTP) annotation;
                parseHttpMethodAndPath(http.method(), http.path(), http.hasBody());
            } else if (annotation instanceof renovate.http.Headers) {
                String[] headersToParse = ((renovate.http.Headers) annotation).value();
                if (headersToParse.length == 0) {
                    throw methodError("@Headers annotation is empty.");
                }
                headers = parseHeaders(headersToParse);
            } else if (annotation instanceof Multipart) {
                if (isFormEncoded) {
                    throw methodError("Only one encoding annotation is allowed.");
                }
                isMultipart = true;
            } else if (annotation instanceof FormUrlEncoded) {
                if (isMultipart) {
                    throw methodError("Only one encoding annotation is allowed.");
                }
                isFormEncoded = true;
            }
        }

        /**
         * 设置请求方法和请求方式 获取到url当中的参数
         * @param httpMethod
         * @param value
         * @param hasBody
         */
        private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
            if (this.httpMethod != null) {
                throw methodError("Only one HTTP method is allowed. Found: %s and %s.",
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
                    throw methodError("URL query string \"%s\" must not have replace block. "
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


        private Headers parseHeaders(String[] headers) {
            Headers.Builder builder = new Headers.Builder();
            for (String header : headers) {
                int colon = header.indexOf(':');
                if (colon == -1 || colon == 0 || colon == header.length() - 1) {
                    throw methodError(
                            "@Headers value must be in the form \"Name: Value\". Found: \"%s\"", header);
                }
                String headerName = header.substring(0, colon);
                String headerValue = header.substring(colon + 1).trim();
                if ("Content-Type".equalsIgnoreCase(headerName)) {
                    MediaType type = MediaType.parse(headerValue);
                    if (type == null) {
                        throw methodError("Malformed content type: %s", headerValue);
                    }
                    contentType = type;
                } else {
                    builder.add(headerName, headerValue);
                }
            }
            return builder.build();
        }

        private RuntimeException methodError(String message, Object... args) {
            return methodError(null, message, args);
        }

        private RuntimeException methodError(Throwable cause, String message, Object... args) {
            message = String.format(message, args);
            return new IllegalArgumentException(message
                    + "\n    for method "
                    + clazz.getSimpleName()
                    + "."
                    + clazz.getName(), cause);
        }

        private RuntimeException parameterError(
                Throwable cause, int p, String message, Object... args) {
            return methodError(cause, message + " (parameter #" + (p + 1) + ")", args);
        }

        private RuntimeException parameterError(int p, String message, Object... args) {
            return methodError(message + " (parameter #" + (p + 1) + ")", args);
        }


    }











}
