package response;


/**
 * Created by babyt on 2017/5/4.
 */

public class LoginResponse extends BaseResponse {


    /**
     * data : {"username":"13306992629","signature":"","thumb":"","gender":0,"age":0,"pious":0,"token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOjE0LCJpc3MiOiJodHRwOlwvXC95eGJhY2suZnR2Y24uY29tOjUwNTFcL2FwaVwvbWVtYmVyXC9sb2dpbiIsImlhdCI6MTQ5NDAwNDgyNywiZXhwIjoxNDk1MjE0NDI3LCJuYmYiOjE0OTQwMDQ4MjcsImp0aSI6IktOeHFkcUU3RXliSjk5TUQifQ.kwbS8ydgiiVpozMih_dKLxXXxIo0_JS85qArJIsyeZo"}
     */

    private User data;

    public boolean isValid() {
        return !(!isSuccess() || data == null || Check.isEmpty(data.getToken()));
    }

    public User getData() {
        return data;
    }

    public void setData(User data) {
        this.data = data;
    }


}
