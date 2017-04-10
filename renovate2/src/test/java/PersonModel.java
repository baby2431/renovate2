import org.junit.runners.Parameterized;

import renovate.http.HTTP;
import renovate.http.Params;

/**
 * Created by xmmc on 2017/3/29.
 */
@HTTP(method = HTTP.Method.GET,path = "www.baidu.com")
public class PersonModel {

    @Params
    String name;

    @Params
    String age;


}
