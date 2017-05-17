package response;

import static java.util.Objects.isNull;


public class Check {
    public static boolean isEmpty(CharSequence str) {
        return isNull(str) || str.length() == 0;
    }
}
