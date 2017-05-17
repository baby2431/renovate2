package request;

import renovate.http.HTTP;
import renovate.http.Params;

@HTTP(method = HTTP.Method.POST, path = "api/member/register")
public class Register extends Login {
    @Params
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
