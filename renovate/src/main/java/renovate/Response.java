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
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;

public final class Response<T> {
  private final okhttp3.Response rawResponse;
  private final T body;
  private final ResponseBody errorBody;

  private Response(okhttp3.Response rawResponse, T body, ResponseBody errorBody) {
    this.rawResponse = rawResponse;
    this.body = body;
    this.errorBody = errorBody;
  }

  public static <T> Response<T> success(T body) {
    return success(body, new okhttp3.Response.Builder() //
        .code(200)
        .message("OK")
        .protocol(Protocol.HTTP_1_1)
        .request(new Request.Builder().url("http://localhost/").build())
        .build());
  }

  public static <T> Response<T> success(T body, Headers headers) {
    if (headers == null) throw new NullPointerException("headers == null");
    return success(body, new okhttp3.Response.Builder() //
        .code(200)
        .message("OK")
        .protocol(Protocol.HTTP_1_1)
        .headers(headers)
        .request(new Request.Builder().url("http://localhost/").build())
        .build());
  }

  public static <T> Response<T> success(T body, okhttp3.Response rawResponse) {
    if (rawResponse == null) throw new NullPointerException("rawResponse == null");
    if (!rawResponse.isSuccessful()) {
      throw new IllegalArgumentException("rawResponse must be successful response");
    }
    return new Response<>(rawResponse, body, null);
  }

  public static <T> Response<T> error(int code, ResponseBody body) {
    if (code < 400) throw new IllegalArgumentException("code < 400: " + code);
    return error(body, new okhttp3.Response.Builder() //
        .code(code)
        .protocol(Protocol.HTTP_1_1)
        .request(new Request.Builder().url("http://localhost/").build())
        .build());
  }

  public static <T> Response<T> error(ResponseBody body, okhttp3.Response rawResponse) {
    if (body == null) throw new NullPointerException("body == null");
    if (rawResponse == null) throw new NullPointerException("rawResponse == null");
    if (rawResponse.isSuccessful()) {
      throw new IllegalArgumentException("rawResponse should not be successful response");
    }
    return new Response<>(rawResponse, null, body);
  }

  public okhttp3.Response raw() {
    return rawResponse;
  }

  public int code() {
    return rawResponse.code();
  }

  public String message() {
    return rawResponse.message();
  }

  public Headers headers() {
    return rawResponse.headers();
  }

  public boolean isSuccessful() {
    return rawResponse.isSuccessful();
  }

  public T body() {
    return body;
  }

  public ResponseBody errorBody() {
    return errorBody;
  }

  @Override public String toString() {
    return rawResponse.toString();
  }
}
