import runtime.JILException;
import runtime.JILInterpreter;

public class Main {
    public static void main(String[] args) {
        JILInterpreter interpreter = new JILInterpreter(0);

        try {
            interpreter.execute("", false, """
            import "std/IO"
            """);

            System.exit(interpreter.runMain());
        } catch (JILException e) {
            System.out.println(e.getMessage());
        }
    }
}