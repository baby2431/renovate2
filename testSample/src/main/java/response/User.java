package response;



public class User {
    /**
     * username : 13306992629
     * signature :
     * thumb :
     * gender : 0
     * age : 0
     * pious : 0
     * token : eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOjE0LCJpc3MiOiJodHRwOlwvXC95eGJhY2suZnR2Y24uY29tOjUwNTFcL2FwaVwvbWVtYmVyXC9sb2dpbiIsImlhdCI6MTQ5NDAwNDgyNywiZXhwIjoxNDk1MjE0NDI3LCJuYmYiOjE0OTQwMDQ4MjcsImp0aSI6IktOeHFkcUU3RXliSjk5TUQifQ.kwbS8ydgiiVpozMih_dKLxXXxIo0_JS85qArJIsyeZo
     */

    private String id;

    private String username;
    private String signature;
    private String thumb;
    private int gender;
    private int age;
    private int pious;
    private String token;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean tokenValid() {
        return !Check.isEmpty(token);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getPious() {
        return pious;
    }

    public void setPious(int pious) {
        this.pious = pious;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
