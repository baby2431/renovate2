package request;

import renovate.http.*;


@HTTP(method = HTTP.Method.GET, path = "Test/Servlet")
public class PersonModel {

    @Header
    public String authorization = "ASDASDASD";


    @Query
    public String name;


    @Params("age")
    @Ignore
    public int age;

}
