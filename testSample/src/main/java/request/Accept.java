package request;

import renovate.http.Header;

/**
 * Created by babyt on 2017/5/3.
 */

interface Accept {
    //Accept
    @Header("Accept")
    String accept = "application/vnd.app.a1+json";

//    @Header("Content-Type")
//    String contentType = "application/json";

}
