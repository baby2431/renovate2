package renovate;

import okhttp3.ResponseBody;

public class Request {
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
        return new OkHttpCall<>(objectParser, object, converter);
    }

    public <T> Call<T> request(Object object, ResponseConvert<T> converter) {
        return new OkHttpCall<>(renovate.initObject(object), object, converter);
    }

    public Call<ResponseBody> request() {
        return new OkHttpCall<>(renovate.initObject(object), object, renovate.responseBodyConverter(ResponseBody.class, objectParser.getAnnotations()));
    }
}

