public class Main {
    public static void main(String[] args) {
        JILInterpreter interpreter = new JILInterpreter(0);
        try {
            interpreter.execute("f main\ndef msg as \"hello there.\"\ncall println with msg\nr 0\nend");
        } catch (JILException e) {
            System.out.println(e.getMessage());
        }
    }
}