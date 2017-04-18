package renovate;

import okhttp3.ResponseBody;
import renovate.call.Call;

/**
 * Created by xmmc on 2017/4/10.
 */

public interface Test {

    Call<ResponseBody> call();


}
