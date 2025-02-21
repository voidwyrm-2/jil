import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("-t") || Arrays.asList(args).contains("--t") || Arrays.asList(args).contains("-test") || Arrays.asList(args).contains("--test")) {
            test();
            return;
        }

        try {
            JILInterpreter interpreter = new JILInterpreter(0);

            interpreter.execute("StaticTest", false, "import \"io\"\n\nf main\ndef msg as \"hello there.\"\ncall println with msg\nr 0\nend");
        } catch (JILException e) {
            System.out.println(e.getMessage());
        }
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