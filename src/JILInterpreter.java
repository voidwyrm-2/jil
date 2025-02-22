import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

public class JILInterpreter {
    private final JILMemory memory;
    private final HashMap<String, Integer> vars;
    private final HashMap<String, JILFunction> funcs;

    private record TokenChecker(JILToken[] tokens) {
        private String check(int index, boolean expectString, String... expected) {
            StringBuilder expectMsg = new StringBuilder();
            if (expectString) {
                expectMsg.append("expected string, but found ");
            } else if (expected.length == 0) {
                expectMsg.append("expected EOL, but found ");
            } else {
                expectMsg.append("expected '");
                for (int i = 0; i < expected.length; i++)
                    expectMsg.append(expected[i]).append(i == expected.length - 1 ? "" : "', '");
                //expectMsg.delete();
                expectMsg.append("', but found ");
            }

            boolean ethrow = true;

            if (index >= tokens.length) {
                if (expected.length == 0 && !expectString)
                    ethrow = false;
                else
                    expectMsg.append("but found EOL instead");
            } else if (expectString) {
                if (tokens[index].isString())
                    ethrow = false;
                else
                    expectMsg.append("'").append(tokens[index].content()).append("' instead");
            } else if (expected.length == 0) {
                expectMsg.append("'").append(tokens[index].content()).append("' instead");
            } else if (Arrays.asList(expected).contains(tokens[index].content())) {
                if (expected[0].isEmpty()) { // empty string is taken as 'any token'
                    ethrow = false;
                } else {
                    expectMsg.append("'").append(tokens[index].content()).append("' instead");
                }
            } else {
                ethrow = false;
            }

            return ethrow ? expectMsg.toString() : null;
        }
    }

    private record InFnChecker(boolean inFunction, String operation) {
        void check() throws JILException {
            if (!inFunction)
                throw new JILException(String.format("cannot use '%s' operation outside of function", operation));
        }
    }

    public JILInterpreter(Integer memorySize) throws JILException {
        memory = new JILMemory(memorySize);
        vars = new HashMap<>();
        funcs = new HashMap<>();
    }

    public JILInterpreter(JILMemory outerMemory) {
        memory = outerMemory;
        vars = new HashMap<>();
        funcs = new HashMap<>();
    }

    private JILToken[][] tokenize(String text) throws JILException {
        ArrayList<JILToken[]> lines = new ArrayList<>();

        int ln = 1;
        for (String line : text.split("\n")) {
            ArrayList<JILToken> tokens = new ArrayList<>();
            StringBuilder acc = null;

            if (line.trim().isEmpty())
                continue;

            for (String t : line.split(" ")) {
                if (t.startsWith("\"") && acc == null) {
                    acc = new StringBuilder();
                    t = t.substring(1);
                }

                if (acc != null) {
                    if (t.endsWith("\"")) {
                        // TODO: add a string parser to convert escape sequences
                        acc.append(t, 0, t.length() - 1);
                        tokens.add(new JILToken(acc.toString(), true));
                        acc = null;
                        continue;
                    }
                    acc.append(t).append(" ");
                } else if (t.contains("//")) {
                    break;
                } else {
                    tokens.add(new JILToken(t, false));
                }
            }

            if (acc != null)
                throw new JILException(String.format("error on line %d: unterminated string literal", ln));

            lines.add(tokens.toArray(new JILToken[0]));
            ln++;
        }

        return lines.toArray(new JILToken[0][]);
    }

    public final void setVar(String name, int val, boolean define) throws JILException {
        if (define && vars.containsKey(name))
            throw new JILException(String.format("cannot redefine existing variable '%s'", name));
        else if (!define && !vars.containsKey(name))
            throw new JILException("variable '" + name + "' does not exist");
        vars.put(name, val);
    }

    public final int getVar(String name) {
        throw new UnsupportedOperationException("unimplemented");
    }

    public int eval(JILToken[] tokens) throws JILException {
        Stack<Integer> estack = new Stack<>();

        for (JILToken t : tokens) {
            if (t.isString())
                throw new JILException("cannot use strings in expressions");

            try {
                estack.add(Integer.parseInt(t.content()));
            } catch (NumberFormatException e) {
                switch (t.content()) {
                    case "+" -> {
                        if (estack.size() < 2)
                            throw new JILException("expected two operands on the stack for '" + t.content() + "', but found " + estack.size() + " instead");
                        Integer b = estack.pop();
                        estack.add(estack.pop() + b);
                    }
                    case "-" -> {
                        if (estack.size() < 2)
                            throw new JILException("expected two operands on the stack for '" + t.content() + "', but found " + estack.size() + " instead");
                        Integer b = estack.pop();
                        estack.add(estack.pop() - b);
                    }
                    case "*" -> {
                        if (estack.size() < 2)
                            throw new JILException("expected two operands on the stack for '" + t.content() + "', but found " + estack.size() + " instead");
                        Integer b = estack.pop();
                        estack.add(estack.pop() * b);
                    }
                    case "/" -> {
                        if (estack.size() < 2)
                            throw new JILException("expected two operands on the stack for '" + t.content() + "', but found " + estack.size() + " instead");
                        Integer b = estack.pop();
                        estack.add(estack.pop() / b);
                    }
                    case "%" -> {
                        if (estack.size() < 2)
                            throw new JILException("expected two operands on the stack for '" + t.content() + "', but found " + estack.size() + " instead");
                        Integer b = estack.pop();
                        estack.add(estack.pop() % b);
                    }
                    case "**" -> {
                        if (estack.size() < 2)
                            throw new JILException("expected two operands on the stack for '" + t.content() + "', but found " + estack.size() + " instead");
                        Integer b = estack.pop();
                        estack.add((int)Math.pow((double)estack.pop(), (double)b));
                    }
                    default -> {
                        if (vars.containsKey(t.content())) {
                            estack.push(vars.get(t.content()));
                        } else {

                        }
                    }
                }
            }
        }

        if (estack.empty())
            throw new JILException("the stack cannot be empty at the end of expression evaluation");

        return estack.pop();
    }

    public int execute(String file, boolean inFunction, JILToken[][] tokenLines) throws JILException {
        int ln = 0;

        try {
            while (ln < tokenLines.length) {
                JILToken[] tl = tokenLines[ln];
                TokenChecker tc = new TokenChecker(tl);
                InFnChecker inFnChecker = new InFnChecker(inFunction, tl[0].content());

                if (tl[0].isString())
                    throw new JILException("unexpected string");

                switch (tl[0].content()) {
                    case "import" -> {
                        String res = tc.check(1, true);
                        if (res != null) throw new JILException(res);

                        if (inFunction)
                            throw new JILException("cannot import a module inside of a function");

                        String modName = tl[0].content();
                        File homeDir = new File(System.getProperty("user.home") + File.separator + "");

                        System.out.println(homeDir);
                        ln++;
                    }
                    case "f" -> {
                        String res = tc.check(1, false, "");
                        if (res != null) throw new JILException(res);

                        if (inFunction)
                            throw new JILException("cannot define a function inside of a function");

                        ln++;
                    }
                    case "def", "defptr" -> {
                        inFnChecker.check();
                        String res = tc.check(1, false, "");
                        if (res != null) throw new JILException(res);

                        String name = tl[1].content();
                        if (vars.containsKey(name))


                        if (tl.length == 2)
                            throw new JILException("expected expression, but found EOL instead");

                        res = tc.check(2, true);
                        int varValue = 0;
                        if (res == null) {
                            String s = tl[2].content();
                            int ptr = memory.malloc(s.length());
                            memory.derefString(ptr, s);
                        } else {
                            varValue = eval(Arrays.copyOfRange(tl, 2, tl.length));
                        }

                        if (tl[0].content().endsWith("ptr")) {
                            int subPtr = memory.malloc(1);
                            memory.deref(subPtr, varValue);
                            varValue = subPtr;
                        }

                        vars.put(name, varValue);
                        ln++;
                    }
                    case "call" -> {
                        inFnChecker.check();
                    }
                    default -> throw new JILException(String.format("unexpected token '%s'", tl[0].content()));
                }
            }
        } catch (JILException e) {
            throw new JILException(String.format("error on line %d of %s:\n%s", ln + 1, file, e.getMessage()));
        }

        return 0;
    }

    public int execute(String file, boolean inFunction, String text) throws JILException {
        return execute(file, inFunction, tokenize(text));
    }
}
