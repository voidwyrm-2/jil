package argparse;

import java.lang.reflect.ParameterizedType;

public class Flag<T> {
    private T value;
    private boolean happened;
    private final IFlagValueConverter<T> converter;
    private final String name;
    private final String description;

    public Flag(String name, T def, IFlagValueConverter<T> converter, String description) {
        value = def;
        happened = false;
        this.converter = converter;
        this.name = name;
        this.description = description;
    }

    public void set(String arg) throws ArgparseException {
        try {
            this.value = converter.convert(arg);
        } catch (Exception e) {
            throw new ArgparseException(String.format("invalid argument '%s' for flag '%s'", arg, name));
        }
    }

    public T get() {
        return value;
    }

    public void setHappened() {
        happened = true;
    }

    public boolean getHappened() {
        return happened;
    }

    @Override
    public String toString() {
        String[] cname = value.getClass().getName().split("\\.");
        return String.format("-%s %s\n    %s", name, cname[cname.length - 1], description);
    }
}
