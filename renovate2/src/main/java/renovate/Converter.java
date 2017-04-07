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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Convert objects to and from their representation in HTTP. Instances are created by {@linkplain
 * Factory a factory} which is {@linkplain Renovate.Builder#addConverterFactory(Factory) installed}
 * into the {@link Renovate} instance.
 */
public interface Converter<F, T> {
  T convert(F value) throws IOException;

  /** Creates {@link Converter} instances based on a type and target usage. */
  abstract class Factory {
    /**
     * Returns a {@link Converter} for converting an HTTP response body to {@code type}, or null if
     * {@code type} cannot be handled by this factory. This is used to create converters for
     * response types such as {@code SimpleResponse} from a {@code Call<SimpleResponse>}
     * declaration.
     */
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
        Renovate Renovate) {
      return null;
    }

    /**
     * Returns a {@link Converter} for converting {@code type} to an HTTP request body, or null if
     * {@code type} cannot be handled by this factory. This is used to create converters for types
     * specified by {@link renovate.http.Body @Body}, {@link renovate.http.Part @Part}, and {@link renovate.http.PartMap @PartMap}
     * values.
     */
    public Converter<?, RequestBody> requestBodyConverter(Type type,
        Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Renovate Renovate) {
      return null;
    }

    /**
     * Returns a {@link Converter} for converting {@code type} to a {@link String}, or null if
     * {@code type} cannot be handled by this factory. This is used to create converters for types
     * specified by {@link renovate.http.Field @Params}, {@link renovate.http.FieldMap @ParamsMap} values,
     * {@link renovate.http.Header @Header}, {@link renovate.http.HeaderMap @HeaderMap}, {@link renovate.http.Path @Path},
     * {@link renovate.http.Query @Query}, and {@link renovate.http.QueryMap @QueryMap} values.
     */
    public Converter<?, String> stringConverter(Type type, Annotation[] annotations,
        Renovate Renovate) {
      return null;
    }
  }
}
