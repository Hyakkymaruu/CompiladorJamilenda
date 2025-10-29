package mlp.Lexico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mlp.Erros.Diagnostico;
import mlp.Erros.Diagnostico.Tipo;

public class AnalisadorLexico {

    private final String fonte;
    private final int n;
    private int i = 0;           // índice atual (0-based)
    private int linha = 1;       // 1-based
    private int coluna = 1;      // 1-based

    private final List<Diagnostico> diagnosticos = new ArrayList<>();

    private static final Map<String, TokenTipo> PALAVRAS = new HashMap<>();
    static {
        // minúsculas
        PALAVRAS.put("se", TokenTipo.KW_SE);
        PALAVRAS.put("entao", TokenTipo.KW_ENTAO);
        PALAVRAS.put("senao", TokenTipo.KW_SENAO);
        PALAVRAS.put("enquanto", TokenTipo.KW_ENQUANTO);
        PALAVRAS.put("inteiro", TokenTipo.KW_INTEIRO);
        PALAVRAS.put("real", TokenTipo.KW_REAL);
        PALAVRAS.put("caracter", TokenTipo.KW_CARACTER);
        // maiúsculas
        PALAVRAS.put("E", TokenTipo.KW_E);
        PALAVRAS.put("OU", TokenTipo.KW_OU);
        PALAVRAS.put("NAO", TokenTipo.KW_NAO);
        // operador-palavra
        PALAVRAS.put("RESTO", TokenTipo.OP_RESTO);
    }

    public AnalisadorLexico(String fonte) {
        this.fonte = (fonte == null ? "" : fonte);
        this.n = this.fonte.length();
    }

    public List<Diagnostico> getDiagnosticos() {
        return diagnosticos;
    }

    public List<Token> listarTodos() {
        List<Token> out = new ArrayList<>();
        Token t;
        do {
            t = proximo();
            out.add(t);
        } while (t.getTipo() != TokenTipo.EOF);
        return out;
    }

    public Token proximo() {
        ignorarEspacosEComentarios();

        if (fim()) return novo(TokenTipo.EOF, "");

        char c = peek();

        // $., depois $
        if (c == '$') {
            if (match("$."))
                return novo(TokenTipo.END, "$.");
            advance(); // consome apenas '$'
            return novo(TokenTipo.START, "$");
        }

        // operadores de 2 chars
        if (match("==")) return novo(TokenTipo.OP_EQ, "==");
        if (match("!=")) return novo(TokenTipo.OP_NE, "!=");
        if (match("<=")) return novo(TokenTipo.OP_LE, "<=");
        if (match(">=")) return novo(TokenTipo.OP_GE, ">=");

        // símbolos simples
        if (c == '(') { advance(); return novo(TokenTipo.ABRE_PAR, "("); }
        if (c == ')') { advance(); return novo(TokenTipo.FECHA_PAR, ")"); }
        if (c == ',') { advance(); return novo(TokenTipo.VIRGULA, ","); }
        if (c == ';') { advance(); return novo(TokenTipo.PONTO_VIRG, ";"); }
        if (c == '+') { advance(); return novo(TokenTipo.OP_MAIS, "+"); }
        if (c == '*') { advance(); return novo(TokenTipo.OP_MULT, "*"); }
        if (c == '/') { advance(); return novo(TokenTipo.OP_DIV, "/"); }
        if (c == '<') { advance(); return novo(TokenTipo.OP_LT, "<"); }
        if (c == '>') { advance(); return novo(TokenTipo.OP_GT, ">"); }
        if (c == '=') { advance(); return novo(TokenTipo.OP_ATRIB, "="); }

        // identificadores / palavras-chave / RESTO
        if (isLetter(c)) {
            int startCol = coluna;             // <-- removido startI (não usado)
            String word = lerIdentOuPalavra();
            TokenTipo t = PALAVRAS.get(word);
            if (t != null) {
                if (t == TokenTipo.OP_RESTO && !fronteiraPalavra())
                    return novoToken(TokenTipo.IDENT, word, linha, startCol);
                return novoToken(t, word, linha, startCol);
            }
            return novoToken(TokenTipo.IDENT, word, linha, startCol);
        }

        // números (real antes de inteiro)
        if (isDigit(c)) {
            int startCol = coluna;
            String num = lerNumero();
            if (num.indexOf('.') >= 0) {
                String[] partes = num.split("\\.", -1);
                if (partes.length == 2 && !partes[0].isEmpty() && !partes[1].isEmpty()) {
                    return novoToken(TokenTipo.NUM_REAL, num, linha, startCol);
                } else {
                    diagnosticos.add(new Diagnostico(
                        Tipo.LEXICO, 2, "número malformado", linha, startCol, num));
                    return novoToken(TokenTipo.INVALIDO, num, linha, startCol);
                }
            } else {
                return novoToken(TokenTipo.NUM_INT, num, linha, startCol);
            }
        }

        // caractere inválido
        int col = coluna;
        String invalido = String.valueOf(c);
        diagnosticos.add(new Diagnostico(Tipo.LEXICO, 1, "símbolo desconhecido", linha, col, invalido));
        advance();
        return novoToken(TokenTipo.INVALIDO, invalido, linha, col);
    }

    // ---------------- utilidades ----------------

    private void ignorarEspacosEComentarios() {
        boolean consumiu;
        do {
            consumiu = false;
            // whitespace
            while (!fim()) {
                char c = peek();
                if (c == ' ' || c == '\t' || c == '\r') { advance(); consumiu = true; }
                else if (c == '\n') { advance(); consumiu = true; }
                else break;
            }
            // // linha
            if (match("//")) {
                consumiu = true;
                while (!fim() && peek() != '\n') advance();
            }
            // /* bloco */
            if (match("/*")) {
                consumiu = true;
                boolean fechado = false;
                while (!fim()) {
                    if (match("*/")) { fechado = true; break; }
                    advance();
                }
                if (!fechado) {
                    diagnosticos.add(new Diagnostico(Tipo.LEXICO, 3, "comentário não terminado",
                            linha, coluna, null));
                }
            }
        } while (consumiu);
    }

    private String lerIdentOuPalavra() {
        int start = i;
        advance(); // já tinha 1 letra
        while (!fim() && (isLetter(peek()) || isDigit(peek()))) {
            advance();
        }
        return fonte.substring(start, i);
    }

    private String lerNumero() {
        int start = i;
        while (!fim() && isDigit(peek())) advance();
        if (!fim() && peek() == '.') {
            int saveI = i, saveLinha = linha, saveCol = coluna;
            advance(); // consome '.'
            if (!fim() && isDigit(peek())) {
                while (!fim() && isDigit(peek())) advance();
                return fonte.substring(start, i);
            } else {
                i = saveI; linha = saveLinha; coluna = saveCol;
            }
        }
        return fonte.substring(start, i);
    }

    private boolean fronteiraPalavra() {
        if (fim()) return true;
        char c = peek();
        return !(isLetter(c) || isDigit(c));
    }

    private Token novo(TokenTipo tipo, String lexema) {
        int startCol = Math.max(1, coluna - Math.max(lexema.length() - 1, 0));
        return new Token(tipo, lexema, linha, startCol);
    }
    private Token novoToken(TokenTipo tipo, String lexema, int lin, int col) {
        return new Token(tipo, lexema, lin, col);
    }

    private boolean match(String s) {
        if (i + s.length() > n) return false;
        for (int k = 0; k < s.length(); k++) {
            if (fonte.charAt(i + k) != s.charAt(k)) return false;
        }
        for (int k = 0; k < s.length(); k++) advance();
        return true;
    }

    private char peek() { return fonte.charAt(i); }

    private void advance() {
        char c = fonte.charAt(i++);
        if (c == '\n') { linha++; coluna = 1; }
        else { coluna++; }
    }

    private boolean fim() { return i >= n; }

    private static boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
    private static boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }
}
