import java.lang.reflect.Method;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("-t") ||
            Arrays.asList(args).contains("--t") ||
            Arrays.asList(args).contains("-test") ||
            Arrays.asList(args).contains("--test")) {
            test();
            return;
        }

        try {
            Method meth = Main.class.getMethod("testf", int.class, String.class);
            System.out.println(meth.getParameters()[1].getType().getName());
        } catch (NoSuchMethodException e) {
            System.out.println(e);
        }

        try {
            JILInterpreter interpreter = new JILInterpreter(0);

            //interpreter.execute("StaticTest", false, "import \"io\"\n\nf main\ndef msg as \"hello there.\"\ncall println with msg\nr 0\nend");

            interpreter.execute("StaticTest2", true, """
import "os"
def x 10
call println with x""");
            System.out.println(interpreter.vars);
        } catch (JILException e) {
            System.out.println(e.getMessage());
        }
    }

    public static int testf(int i, String s) {
        return 0;
    }

    public static void test() {
        try {
            JILMemory mem = new JILMemory(10);

            int ptr1 = mem.malloc(5);
            System.out.println(ptr1);

            mem.debug.printMemory();

            int ptr2 = mem.malloc(5);
            System.out.println(ptr2);

            mem.deref(ptr2, 42);

            mem.debug.printMemory();

            mem.debug.printAllocations();

            mem.free(ptr1);

            mem.debug.printMemory();

            System.out.println(mem.deref(ptr2));
            System.out.println(Arrays.toString(mem.derefArray(ptr2)));

            mem.deref(ptr2, 20);

            mem.debug.printMemory();

            System.out.println(mem.deref(ptr2));

            mem.derefArray(ptr2, new int[]{'h', 'e', 'l', 'l', 'o'});

            System.out.println(Arrays.toString(mem.derefArray(ptr2)));
            System.out.println(mem.derefString(ptr2));

            mem.debug.printAllocations();

            mem.free(ptr2);

            mem.debug.printAllocations();
        } catch (JILException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}