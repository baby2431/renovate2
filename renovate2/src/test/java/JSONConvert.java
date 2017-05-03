import okhttp3.ResponseBody;
import renovate.Converter;

import java.io.IOException;

/**
 * Created by babyt on 2017/5/3.
 */
public class JSONConvert<T> implements Converter<ResponseBody, T> {
    @Override
    public T convert(ResponseBody value) throws IOException {
        return null;
    }
}
