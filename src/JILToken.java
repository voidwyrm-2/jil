import java.util.Objects;

public class JILToken {
    private String content;
    private boolean isString;

    public JILToken(String content, boolean isString) {
        this.content = content;
        this.isString = isString;
    }

    public boolean is(String cmp) {
        return Objects.equals(content, cmp);
    }

    public boolean isString() {
        return isString;
    }

    @Override
    public String toString() {
        return String.format("{`%s`, isString? %s}", content, isString ? "yes" : "no");
    }
}
