package request;

import renovate.http.HeaderMap;
import renovate.http.Headers;

import java.util.HashMap;
import java.util.Map;

@Headers({"Header: application/vnd.github.v3.full+json", "User-Agent: Retrofit-Sample-App"})
public interface ImplHeader {
    //Header
    @renovate.http.Header("Header")
    String accept = "application/vnd.app.a1+json";

    @HeaderMap
    Map<String, String> headerMap = new HashMap<String, String>() {
        {
            put("Cache-Control", "no-cache");
            put("Referer", " http://download.google.com/");
        }
    };


}
