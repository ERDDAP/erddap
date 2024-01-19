package tags;

import org.junit.jupiter.api.Tag;
 
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
 
/** TODO: Bundle non-common fonts (Deja Vu) with the app, so that these tests aren't flaky. */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("FontDependent")
public @interface TagFontDependent {
}