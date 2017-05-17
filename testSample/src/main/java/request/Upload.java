package request;

import renovate.http.FormUrlEncoded;
import renovate.http.HTTP;
import renovate.http.Params;
import renovate.http.PartMap;

import java.io.File;
import java.util.Map;

@HTTP(method = HTTP.Method.POST, path = "api/member/login")
@FormUrlEncoded
public class Upload {

    @PartMap
    Map<String, File> uploads;

    @Params
    String name;

    int age;

    String autograph;

    String userId;

    Friends friends;


}
