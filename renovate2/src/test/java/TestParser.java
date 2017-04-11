import org.junit.Test;

import java.lang.reflect.Field;

import renovate.Renovate;
import renovate.http.Ignore;
import renovate.http.Params;

/**
 * Created by xmmc on 2017/3/29.
 */
public class TestParser {

    PersonModel p = new PersonModel();

    @Test
    public void test(){
        p.age = 123123;
        p.name="xiao wenwen";
        Renovate renovate = new Renovate.Builder().baseUrl("http://www.baidu.com").build();
        renovate.request(p);
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
