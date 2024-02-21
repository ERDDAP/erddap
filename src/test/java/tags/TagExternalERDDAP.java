package tags;

import org.junit.jupiter.api.Tag;
 
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
 
/** TODO: Make tests with this tag runnable. */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("ExternalERDDAP")
public @interface TagExternalERDDAP {
}
