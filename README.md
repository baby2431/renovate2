


# Renovate - preview

对象式Http网络请求框架


## 背景及面向对象思想

用过Retrofit的知道，Retrofit是一个将http请求，表现成高级语言的接口（Interface）,抽象成API的规范的优秀思想的Http框架,只暴露出了我们业务中的数据模型和操作方法。



然而什么是面向对象？Renovate如何运用这个思想？

>“面向对象”是专指在程序设计中采用封装、继承、多态和抽象等设计方法。[百度百科](http://baike.baidu.com/link?url=W9ir0cbfShdpOzpCYiE1UdUbYBwE3_iVYiNsyFu8DmLuP10p5jqGH8a6JzVONAD2ieuSgkdAEuINBUxohFVIPkDPv5N1cN5qG7ZH6K8MnGmCNI8VwXqt8oTlWgATeYe88ool6_37P4pQz0SGJS5hW_)

>对象是人们要进行研究的任何事物，从最简单的整数到复杂的飞机等均可看作对象，它不仅能表示具体的事物，还能表示抽象的规则、计划或事件。[百度百科](http://baike.baidu.com/link?url=W9ir0cbfShdpOzpCYiE1UdUbYBwE3_iVYiNsyFu8DmLuP10p5jqGH8a6JzVONAD2ieuSgkdAEuINBUxohFVIPkDPv5N1cN5qG7ZH6K8MnGmCNI8VwXqt8oTlWgATeYe88ool6_37P4pQz0SGJS5hW_)

**Renovate重新定义了Http请求的方式，抽象出Http的请求为对象，采用了基于对象的注解和配置，对Http接口进行请求**

Renovate是封装了OKHttp和标准RESTful风格的网络框架，只要配置好对象的，就能轻松的进行网络请求。



## Renovate 目前支持
* 支持 GET, HEAD, DELETE,PUT,PATCH,OPTIONS,POST等请求方式
* 比Retrofit更简单方便
* 文件上传
* 支持RxJava函数式相应编程（强烈推荐使用）
* 自适应Java平台和Android平台（Android平台回调在主线程中执行）
* 使用过Retrofit的接入到Renovate学习成本低


下载及使用
--------
Renovate

下载 [最新版本][2] 或者通过Maven:
```xml
<dependency>
  <groupId>renovate2</groupId>
  <artifactId>renovate</artifactId>
  <version>0.2</version>
  <type>pom</type>
</dependency>
```
还可以通过 Gradle:
```groovy
compile 'renovate2:renovate:0.2'
```

**Renovare 至少需要Java 7 和Android 2.3.**

RX-Renovate（需要使用RxJava需要这个版本）

下载 [最新版本][3] 或者通过Maven:
```xml
<dependency>
    <groupId>renovate2</groupId>
    <artifactId>rx-renovate</artifactId>
    <version>0.2</version>
    <type>pom</type>
</dependency>
```
还可以通过 Gradle:
```groovy
compile 'renovate2:rx-renovate:0.2'
```



**如果需要在Android平台上进行使用，需要添加rxandroid**

*目前支持的是rxjava 1.2.0，后续版本将增强至rxjava2*

Maven:
```xml
<dependency>
    <groupId>io.reactivex</groupId>
    <artifactId>rxandroid</artifactId>
    <version>1.2.1</version>
    <packaging>aar</packaging>
</dependency>
```
Gradle:
```groovy
compile 'io.reactivex:rxandroid:1.2.1'
```

## Examples
e.g

比如你有一个登录的实体，长得下面的样子：
```
@HTTP(method = HTTP.Method.POST, path = "api/member/login") 
@FormUrlEncoded
public class Login  {
    @Params(value = "mobile") //如果不写，参数名将是字段名
    public String mobile;//帐号

    @Params(value = "password")
    public String password; //密码

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```
*当中的@Http是必须要的，而Post请求中如果没有文件等要上传，只是表单则需要添加@FormUrlEncoded*

好了，一个简单的http请求的对象化已经实现完成。接下来我们就要进行请求了
```
    @Test
    public void testConvert() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);//用于junit测试
        Login p = new Login();
        p.mobile = "133xxxxxxxx";//设置请求的电话号码
        p.password = "password";//当然这是密码
        System.out.println("current thread = " + Thread.currentThread().getName());
        //显示的是当前的线程，Android上的话，就是主线程了
        Renovate renovate = new Renovate.Builder().baseUrl("http://localhost:8080/").build();
        renovate.request(p).request().enqueue(new Callback<ResponseBody>() { //异步请求
            @Override
            public void onResponse(Call call, Response response) {
                print(response);//打印返回信息
                //也是主线程
                System.out.println("response thread = " + Thread.currentThread().getName());
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                t.printStackTrace();
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        System.out.println("end");
    }

```
So easy，一个简单的登录就完成了。当然了，功能不仅仅是这样子，如果要做更多的事情呢。
比如：
- 结合Rxjava，实现函数式响应编程
- 请求当中有个对象序列化成对象请求
- 对返回的Json自动解析成想要的对象
- 取消请求
- 上传多个文件
- 实现其他的请求方式，例如GET, HEAD, DELETE,PUT,PATCH,OPTIONS
- 添加请求头部














**希望大家能够提供宝贵意见和建议，更好的维护这个产品**

## Contact me
 * 邮箱地址：babyte185@163.com
 * QQ：243107006
 


### License

Renovate is released under the [Apache 2.0 license](LICENSE).

```
Copyright 2017 Sirius Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
