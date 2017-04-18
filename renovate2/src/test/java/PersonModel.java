import renovate.http.FormUrlEncoded;
import renovate.http.HTTP;
import renovate.http.Ignore;
import renovate.http.Params;

/**
 * Created by xmmc on 2017/3/29.
 */
@HTTP(method = HTTP.Method.GET, path = "servlet")
@FormUrlEncoded
public class PersonModel {

    @Params
    String name;


    @Params("age")
    @Ignore
    int age;


}
