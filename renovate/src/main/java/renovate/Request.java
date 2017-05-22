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

import okhttp3.ResponseBody;

public class Request extends HeaderCURD {
    private ObjectParser objectParser;
    private Object object;
    private Renovate renovate;

    Request(Renovate renovate) {
        this.renovate = renovate;
    }

    Request(Renovate renovate, ObjectParser objectParser, Object object) {
        this(renovate);
        this.objectParser = objectParser;
        this.object = object;
    }

    public <T, E> E request(ResponseConvert<T> converter, CallAdapter<E> adapter) {
        return adapter.adapt(request(converter));
    }

    public <E> E request(CallAdapter<E> adapter) {
        return adapter.adapt(request());
    }

    public <T> Call<T> request(ResponseConvert<T> converter) {
        return new OkHttpCall<>(objectParser, object, converter,headerMap);
    }

    public <T> Call<T> request(Object object, ResponseConvert<T> converter) {
        return new OkHttpCall<>(renovate.initObject(object), object, converter,headerMap);
    }

    public Call<ResponseBody> request() {
        return new OkHttpCall<>(renovate.initObject(object), object, renovate.responseBodyConverter(ResponseBody.class, objectParser.getAnnotations()),headerMap);
    }
}

