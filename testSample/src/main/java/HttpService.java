import renovate.Renovate;
import renovate.RxAdapter;
import request.AuthRequest;
import response.LoginResponse;
import rx.Observable;
import rx.functions.Func1;

public class HttpService {
    private static HttpService httpService;
    private static Renovate renovate;

    private HttpService() {
        renovate = new Renovate.Builder().baseUrl("http://localhost:8080/").build();
    }

    public static HttpService instance() {
        if (httpService == null) {
            synchronized (HttpService.class) {
                if (httpService == null) {
                    httpService = new HttpService();
                }
            }
        }
        return httpService;
    }

    public <T> Observable<T> request(Object object, Class<T> tClass) {
        return renovate.request(object).request(new FastJsonConvert<T>(tClass), new RxAdapter<T>());
    }

    public Observable<LoginResponse> login(Object object) {
        return renovate.request(object).request(new FastJsonConvert<LoginResponse>(), new RxAdapter<LoginResponse>()).map(new Func1<LoginResponse, LoginResponse>() {
            @Override
            public LoginResponse call(LoginResponse t) {
                if (t != null && t.isValid()) {
                    if (t.getData() != null && t.getData().getToken() != null)
                        AuthRequest.authorization = t.getData().getToken();
                }
                return t;
            }
        });
    }


}
