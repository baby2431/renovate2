package renovate;

import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * Created by babyt on 2017/4/27.
 */
public interface ResponseConvert<T> extends Converter<ResponseBody,T> {

}
