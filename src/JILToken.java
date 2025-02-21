public record JILToken(String content, boolean isString) {
    @Override
    public String toString() {
        return String.format("{`%s`, isString? %s}", content, isString ? "yes" : "no");
    }
}
