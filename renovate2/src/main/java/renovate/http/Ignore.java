package renovate.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Ignore renovate parse
 * Created by Sirius on 2017/4/10.
 */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface Ignore {
}
