import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class JILInterpreter {
    JILMemory memory;
    HashMap<String, Integer> vars;

    public JILInterpreter(Integer memorySize) throws JILException {
        memory = new JILMemory(memorySize);
        vars = new HashMap<>();
    }

    private JILToken[][] tokenize(String text) {
        ArrayList<JILToken[]> lines = new ArrayList<>();

        for (String line : text.split("\n")) {
            ArrayList<JILToken> tokens = new ArrayList<>();
            StringBuilder acc = null;

            for (String t : line.split(" ")) {
                if (t.startsWith("\"") && acc == null) {
                    acc = new StringBuilder();
                    t = t.substring(1);
                }

                if (acc != null) {
                    if (t.endsWith("\"")) {
                        acc.append(t, 0, t.length() - 1);
                        tokens.add(new JILToken(acc.toString(), true));
                        acc = null;
                        continue;
                    }
                    acc.append(t).append(" ");
                } else {
                    tokens.add(new JILToken(t, false));
                }
            }

            lines.add(tokens.toArray(new JILToken[0]));
        }

        return lines.toArray(new JILToken[0][]);
    }

    public int execute(JILToken[][] tokenLines) throws JILException {
        for (JILToken[] tl : tokenLines) {
            for (JILToken t : tl) System.out.print(t);
            System.out.print("\n");
        }
        return 0;
    }

    public int execute(String text) throws JILException {
        return execute(tokenize(text));
    }
}
