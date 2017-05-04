import okhttp3.ResponseBody;
import org.junit.Assert;
import org.junit.Test;
import renovate.Callback;
import renovate.Renovate;
import renovate.Response;
import renovate.RxAdapter;
import renovate.call.Call;
import rx.Subscriber;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class TestRxJava {

    PersonModel p = new PersonModel();

    @Test
    public void testRxJavaBase() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        p.age = 123123;
        p.name = "xiao wenwen";
        System.out.println("current thread = " + Thread.currentThread().getName());
        Renovate renovate = new Renovate.Builder().baseUrl("http://localhost:8080/").build();
        renovate.request(p).request(new RxAdapter<ResponseBody>()).subscribe(new Subscriber<ResponseBody>() {
            @Override
            public void onCompleted() {
                System.out.println("compiled");
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(ResponseBody responseBody) {
                try {
                    System.out.println(responseBody.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
        System.out.println("end");


    }



}
