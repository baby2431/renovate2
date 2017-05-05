package renovate;

class DefaultCallAdapter<T> implements CallAdapter<Call<T>> {
    static final DefaultCallAdapter INSTANCE = new DefaultCallAdapter();
    @Override
    public <R> Call<T> adapt(Call<R> call) {
        return (Call<T>) call;
    }
}
