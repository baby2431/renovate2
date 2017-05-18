package renovate;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by babyt on 2017/5/18.
 */
public abstract class HeaderCURD {

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

    public boolean hasHeader(String key) {
        return headerMap.containsKey(key);
    }

    public String findHeader(String key) {
        return headerMap.get(key);
    }

}
