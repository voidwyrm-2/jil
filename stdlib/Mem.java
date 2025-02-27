import runtime.JILMemory;
import runtime.JILFunction;
import runtime.JILNative;
import runtime.errors.JILException;
import runtime.errors.JILNativeException;

import java.util.HashMap;


public class Mem {
    @JILNative("")
    public static int malloc(JILMemory memory, HashMap<String, JILFunction> funcs, int sizePtr) throws JILException, JILNativeException {
        int allocPtr;
        try {
            allocPtr = memory.malloc(memory.deref(sizePtr));
        } catch (JILException e) {
            throw new JILNativeException(e.getMessage());
        }

        return allocPtr;
    }

    @JILNative("")
    public static int free(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException, JILNativeException {
        try {
            memory.free(ptr);
        } catch (JILException e) {
            throw new JILNativeException(e.getMessage());
        }

        return 0;
    }

    @JILNative("")
    public static int sizeOfPointer(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException, JILNativeException {
        try {
            return memory.derefArray(ptr).length;
        } catch (JILException e) {
            throw new JILNativeException(e.getMessage());
        }
    }
}
