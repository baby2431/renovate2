package request;

import renovate.http.FormUrlEncoded;
import renovate.http.HTTP;
import renovate.http.Params;

@HTTP(method = HTTP.Method.POST, path = "api/member/code")
@FormUrlEncoded
public class GetCode implements Accept {
    @Params
    private String mobile;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
