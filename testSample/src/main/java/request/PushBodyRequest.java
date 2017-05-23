package request;

import renovate.http.Body;
import renovate.http.HTTP;
import response.User;

/**
 * Created by babyt on 2017/5/23.
 */
@HTTP(method = HTTP.Method.POST, path = "/Servlet")
public class PushBodyRequest {

    @Body
    User friends;
}
