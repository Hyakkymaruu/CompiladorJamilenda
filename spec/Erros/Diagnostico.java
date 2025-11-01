package mlp.Erros;

public class Diagnostico {

    public enum Tipo { LEXICO, SINTATICO, SEMANTICO }

    private final Tipo tipo;
    private final int codigo;
    private final String mensagem;
    private final int linha;
    private final int coluna;
    private final String lexema;

    // ---------- Códigos padronizados ----------
    // Léxico 01xx
    public static final int LEX_SIMBOLO_DESCONHECIDO   = 101;
    public static final int LEX_REAL_INVALIDO          = 102;
    public static final int LEX_IDENT_MALFORMADO       = 103;

    // Sintático 10xx
    public static final int SIN_COMANDO_INVALIDO       = 1001;
    public static final int SIN_ESPERAVA_START         = 1002;
    public static final int SIN_ESPERAVA_END           = 1003;
    public static final int SIN_ESPERAVA_TIPO          = 1004;
    public static final int SIN_ESPERAVA_IDENT         = 1005;
    public static final int SIN_ESPERAVA_PV_DECL       = 1006;
    public static final int SIN_ESPERAVA_IDENT_APOS_V  = 1007;
    public static final int SIN_ESPERAVA_OP_REL        = 1010;
    public static final int SIN_OPERANDO_REL_INVALIDO  = 1011;
    public static final int SIN_ESPERAVA_FP_EXP        = 1012; // fecha parêntese após expressão
    public static final int SIN_ESPERAVA_FP_COND       = 1013; // fecha parêntese após condição
    public static final int SIN_ESPERAVA_ENTAO         = 1014;
    public static final int SIN_ESPERAVA_ATR           = 1015; // '='
    public static final int SIN_FATOR_INVALIDO         = 1016;
    public static final int SIN_ESPERAVA_PV_ATR        = 1017;

    // Semântico 20xx
    public static final int SEM_VAR_NAO_DECL           = 2001;
    public static final int SEM_VAR_REDECLARADA        = 2002;
    public static final int SEM_TIPO_INCOMPATIVEL      = 2003;
    public static final int SEM_COND_NAO_BOLEANA       = 2004;

    // ---------- Construtores ----------
    public Diagnostico(Tipo tipo, int codigo, String mensagem, int linha, int coluna, String lexema) {
        this.tipo = tipo;
        this.codigo = codigo;
        this.mensagem = mensagem;
        this.linha = linha;
        this.coluna = coluna;
        this.lexema = lexema;
    }

    // Helpers estáticos
    public static Diagnostico lex(int codigo, String msg, int lin, int col, String lex) {
        return new Diagnostico(Tipo.LEXICO, codigo, msg, lin, col, lex);
    }
    public static Diagnostico sin(int codigo, String msg, int lin, int col, String lex) {
        return new Diagnostico(Tipo.SINTATICO, codigo, msg, lin, col, lex);
    }
    public static Diagnostico sem(int codigo, String msg, int lin, int col, String lex) {
        return new Diagnostico(Tipo.SEMANTICO, codigo, msg, lin, col, lex);
    }

    // ---------- Getters ----------
    public Tipo getTipo()         { return tipo; }
    public int getCodigo()        { return codigo; }
    public String getMensagem()   { return mensagem; }
    public int getLinha()         { return linha; }
    public int getColuna()        { return coluna; }
    public String getLexema()     { return lexema; }

    @Override
    public String toString() {
        String tip = switch (tipo) {
            case LEXICO -> "LEXICO";
            case SINTATICO -> "SINTATICO";
            case SEMANTICO -> "SEMANTICO";
        };
        return String.format("[%s] [%d:%d] COD.%d - %s (lexema='%s')",
                tip, linha, coluna, codigo, mensagem, lexema);
    }
}
