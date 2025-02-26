package argparse;

import java.util.ArrayList;
import java.util.HashMap;

public class Argparser {
    private final HashMap<String, Flag<?>> flags;
    private final String name;

    public Argparser(String name) {
        this.name = name;
        flags = new HashMap<>();
    }

    public <T> Flag<T> addFlag(String name, T def, IFlagValueConverter<T> converter, String description) {
        if (flags.containsKey(name))
            return null;

        Flag<T> f = new Flag<>(name, def, converter, description);
        flags.put(name, f);
        return f;
    }

    public String[] parse(String[] source) throws ArgparseException {
        ArrayList<String> leftover = new ArrayList<>();

        for (int i = 0; i < source.length; i++) {
            if (source[i].startsWith("-")) {
                String name = source[i];
                if (name.startsWith("--")) {
                    name = name.substring(2);
                } else {
                    name = name.substring(1);
                }

                if (!flags.containsKey(name))
                    throw new ArgparseException("unknown flag '" + source[i] + "'");

                Flag<?> f = flags.get(name);
                f.setHappened();

                if (f.get().getClass().equals(Boolean.class)) {
                    f.set((Boolean)f.get() ? "false" : "true");
                } else {
                    if (i + 1 >= source.length)
                        throw new ArgparseException("expected argument for flag '" + source[i] + "'");

                    i += 1;
                    f.set(source[i]);
                }
            } else {
                leftover.add(source[i]);
            }
        }

        return leftover.toArray(new String[0]);
    }

    public String help() {
        StringBuilder msg = new StringBuilder();
        msg.append("Usage of ").append(name);
        for (Flag<?> f : flags.values())
            msg.append("\n  ").append(f.toString());

        return msg.toString();
    }
}
