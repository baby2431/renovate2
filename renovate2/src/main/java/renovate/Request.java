package renovate;

import okhttp3.ResponseBody;
import renovate.call.Call;
import renovate.call.CallAdapter;
import renovate.call.OkHttpCall;

/**
 * 组成Request所有所需对象
 * Created by babyt on 2017/4/27.
 */
public class Request {
    ObjectParser objectParser;
    Converter<ResponseBody, ?> responseBodyConverter;
    CallAdapter<?> adapter;

    public Request(ObjectParser objectParser, Converter<ResponseBody, ?> responseBodyConverter, CallAdapter<?> adapter) {
        this.objectParser = objectParser;
        this.responseBodyConverter = responseBodyConverter;
        this.adapter = adapter;
    }


    public Request(ObjectParser objectParser) {
        this.objectParser = objectParser;
    }

    public void request(){

    }



    /**
     * Rx支持,获取同步call对象
     */
    public <T, E> E request(ResponseConvert<T> converter, CallAdapter<E> adapter) {
        return null;
    }

    /**
     * Rx支持,获取同步call对象
     */
    public <T> Call<T> request(ResponseConvert<T> converter) {
        return null;
    }

}
