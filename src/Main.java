import argparse.ArgparseException;
import argparse.Argparser;
import argparse.Flag;
import runtime.errors.JILException;
import runtime.JILInterpreter;

import java.io.*;

public class Main {
    public static void main(String[] args) {
        Argparser parser = new Argparser("jil");

        Flag<Boolean> help = parser.addFlag("h", false, Boolean::parseBoolean, "Lists the flags of the program");
        Flag<Boolean> runTests = parser.addFlag("t", false, Boolean::parseBoolean, "Runs the language tests");
        Flag<Boolean> showVars = parser.addFlag("vars", false, Boolean::parseBoolean, "Prints the variables after execution");
        Flag<Boolean> showFuncs = parser.addFlag("funcs", false, Boolean::parseBoolean, "Prints the functions after execution");
        Flag<Integer> memorySize = parser.addFlag("m", 0, Integer::parseInt, "The amount of memory the interpreter has");

        String[] leftover = new String[0];
        try {
            leftover = parser.parse(args);
        } catch (ArgparseException e) {
            System.out.println(e.getMessage());
            System.out.println(parser.help());
            System.exit(1);
        }

        if (help.get()) {
            System.out.println(parser.help());
            System.exit(0);
        }

        JILInterpreter interpreter = new JILInterpreter(memorySize.get());

        if (leftover.length == 0) {
            System.out.println("expected 'jil [file]'");
            System.exit(1);
        }

        String content = "";
        try(BufferedReader br = new BufferedReader(new FileReader(leftover[0]))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            content = sb.toString();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        try {
            interpreter.execute(new File(leftover[0]).getName(), 0, false, content);

            int code = interpreter.runMain(showVars.get());

            if (showFuncs.get())
                System.out.println(interpreter.getFuncs());

            System.exit(code);
        } catch (JILException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}