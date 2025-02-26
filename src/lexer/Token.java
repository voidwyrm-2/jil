package lexer;

public record Token(TokenType kind, String content, int col, int ln) {
    public boolean is(TokenType kind) {
        return this.kind == kind;
    }

    public boolean is(String content) {
        return this.content.equals(content);
    }

    public static Token dummy(int col, int ln) {
        return new Token(TokenType.None, "", col, ln);
    }

    public String format(String fmt, Object... args) {
        return String.format("error on line %d, col %d: %s", ln, col, String.format(fmt, args));
    }

    @Override
    public String toString() {
        return String.format("{%s, `%s`, %d, %d}", kind, content, col, ln);
    }
}
