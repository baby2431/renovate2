package renovate.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by xmmc on 2017/3/24.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface JSON {
}
