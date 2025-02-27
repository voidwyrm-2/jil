import runtime.JILMemory;
import runtime.JILFunction;
import runtime.JILNative;
import runtime.errors.JILException;
import runtime.errors.JILNativeException;

import java.util.HashMap;
import java.io.*;


public class IO {
    @JILNative("")
    public static int print(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException, JILNativeException {
        System.out.print(memory.deref(ptr));
        return 0;
    }

    @JILNative("")
    public static int prints(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException, JILNativeException {
        System.out.print(memory.derefString(ptr));
        return 0;
    }

    @JILNative("")
    public static int println(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException, JILNativeException {
        System.out.println(memory.deref(ptr));
        return 0;
    }

    @JILNative("")
    public static int printsln(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException, JILNativeException {
        System.out.println(memory.derefString(ptr));
        return 0;
    }

    @JILNative("")
    public static int sizeOfFile(JILMemory memory, HashMap<String, JILFunction> funcs, int ptr) throws JILException, JILNativeException {
        File f = new File(memory.derefString(ptr));
        if (!f.exists() || !f.isFile())
            return -1;

        return (int)f.length();
    }

    @JILNative("")
    public static int readFile(JILMemory memory, HashMap<String, JILFunction> funcs, int ptrIn, int ptrOut) throws JILException, JILNativeException {
        String content = "";
        try(BufferedReader br = new BufferedReader(new FileReader(memory.derefString(ptrIn)))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            content = sb.toString();
        } catch (IOException e) {
            throw new JILNativeException(e.getMessage());
        }

        memory.derefString(ptrOut, content);

        return 0;
    }
}
