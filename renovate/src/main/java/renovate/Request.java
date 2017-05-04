package renovate;

import okhttp3.ResponseBody;
import renovate.call.Call;
import renovate.call.CallAdapter;
import renovate.call.OkHttpCall;

public class Request {
    private ObjectParser objectParser;
    private Converter<ResponseBody, ?> responseBodyConverter;
    private CallAdapter<?> adapter;
    private Object object;
    private Renovate renovate;

    public Request(ObjectParser objectParser, Converter<ResponseBody, ?> responseBodyConverter, CallAdapter<?> adapter) {
        this.objectParser = objectParser;
        this.responseBodyConverter = responseBodyConverter;
        this.adapter = adapter;
    }


    public Request(Renovate renovate, ObjectParser objectParser, Object object) {
        this.renovate = renovate;
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

    public Call<ResponseBody> request() {
        return new OkHttpCall<ResponseBody>(objectParser, object, renovate.responseBodyConverter(ResponseBody.class, objectParser.getAnnotations()));
    }
}

