import okhttp3.ResponseBody;
import org.junit.Test;
import renovate.Call;
import renovate.Callback;
import renovate.Renovate;
import renovate.Response;
import renovate.http.Ignore;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

public class TestParser {

    PersonModel p = new PersonModel();

    @Test
    public void test123() {

        Class<Test> tClass = Test.class;

        System.out.println(tClass);
    }


    @Test
    public void test() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        PersonModel p = new PersonModel();
        p.age = 123123;
        p.name = "xiao wenwen";
        System.out.println("current thread = " + Thread.currentThread().getName());
        Renovate renovate = new Renovate.Builder().baseUrl("http://localhost:8080/").build();

        renovate.request(p).request().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call call, Response response) {
                print(response);
                System.out.println("response thread = " + Thread.currentThread().getName());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                t.printStackTrace();
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        System.out.println("end");
    }

    @Test
    public void testConvert() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        p.age = 123123;
        p.name = "xiao wenwen";
        System.out.println("current thread = " + Thread.currentThread().getName());
        Renovate renovate = new Renovate.Builder().baseUrl("http://localhost:8080/").build();

        renovate.request(p).request().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call call, Response response) {
                print(response);
                System.out.println("response thread = " + Thread.currentThread().getName());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                t.printStackTrace();
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        System.out.println("end");
    }

    public void print(Response<ResponseBody> response) {
        System.out.println("current thread = " + Thread.currentThread().getName());
        System.out.println(response.toString());
        if (response.isSuccessful()) {
            System.out.println("成功");
            try {
                System.out.println(response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("fail ");
        }
        System.out.println(response.message());
    }

    @Test
    public void testField() {
        p.age = 123123;
        p.name = "xiao wenwen";
        Class clazz = PersonModel.class;
        System.out.println(clazz.toGenericString());
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            ///public class PersonModel
            try {
                if (field.isAnnotationPresent(Ignore.class)) {
                    System.out.println(" " + clazz.getName() + "." + field.getName());
                    continue;
                }
                field.setAccessible(true);
                System.out.println(field.get(p));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

    }


}
