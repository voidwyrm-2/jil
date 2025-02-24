import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class JILFunction {
    JILToken[][] tokens;
    int argc;
    Method builtin = null;

    public JILFunction(JILToken[][] tokens, int argc) {
        this.tokens = tokens;
        this.argc = argc;
    }

    public JILFunction(Method builtin) throws JILException {
        if (!builtin.getReturnType().equals(int.class))
            throw new JILException(String.format("imported function '%s' does not return a JILReturn record", builtin.getName()));

        int i = 1;
        for (Class<?> param : builtin.getParameterTypes()) {
            if (i == 1) {
                if (!param.equals(JILMemory.class))
                    throw new JILException(String.format("argument %d from imported function '%s' is not of the type JILMemory", i, builtin.getName()));
            } else if (!param.equals(int.class)) {
                throw new JILException(String.format("argument %d from imported function '%s' is not of the type JILMemory", i, builtin.getName()));
            }
            i++;
        }

        argc = builtin.getParameterCount();
        this.builtin = builtin;
    }

    public int run(String file, JILMemory outerMemory, int ...args) throws JILException {
        if (builtin != null) {
            if (args.length < argc - 1)
                throw new JILException(String.format("not enough arguments; expected %d, but %d were given", argc, args.length));
            else if (args.length > argc - 1)
                throw new JILException(String.format("too many enough arguments; expected %d, but %d were given", argc, args.length));

            Object[] finalArgs = new Object[args.length + 1];
            finalArgs[0] = outerMemory;
            for (int i = 0; i < args.length; i++) {
                finalArgs[i + 1] = args[i];
            }

            try {
                return (int) builtin.invoke(null, finalArgs);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new JILException("'" + e.getMessage() + "' from function invocation");
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause.getClass().equals(JILException.class)) {
                        throw (JILException)cause;
                    } else {
                        StackTraceElement[] trace = cause.getStackTrace();
                        String[] stringTrace = new String[trace.length];
                        for (int i = 0; i < trace.length; i++)
                            stringTrace[i] = trace[i].toString();

                        throw new JILException("an exception has occurred inside the called native function:\n " + e.getCause().getMessage() + "\n  " + String.join("\n  ", stringTrace));
                    }
                } else {
                    throw new JILException("an unknown exception has occurred inside the called native function");
                }
            }
        } else {
            JILInterpreter interpreter = new JILInterpreter(outerMemory);
            return interpreter.execute(file, true, tokens);
        }
    }
}
