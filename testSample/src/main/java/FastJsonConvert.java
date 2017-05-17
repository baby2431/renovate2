import com.alibaba.fastjson.JSON;
import okhttp3.ResponseBody;
import renovate.ResponseConvert;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


public class FastJsonConvert<T> implements ResponseConvert<T> {

    private Class<T> t;
    private Type rawType;

    public FastJsonConvert(Class<T> t) {
        this.t = t;
    }

    public FastJsonConvert() {

        Type genType = getClass().getGenericSuperclass();
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        Type type = params[0];

        if (!(type instanceof ParameterizedType)) throw new IllegalStateException("没有填写泛型参数");
        rawType = ((ParameterizedType) type).getRawType();
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        //不同的业务，这里的代码逻辑都不一样，如果你不修改，那么基本不可用

        String string = value.string();
        System.out.println(string);
        if (rawType != null) {
            return JSON.parseObject(string, rawType);
        } else {
            return JSON.parseObject(string, t);
        }

    }
}
