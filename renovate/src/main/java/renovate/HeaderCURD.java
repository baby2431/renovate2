package renovate;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by babyt on 2017/5/18.
 */
abstract class HeaderCURD {

    protected Map<String, String> headerMap = new HashMap<>();

    protected HeaderCURD() {

    }

    public void addHeader(String name, String value) {
        headerMap.put(name, value);
    }

    Map<String, String> getHeader() {
        return headerMap;
    }

    public void setHeader(Map<String, String> map) {
        headerMap = map;
    }

    public void removeHeader(String key) {
        headerMap.remove(key);
    }

    public void hasHeader(String key) {
        headerMap.containsKey(key);
    }

    public void findHeader(String key) {
        headerMap.get(key);
    }

}
