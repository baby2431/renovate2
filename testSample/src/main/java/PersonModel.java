import renovate.http.*;


@HTTP(method = HTTP.Method.GET, path = "Test/Servlet")
public class PersonModel {

    @Header
    String authorization = "ASDASDASD";


    @Query
    String name;


    @Params("age")
    @Ignore
    int age;


}
