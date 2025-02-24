import java.lang.annotation.*;


/**
 * Any methods in a JIL native module that are annotated with {@code JILNative}` are imported when loading said module
 * If the parameter of {@code JILNative}` is not empty, that will be used instead of the method's real name
*/
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JILNative {
    String value();
}
