import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        test();

        /*
        try {
            JILInterpreter interpreter = new JILInterpreter(0);

            interpreter.execute("f main\ndef msg as \"hello there.\"\ncall println with msg\nr 0\nend");
        } catch (JILException e) {
            System.out.println(e.getMessage());
        }
        */
    }

    public static void test() {
        try {
            JILMemory mem = new JILMemory(10);

            int ptr1 = mem.malloc(5);
            System.out.println(ptr1);

            int ptr2 = mem.malloc(5);
            System.out.println(ptr2);

            mem.free(ptr1);

            System.out.println(mem.getRef(ptr2));
            System.out.println(Arrays.toString(mem.getArrayRef(ptr2)));
        } catch (JILException e) {
            System.out.println(e.getMessage());
        }
    }
}