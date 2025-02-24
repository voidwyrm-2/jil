public class IO {
    @JILNative("")
    public static int prin(JILMemory memory, int ptr) throws JILException {
        System.out.print(memory.deref(ptr));
        return 0;
    }

    @JILNative("")
    public static int prins(JILMemory memory, int ptr) throws JILException {
        System.out.print(memory.derefString(ptr));
        return 0;
    }

    @JILNative("")
    public static int prinl(JILMemory memory, int ptr) throws JILException {
        System.out.println(memory.deref(ptr));
        return 0;
    }

    @JILNative("")
    public static int prinsl(JILMemory memory, int ptr) throws JILException {
        System.out.println(memory.derefString(ptr));
        return 0;
    }
}
