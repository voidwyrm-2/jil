package runtime;

import lexer.Token;
import runtime.errors.JILException;
import runtime.errors.JILNativeException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;

public class JILFunction {
    Token[][] tokens;
    int argc;
    Method builtin = null;

    public JILFunction(Token[][] tokens, int argc) {
        this.tokens = tokens;
        this.argc = argc;
    }

    public JILFunction(Method builtin) throws JILException {
        if (!builtin.getReturnType().equals(int.class))
            throw new JILException(String.format("imported function '%s' does not return a JILReturn record", builtin.getName()));

        if (builtin.getParameterCount() < 2)
            throw new JILException(String.format("imported function '%s' must have at least two parameters", builtin.getName()));

        Class<?>[] exceptions = builtin.getExceptionTypes();
        if (exceptions.length != 2 || !exceptions[0].equals(JILException.class) || !exceptions[1].equals(JILNativeException.class)) {
            throw new JILException(String.format("imported function '%s' must throw two exceptions of the JILException and JILNativeException classes in that order", builtin.getName()));
        }

        Class<?>[] params = builtin.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i == 0) {
                if (!params[i].equals(JILMemory.class))
                    throw new JILException(String.format("argument %d from imported function '%s' is not a JILMemory", i, builtin.getName()));
            } else if (i == 1) {
                Type[] generics = ((ParameterizedType) builtin.getGenericParameterTypes()[i]).getActualTypeArguments();
                if (!params[i].equals(HashMap.class) || !generics[0].getTypeName().equals(String.class.getTypeName()) || !generics[1].getTypeName().equals(JILFunction.class.getTypeName()))
                    throw new JILException(String.format("argument %d from imported function '%s' is not a Hashmap<String, JILFunction>", i, builtin.getName()));
            } else if (!params[i].equals(int.class)) {
                throw new JILException(String.format("argument %d from imported function '%s' is not an int", i, builtin.getName()));
            }
        }

        argc = builtin.getParameterCount();
        this.builtin = builtin;
    }

    public int run(String file, int showVars, JILMemory outerMemory, HashMap<String, JILFunction> funcs, int ...args) throws JILException {
        if (builtin != null) {
            if (args.length < argc - 2)
                throw new JILException(String.format("not enough arguments; expected %d, but %d were given", argc, args.length));
            else if (args.length > argc - 2)
                throw new JILException(String.format("too many enough arguments; expected %d, but %d were given", argc, args.length));

            Object[] finalArgs = new Object[args.length + 2];
            finalArgs[0] = outerMemory;
            finalArgs[1] = funcs;
            for (int i = 0; i < args.length; i++) {
                finalArgs[i + 2] = args[i];
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
                    } else if (cause.getClass().equals(JILNativeException.class)) {
                        throw (JILNativeException)cause;
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
            JILInterpreter interpreter = new JILInterpreter(outerMemory, funcs);
            for (int i = 0; i < args.length; i++)
                interpreter.setVar("$" + i, args[i], true);

            return interpreter.execute(file, showVars, true, tokens);
        }
    }

    @Override
    public String toString() {
        return String.format("fun(%d)", builtin != null ? argc - 2 : argc);
    }
}
