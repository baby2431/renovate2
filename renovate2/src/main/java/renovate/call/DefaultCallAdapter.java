package renovate.call;

import java.lang.reflect.Type;

/**
 * Created by babyt on 2017/4/27.
 */
public class DefaultCallAdapter<T>  implements  CallAdapter<Call<T>>{
    public static final DefaultCallAdapter INSTANCE = new DefaultCallAdapter();
    @Override
    public <R> Call<T> adapt(Call<R> call) {
        return (Call<T>) call;
    }
}
