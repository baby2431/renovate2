import renovate.http.HTTP;
import renovate.http.Ignore;
import renovate.http.Params;
import renovate.http.Query;

/**
 * Created by xmmc on 2017/3/29.
 */
@HTTP(method = HTTP.Method.GET, path = "Servlet")
public class PersonModel {

    @Query
    String name;


    @Params("age")
    @Ignore
    int age;


}
