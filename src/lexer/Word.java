package lexer;

public class Word extends Token {
    public final String lexeme;

    public Word(int tag, String lexeme) {
        super(tag);
        this.lexeme = lexeme;
    }

    @Override
    public String toString() {
        return "Word<" + tag + ", " + lexeme + ">";
    }
}
