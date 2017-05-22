import okhttp3.ResponseBody;
import renovate.Renovate;
import renovate.Request;
import renovate.RxAdapter;
import rx.Subscriber;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;


public class TestHeader {

    PersonModel p = new PersonModel();
    public void uploadHead() throws InterruptedException {

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        p.age = 123123;
        p.name = "xiao wenwen";

        Renovate renovate = new Renovate.Builder()
                .addHeader("Content-type", "text/html")
                .baseUrl("http://localhost:8080/").build();
        Request request = renovate.request(p);
        request.addHeader("Authorization","imrequestheader");
        request(countDownLatch, request);
        countDownLatch.await();
        System.out.println("end");
    }
    public void uploadOnlyCommonHead() throws InterruptedException {

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        p.age = 123123;
        p.name = "xiao wenwen";

        Renovate renovate = new Renovate.Builder().baseUrl("http://localhost:8080/").build();
        renovate.addHeader("Authorization", "imcommonheader");
        Request request = renovate.request(p);
        request(countDownLatch, request);
        countDownLatch.await();
        System.out.println("end");
    }

    private void request(CountDownLatch countDownLatch, Request request) {
        request.request(new RxAdapter<ResponseBody>()).subscribe(new Subscriber<ResponseBody>() {
            @Override
            public void onCompleted() {
                System.out.println("compiled");
                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                countDownLatch.countDown();
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
    }

}
