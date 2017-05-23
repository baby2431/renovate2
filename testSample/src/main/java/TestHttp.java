import okhttp3.ResponseBody;
import org.junit.Test;
import renovate.*;
import request.PersonModel;

import java.util.concurrent.CountDownLatch;

/**
 * Created by babyt on 2017/5/23.
 */
public class TestHttp {
    PersonModel p = new PersonModel();

    @Test
    public void testBody() throws InterruptedException {

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        p.age = 123123;
        p.name = "xiao wenwen";
        Renovate renovate = new Renovate.Builder().baseUrl("http://localhost:8080/").build();
        Request request = renovate.request(p);
        request(countDownLatch, request);
        countDownLatch.await();
        System.out.println("end");
    }

    private void request(CountDownLatch countDownLatch, Request request) {
        request.request().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }
}
