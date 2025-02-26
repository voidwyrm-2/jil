import runtime.JILMemory;
import runtime.JILFunction;
import runtime.JILNative;
import runtime.JILException;
import java.util.HashMap;


public class IO {
    @JILNative("")
    public static int print(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException {
        System.out.print(memory.deref(ptr));
        return 0;
    }

    @JILNative("")
    public static int prints(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException {
        System.out.print(memory.derefString(ptr));
        return 0;
    }

    @JILNative("")
    public static int println(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException {
        System.out.println(memory.deref(ptr));
        return 0;
    }

    @JILNative("")
    public static int printsln(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException {
        System.out.println(memory.derefString(ptr));
        return 0;
    }
}
