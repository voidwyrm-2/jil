import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Pattern;

public class JILInterpreter {
    private final JILMemory memory;
    private final HashMap<String, Integer> vars;
    private final HashMap<String, JILFunction> funcs;

    private record TokenChecker(JILToken[] tokens) {
        static class TMatcher {
            boolean expectString, anyOption;
            String[] options;

            TMatcher(boolean expectString, boolean anyOption, String[] options) {
                this.expectString = expectString;
                this.anyOption = anyOption;
                this.options = options;
            }

            public static TMatcher str() {
                return new TMatcher(true, false, null);
            }

            public static TMatcher any() {
                return new TMatcher(false, true, null);
            }

            public static TMatcher opt(String... options) {
                return new TMatcher(false, false, options);
            }

            @Override
            public String toString() {
                return String.format("{string: %s, any: %s, options: %s}", expectString, anyOption, Arrays.toString(options));
            }
        }

        private String check(int index, TMatcher matcher) {
            StringBuilder expectMsg = new StringBuilder();
            if (matcher.expectString) {
                expectMsg.append("expected string, but found ");
            } else if (matcher.anyOption) {
                expectMsg.append("expected identifier, but found ");
            } else if (matcher.options.length == 0) {
                expectMsg.append("expected EOL, but found ");
            } else {
                expectMsg.append("expected '");
                for (int i = 0; i < matcher.options.length; i++)
                    expectMsg.append(matcher.options[i]).append(i == matcher.options.length - 1 ? "" : i == matcher.options.length - 2 ? "', or '" :  "', '");
                expectMsg.append("', but found ");
            }

            boolean ethrow = true;

            if (index >= tokens.length) {
                if (matcher.options != null && matcher.options.length == 0 && !matcher.expectString)
                    ethrow = false;
                expectMsg.append("but found EOL instead");
            } else if (matcher.expectString) {
                expectMsg.append("'").append(tokens[index].content()).append("' instead");
                ethrow = !tokens[index].isString();
            } else if (matcher.anyOption) {
                ethrow = false;
            } else if (matcher.options.length == 0) {
                expectMsg.append("'").append(tokens[index].content()).append("' instead");
            } else if (!Arrays.asList(matcher.options).contains(tokens[index].content())) {
                expectMsg.append("'").append(tokens[index].content()).append("' instead");
            } else {
                ethrow = false;
            }

            return ethrow ? expectMsg.toString() : null;
        }

        private String checkAll(int start, int end, TMatcher... matchers) {
            if (start < 0) start = 0;

            if (end > tokens.length)
                end = tokens.length;
            else if (end < start - 1)
                end = start + 1;

            for (int i = 0; i + start < end; i++) {
                String res = check(i + start, matchers[i]);
                if (res != null) return res;
            }

            return null;
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

    public JILMemory.MemoryDebug memoryDebug() {
        return memory.debug;
    }

    public JILToken[][] tokenize(String text) throws JILException {
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

    public final void setRawVar(String name, int val, boolean define) throws JILException {
        if (define && vars.containsKey(name))
            throw new JILException(String.format("cannot redefine existing variable '%s'", name));
        else if (!define && !vars.containsKey(name))
            throw new JILException("variable '" + name + "' does not exist");

        vars.put(name, val);
    }

    public final void setRawVar(String name, int val) throws JILException {
        setRawVar(name, val, !vars.containsKey(name));
    }

    public final int getRawVar(String name) throws JILException {
        if (!vars.containsKey(name))
            throw new JILException("variable '" + name + "' does not exist");

        return vars.get(name);
    }

    public final void setVar(String name, int val, boolean define) throws JILException {
        if (define) {
            int ptr = memory.malloc(1);
            memory.deref(ptr, val);
            setRawVar(name, ptr, true);
        } else {
            int ptr = getRawVar(name);
            memory.deref(ptr, val);
        }
    }

    public final void setVar(String name, int val) throws JILException {
        setVar(name, val, !vars.containsKey(name));
    }

    public final int getVar(String name) throws JILException {
        int ptr = getRawVar(name);
        return memory.deref(ptr);
    }

    public final void defFunc(String name, JILFunction f) throws JILException {
        if (funcs.containsKey(name))
            throw new JILException(String.format("cannot redefine existing function '%s'", name));

        funcs.put(name, f);
    }

    public final JILFunction getFunc(String name) throws JILException {
        if (!funcs.containsKey(name))
            throw new JILException("function '" + name + "' does not exist");

        return funcs.get(name);
    }

    public final void loadModule(String path) throws JILException {
        loadModule(new File(path));
    }

    public final void loadModule(File path) throws JILException {
        String[] spath = path.getAbsolutePath().split(Pattern.quote(File.separator));

        String modclass = spath[spath.length - 1];
        if (modclass.endsWith(".class")) {
            modclass = modclass.substring(0, modclass.length() - 6);
        }

        File modpath = new File(String.join("/", Arrays.copyOf(spath, spath.length - 1)));

        URL url;
        try {
            url = modpath.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new JILException(e.getMessage());
        }

        ClassLoader cl = new URLClassLoader(new URL[]{url});

        Class<?> cls;
        try {
            cls = cl.loadClass(modclass);
        } catch (ClassNotFoundException e) {
            throw new JILException(e.toString());
        }

        for (Method m : cls.getMethods()) {
            JILNative jilNative = m.getAnnotation(JILNative.class);

            if (jilNative != null) {
                String fname = jilNative.value().trim();
                if (fname.isBlank() || fname.isEmpty()) fname = m.getName();

                defFunc(fname, new JILFunction(m));
            }
        }
    }

    public final int eval(JILToken[] tokens) throws JILException {
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
                        estack.push(getVar(t.content()));
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
                        if (inFunction)
                            throw new JILException("cannot import a module inside of a function");

                        String res = tc.check(1, TokenChecker.TMatcher.str());
                        if (res != null) throw new JILException(res);

                        String modPath = tl[1].content();
                        File libModule = new File(System.getProperty("user.home") + File.separator + "jil" + File.separator + "lib" + File.separator + modPath + ".class");
                        File localModule = new File(modPath + ".class");

                        if (localModule.exists()) {
                            loadModule(localModule);
                        } else if (libModule.exists()) {
                            loadModule(libModule);
                        } else {
                            throw new JILException(String.format("module '%s' not found", modPath));
                        }

                        ln++;
                    }
                    case "f" -> {
                        if (inFunction)
                            throw new JILException("cannot define a function inside of a function");

                        String res = tc.check(1, TokenChecker.TMatcher.any());
                        if (res != null)
                            throw new JILException(res);

                        ln++;
                    }
                    case "def" -> {
                        inFnChecker.check();
                        String res = tc.check(1, TokenChecker.TMatcher.any());
                        if (res != null)
                            throw new JILException(res);

                        String name = tl[1].content();

                        if (tl.length == 2)
                            throw new JILException("expected expression, but found EOL instead");

                        res = tc.check(2, TokenChecker.TMatcher.str());
                        int varValue = 0;

                        if (res == null) {
                            String str = tl[2].content();
                            int ptr = memory.malloc(str.length());
                            memory.derefString(ptr, str);
                            setRawVar(name, ptr, true);
                        } else {
                            int evalRes = eval(Arrays.copyOfRange(tl, 2, tl.length));
                            setVar(name, evalRes, true);
                        }

                        ln++;
                    }
                    case "defptr" -> {
                        ln++;
                    }
                    case "call" -> {
                        inFnChecker.check();

                        String res = tc.checkAll(1, 4, TokenChecker.TMatcher.opt("into"), TokenChecker.TMatcher.any(), TokenChecker.TMatcher.any());

                        String fname;
                        int argOffset = 2;
                        int[] args;
                        String outVar = null;
                        if (res == null) {
                            fname = tl[3].content();
                            outVar = tl[2].content();
                            argOffset = 4;
                        } else {
                            String res2 = tc.check(1, TokenChecker.TMatcher.any());
                            if (res2 != null)
                                throw new JILException(res2);

                            fname = tl[1].content();
                        }

                        JILToken[] tArgs = Arrays.copyOfRange(tl, argOffset, tl.length);
                        args = new int[tArgs.length];

                        for (int i = 0; i < tArgs.length; i++)
                            args[i] = getRawVar(tArgs[i].content());

                        JILFunction f = getFunc(fname);

                        int ret = f.run(file, memory, args);

                        if (outVar != null)
                            setVar(outVar, ret);

                        ln++;
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
