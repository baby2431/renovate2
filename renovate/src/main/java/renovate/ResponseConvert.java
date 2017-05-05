package renovate;

import okhttp3.ResponseBody;

interface ResponseConvert<T> extends Converter<ResponseBody, T> {

}
