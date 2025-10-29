package mlp.Lexico;

public class Token {
    private final TokenTipo tipo;
    private final String lexema;
    private final int linha;   // 1-based
    private final int coluna;  // 1-based

    public Token(TokenTipo tipo, String lexema, int linha, int coluna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
        this.coluna = coluna;
    }

    public TokenTipo getTipo() { return tipo; }
    public String getLexema()  { return lexema; }
    public int getLinha()      { return linha; }
    public int getColuna()     { return coluna; }

    @Override
    public String toString() {
        return tipo + "('" + lexema + "')@" + linha + ":" + coluna;
    }
}