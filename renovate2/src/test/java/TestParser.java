import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

import okhttp3.ResponseBody;
import renovate.Renovate;
import renovate.Response;
import renovate.http.Ignore;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by xmmc on 2017/3/29.
 */
public class TestParser {

    PersonModel p = new PersonModel();

    @Test
    public void test(){
        CountDownLatch countDownLatch = new CountDownLatch(1);
        p.age = 123123;
        p.name="xiao wenwen";
        System.out.println("current thread = "+Thread.currentThread().getName());
        Renovate renovate = new Renovate.Builder().baseUrl("http://localhost:8080/").addConverterFactory(GsonConverterFactory.create()).build();

    }

    private void print(Response<ResponseBody> response) {
        System.out.println("current thread = "+Thread.currentThread().getName());
        System.out.println(response.toString());
        try {
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(response.message());
    }

    @Test
    public void testField(){
        p.age = 123123;
        p.name="xiao wenwen";
        Class clazz = PersonModel.class;
        System.out.println(clazz.toGenericString());
        Field[] fields =  clazz.getDeclaredFields();
        for (Field field : fields) {
            ///public class PersonModel
            try {
                if(field.isAnnotationPresent(Ignore.class)){
                    System.out.println("已忽略字段 "+clazz.getName()+"."+field.getName());
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
