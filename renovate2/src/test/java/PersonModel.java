import renovate.http.FormUrlEncoded;
import renovate.http.HTTP;
import renovate.http.Params;

/**
 * Created by xmmc on 2017/3/29.
 */
@HTTP(method = HTTP.Method.POST, path = "www.baidu.com")
@FormUrlEncoded
public class PersonModel {


    String name;

    @Params("age")
    int age;


}
