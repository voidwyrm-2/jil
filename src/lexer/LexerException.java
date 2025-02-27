package lexer;

import runtime.errors.JILException;

public class LexerException extends JILException {
    public LexerException(String message) {
        super(message);
    }
}
