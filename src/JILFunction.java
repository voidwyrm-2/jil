import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            throw new JILException(String.format("imported function '%s' does not return an integer", builtin.getName()));

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

        this.builtin = builtin;
    }

    public int run(String file, JILMemory outerMemory, int ...args) throws JILException {
        if (builtin != null) {
            try {
                return (int)builtin.invoke(null);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new JILException(e.getMessage());
            } catch (InvocationTargetException e) {
                throw new JILException("an internal exception has occurred inside the called native function");
            }
        } else {
            JILInterpreter interpreter = new JILInterpreter(outerMemory);
            return interpreter.execute(file, true, tokens);
        }
    }
}
