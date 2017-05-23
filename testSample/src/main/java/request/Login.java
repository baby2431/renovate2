package request;

import renovate.http.FormUrlEncoded;
import renovate.http.HTTP;
import renovate.http.Params;

@HTTP(method = HTTP.Method.POST, path = "/Servlet")
@FormUrlEncoded
public class Login {
    @Params("mobile")
    public String mobile;

    @Params("password")
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
