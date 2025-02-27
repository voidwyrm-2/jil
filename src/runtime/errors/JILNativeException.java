package runtime.errors;

/**
 * The exception that should be thrown by native functions instead of {@code runtime.errors.JILException}
 * <p>
 * Throwing this instead allows errors thrown by the function itself to be caught
 */
public class JILNativeException extends JILException {
    public JILNativeException(String message) {
        super(message);
    }
}
