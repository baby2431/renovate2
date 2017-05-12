package request;

import renovate.http.FormUrlEncoded;
import renovate.http.HTTP;
import renovate.http.Params;

/**
 * Created by babyt on 2017/5/3.
 */
@HTTP(method = HTTP.Method.POST, path = "api/member/login")
@FormUrlEncoded
public class Login implements Accept {
    @Params(value = "mobile")
    public String mobile;

    @Params(value = "password")
    public String password;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
