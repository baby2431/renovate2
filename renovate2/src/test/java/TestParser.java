import org.junit.Test;

import renovate.Renovate;

/**
 * Created by xmmc on 2017/3/29.
 */
public class TestParser {


    @Test
    public void test(){
        PersonModel p = new PersonModel();


        new Renovate().post(p);

    }

}
