package runtime;

import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;

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

    private record TokenChecker(Token[] tokens) {
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
                ethrow = !tokens[index].is(TokenType.String);
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

        private String checkAll(int start, TMatcher... matchers) {
            return checkAll(start, tokens.length, matchers);
        }

        private String checkAll(TMatcher... matchers) {
            return checkAll(1, matchers);
        }
    }

    private record InFnChecker(boolean inFunction, String operation) {
        void check() throws JILException {
            if (!inFunction)
                throw new JILException(String.format("cannot use '%s' operation outside of function", operation));
        }
    }

    public JILInterpreter(Integer memorySize) {
        memory = new JILMemory(memorySize);
        vars = new HashMap<>();
        funcs = new HashMap<>();
    }

    public JILInterpreter(JILMemory outerMemory, HashMap<String, JILFunction> funcs) {
        memory = outerMemory;
        vars = new HashMap<>();
        this.funcs = funcs;
    }

    public JILMemory.MemoryDebug memoryDebug() {
        return memory.debug;
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

    public final JILFunction getFuncUnguarded(String name) {
        return funcs.get(name);
    }

    public final void loadModule(String path) throws JILException {
        loadModule(new File(path));
    }

    public final void loadModule(File path) throws JILException {
        String[] spath = path.getAbsolutePath().split(Pattern.quote(File.separator));

        String modclass = spath[spath.length - 1];
        if (modclass.endsWith(".class"))
            modclass = modclass.substring(0, modclass.length() - 6);

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
                if (fname.isBlank() || fname.isEmpty())
                    fname = m.getName();

                defFunc(fname, new JILFunction(m));
            }
        }
    }

    public final int eval(Token[] tokens) throws JILException {
        Stack<Integer> estack = new Stack<>();

        for (Token t : tokens) {
            if (t.is(TokenType.String))
                throw new JILException("cannot use strings in expressions");

            JILException n2op = new JILException("expected one operands on the stack for '" + t.content() + "', but found " + estack.size() + " instead");

            JILException n1op = new JILException("expected one operand on the stack for '" + t.content() + "', but found " + estack.size() + " instead");

            try {
                estack.add(Integer.parseInt(t.content()));
            } catch (NumberFormatException e) {
                switch (t.content()) {
                    case "+" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        estack.add(estack.pop() + b);
                    }
                    case "-" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        estack.add(estack.pop() - b);
                    }
                    case "*" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        estack.add(estack.pop() * b);
                    }
                    case "/" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        estack.add(estack.pop() / b);
                    }
                    case "%" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        estack.add(estack.pop() % b);
                    }
                    case "**" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        estack.add((int)Math.pow((double)estack.pop(), (double)b));
                    }
                    case "and" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        estack.add(estack.pop() != 0 && b != 0 ? 1 : 0);
                    }
                    case "or" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        estack.add(estack.pop() != 0 || b != 0 ? 1 : 0);
                    }
                    case "=" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        estack.add(estack.pop().equals(b) ? 1 : 0);
                    }
                    case "!=" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        estack.add(estack.pop().equals(b) ? 0 : 1);
                    }
                    case ">", ">=" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        Integer a = estack.pop();
                        estack.add(a > b || (t.content().endsWith("=") && a.equals(b)) ? 1 : 0);
                    }
                    case "<", "<=" -> {
                        if (estack.size() < 2)
                            throw n2op;

                        Integer b = estack.pop();
                        Integer a = estack.pop();
                        estack.add(a < b || (t.content().endsWith("=") && a.equals(b)) ? 1 : 0);
                    }
                    case "!" -> {
                        if (estack.empty())
                            throw n1op;

                        estack.add(estack.pop() == 0 ? 1 : 0);
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

    public int runMain(int... args) throws JILException {
        JILFunction mainf = getFuncUnguarded("main");
        if (mainf == null)
            throw new JILException("function 'main' not found");

        return mainf.run("main", memory, funcs, args);
    }

    public int execute(String file, boolean inFunction, Token[][] tokenLines) throws JILException {
        HashMap<String, Integer> labels = new HashMap<>();
        int ln = 0;
        Token ct = null;

        try {
            while (ln < tokenLines.length) {
                Token[] tl = tokenLines[ln];
                TokenChecker tc = new TokenChecker(tl);
                InFnChecker inFnChecker = new InFnChecker(inFunction, tl[0].content());

                ct = tl[0];

                if (tl[0].is(TokenType.String))
                    throw new JILException("unexpected string");

                switch (tl[0].content()) {
                    case "rem" -> ln++;
                    case "lbl" -> {
                        inFnChecker.check();

                        String res = tc.checkAll(TokenChecker.TMatcher.any());
                        if (res != null)
                            throw new JILException(res);

                        String label = tl[1].content();

                        if (labels.containsKey(label))
                            throw new JILException("cannot redefine label '" + label + "'");

                        labels.put(label, ln + 1);

                        ln++;
                    }
                    case "goto" -> {
                        inFnChecker.check();

                        String res = tc.checkAll(TokenChecker.TMatcher.any());
                        if (res != null)
                            throw new JILException(res);

                        String label = tl[1].content();

                        if (!labels.containsKey(label))
                            throw new JILException("unknown label '" + label + "'");

                        ln = labels.get(label);
                    }
                    case "import" -> {
                        if (inFunction)
                            throw new JILException("cannot import a module inside of a function");

                        String res = tc.check(1, TokenChecker.TMatcher.str());
                        if (res != null)
                            throw new JILException(res);

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
                    case "fun" -> {
                        if (inFunction)
                            throw new JILException("cannot define a function inside of a function");

                        String res = tc.check(1, TokenChecker.TMatcher.any());
                        if (res != null)
                            throw new JILException(res);

                        String res2 = tc.check(2, TokenChecker.TMatcher.any());

                        String name = tl[1].content();
                        int argc = 0;
                        if (res2 == null) {
                            try {
                                argc = Integer.parseInt(tl[2].content());
                            } catch (NumberFormatException e) {
                                throw new JILException("function argument count must be a number");
                            }
                        }

                        ArrayList<Token[]> acc = new ArrayList<>();

                        int seek = ln + 1;
                        while (seek < tokenLines.length) {
                            Token[] subTL = tokenLines[seek];
                            TokenChecker subTC = new TokenChecker(subTL);

                            res = subTC.checkAll(0, TokenChecker.TMatcher.opt("end"));
                            if (res == null)
                                break;

                            if (seek == tokenLines.length - 1)
                                throw new JILException("unterminated function definition");

                            acc.add(subTL);

                            seek++;
                        }

                        defFunc(name, new JILFunction(acc.toArray(new Token[0][]), argc));

                        ln = seek;

                        ln++;
                    }
                    case "struct" -> {
                        if (inFunction)
                            throw new JILException("cannot define a struct inside of a function");

                        String res = tc.check(1, TokenChecker.TMatcher.any());
                        if (res != null)
                            throw new JILException(res);

                        String name = tl[1].content();

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
                        if (res == null && tl.length == 3) {
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
                    case "set" -> {
                        inFnChecker.check();

                        String res = tc.check(1, TokenChecker.TMatcher.any());
                        if (res != null)
                            throw new JILException(res);

                        String name = tl[1].content();

                        if (tl.length == 2)
                            throw new JILException("expected expression, but found EOL instead");

                        res = tc.check(2, TokenChecker.TMatcher.str());
                        if (res == null && tl.length == 3) {
                            String str = tl[2].content();
                            int ptr = memory.malloc(str.length());
                            memory.derefString(ptr, str);
                            setRawVar(name, ptr, false);
                        } else {
                            int evalRes = eval(Arrays.copyOfRange(tl, 2, tl.length));
                            setVar(name, evalRes, false);
                        }

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

                        Token[] tArgs = Arrays.copyOfRange(tl, argOffset, tl.length);
                        args = new int[tArgs.length];

                        for (int i = 0; i < tArgs.length; i++)
                            args[i] = getRawVar(tArgs[i].content());

                        JILFunction f = getFunc(fname);

                        int ret = f.run(fname, memory, funcs, args);

                        if (outVar != null)
                            setVar(outVar, ret);

                        ln++;
                    }
                    case "ret" -> {
                        inFnChecker.check();

                        if (tl.length == 1)
                            throw new JILException("expected expression, but found EOL instead");

                        String res = tc.check(2, TokenChecker.TMatcher.str());
                        if (res == null && tl.length == 2) {
                            String str = tl[1].content();
                            int ptr = memory.malloc(str.length());
                            memory.derefString(ptr, str);

                            return ptr;
                        } else {
                            return eval(Arrays.copyOfRange(tl, 1, tl.length));
                        }
                    }
                    case "if", "ifn" -> {
                        inFnChecker.check();

                        if (tl.length == 1)
                            throw new JILException("expected expression, but found EOL instead");

                        boolean cond = eval(Arrays.copyOfRange(tl, 1, tl.length)) != 0;

                        if (tl[0].content().endsWith("n"))
                            cond = !cond;

                        ln += cond ? 1 : 2;
                    }
                    default -> throw new JILException(String.format("unexpected token '%s'", tl[0].content()));
                }
            }
        } catch (JILException e) {
            String[] s = ct.format(e.getMessage()).split(":", 2);
            throw new JILException(s[0] + " of " + file + ":" + s[1]);
        }

        return 0;
    }

    public int execute(String file, boolean inFunction, String text) throws JILException {
        return execute(file, inFunction, new Lexer(text).lex());
    }
}
