import org.junit.Test;

import renovate.Renovate;
import renovate.http.Params;

/**
 * Created by xmmc on 2017/3/29.
 */
public class TestParser {


    @Test
    public void test(){
        PersonModel p = new PersonModel();

        Renovate renovate = new Renovate.Builder().baseUrl("http://baidu.com").build();
        renovate.post(p);
    }

}
