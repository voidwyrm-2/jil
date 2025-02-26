package lexer;

import runtime.JILException;

import java.util.ArrayList;

public class Lexer {
    private final String text;

    private class LineLexer {
        private final String line;
        private int idx, col;
        private final int ln;
        private Character ch;

        LineLexer(String line, int ln) {
            this.line = line;
            idx = -1;
            col = 0;
            this.ln = ln;
            ch = null;
            advance();
        }

        private void advance(int amount) {
            idx++;
            col++;

            ch = idx < line.length() ? line.charAt(idx) : null;
        }

        private void advance() {
            advance(1);
        }

        private Token collectString() throws LexerException {
            int start = col;
            int startln = ln;
            StringBuilder str = new StringBuilder();
            boolean escaped = false;

            advance();

            while (ch != null) {
                if (escaped) {
                    switch (ch) {
                        case '\\', '\'', '"' -> str.append(ch);
                        case 'n' -> str.append('\n');
                        case 't' -> str.append('\t');
                        case 'v' -> str.append((char) 11);
                        case 'a' -> str.append((char) 7);
                        case 'f' -> str.append('\f');
                        case 'r' -> str.append('\r');
                        case '0' -> str.append('\0');
                        default -> throw new LexerException(Token.dummy(col, ln).format("invalid escape character '%c'", ch));
                    }
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    break;
                } else {
                    str.append(ch);
                }
                advance();
            }

            if (ch == null)
                throw new LexerException(Token.dummy(start, startln).format("unterminated string literal"));

            advance();

            return new Token(TokenType.String, str.toString(), start, startln);
        }

        Token[] lex() throws LexerException {
            ArrayList<Token> tokens = new ArrayList<>();

            StringBuilder acc = null;
            int start = -1;
            int startln = -1;

            while (ch != null) {
                if (ch == '"') {
                    if (acc != null) {
                        tokens.add(new Token(TokenType.Ident, acc.toString(), start, startln));
                        acc = null;
                    }
                    tokens.add(collectString());
                } else if (ch <= 20 || ch == ' ') {
                    if (acc != null) {
                        tokens.add(new Token(TokenType.Ident, acc.toString(), start, startln));
                        acc = null;
                    }
                    advance();
                } else {
                    if (acc == null) {
                        start = col;
                        startln = ln;
                        acc = new StringBuilder();
                    }

                    acc.append(ch);

                    advance();
                }
            }

            if (acc != null)
                tokens.add(new Token(TokenType.Ident, acc.toString(), start, startln));

            return tokens.toArray(new Token[0]);
        }
    }

    public Lexer(String text) {
        this.text = text;
    }

    public Token[][] lex() throws JILException {
        ArrayList<Token[]> tokens = new ArrayList<>();

        int ln = 1;
        for (String line : text.split("\n")) {
            Token[] tl = new LineLexer(line, ln).lex();
            if (tl.length > 0)
                tokens.add(tl);

            ln++;
        }

        return tokens.toArray(new Token[0][]);
    }
}
