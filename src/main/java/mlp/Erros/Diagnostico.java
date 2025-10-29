package mlp.Erros;

public class Diagnostico {
    public enum Tipo { LEXICO, SINTATICO, SEMANTICO }

    private final Tipo tipo;
    private final int codigo;        // ex.: 1, 2, 10...
    private final String mensagem;   // mensagem legível
    private final int linha;         // 1-based
    private final int coluna;        // 1-based
    private final String lexema;     // opcional

    public Diagnostico(Tipo tipo, int codigo, String mensagem, int linha, int coluna, String lexema) {
        this.tipo = tipo;
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.linha = linha;
        this.coluna = coluna;
        this.lexema = lexema;
    }

    public Tipo getTipo()    { return tipo; }
    public int getCodigo()   { return codigo; }
    public String getMensagem() { return mensagem; }
    public int getLinha()    { return linha; }
    public int getColuna()   { return coluna; }
    public String getLexema(){ return lexema; }

    @Override
    public String toString() {
        String lx = (lexema == null ? "" : " (lexema='" + lexema + "')");
        return "[" + tipo + "] [" + linha + ":" + coluna + "] COD." +
               String.format("%02d", codigo) + " - " + mensagem + lx;
    }
}
