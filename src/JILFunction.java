public record JILFunction(JILToken[][] tokens, int args) {
    public int runf(String file, JILMemory outerMemory) throws JILException {
        JILInterpreter interpreter = new JILInterpreter(outerMemory);
        return interpreter.execute(file, true, tokens);
    }
}
