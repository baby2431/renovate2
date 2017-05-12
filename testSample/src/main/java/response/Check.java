package response;

import static java.util.Objects.isNull;

/**
 * Created by babyt on 2017/5/11.
 */
public class Check {
    public static boolean isEmpty(CharSequence str) {
        return isNull(str) || str.length() == 0;
    }
}
