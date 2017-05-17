package request;

import renovate.http.Header;


interface Accept {
    //Accept
    @Header("Accept")
    String accept = "application/vnd.app.a1+json";

//    @Header("Content-Type")
//    String contentType = "application/json";

}
