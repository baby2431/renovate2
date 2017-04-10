import org.junit.Test;

import renovate.Renovate;

/**
 * Created by xmmc on 2017/3/29.
 */
public class TestParser {


    @Test
    public void test(){
        PersonModel p = new PersonModel();
        p.age = "123123";
        p.name="xiao wenwen";

        Renovate renovate = new Renovate.Builder().baseUrl("http://www.baidu.com").build();
        renovate.post(p);
    }

}
