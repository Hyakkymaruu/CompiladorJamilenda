package mlp.Lexico;

import java.util.ArrayList;
import java.util.List;

import mlp.Erros.Diagnostico;
import mlp.Erros.Diagnostico.Tipo;

public class AnalisadorLexico {

    // ---- Códigos locais de erro léxico (01xx) ----
    private static final int LEX_SIMBOLO_DESCONHECIDO = 101; // caractere não pertence ao alfabeto
    private static final int LEX_REAL_INVALIDO        = 102; // "5." (ponto sem dígitos após)
    private static final int LEX_IDENT_MALFORMADO     = 103; // reservado p/ futuro

    // ---- Entrada ----
    private final String fonte;
    private final int n;
    private int i = 0;        // índice no buffer
    private int linha = 1;    // 1-based
    private int coluna = 1;   // 1-based

    // ---- Saída ----
    private final List<Diagnostico> diagnosticos = new ArrayList<>();

    public AnalisadorLexico(String fonte) {
        this.fonte = fonte != null ? fonte : "";
        this.n = this.fonte.length();
    }

    public List<Diagnostico> getDiagnosticos() {
        return diagnosticos;
    }

    // ------------------------------------------------------
    // Interface pública
    // ------------------------------------------------------
    public Token proximo() {
        consumirEspacos();

        if (fim()) return novo(TokenTipo.EOF, "<EOF>", posLinha(), posColuna());

        char c = peek();

        // Delimitadores simples
        if (c == '(') { avancar(); return novo(TokenTipo.ABRE_PAR, "(", linha, coluna - 1); }
        if (c == ')') { avancar(); return novo(TokenTipo.FECHA_PAR, ")", linha, coluna - 1); }
        if (c == ',') { avancar(); return novo(TokenTipo.VIRGULA, ",", linha, coluna - 1); }
        if (c == ';') { avancar(); return novo(TokenTipo.PONTO_VIRG, ";", linha, coluna - 1); }
        if (c == '+') { avancar(); return novo(TokenTipo.OP_MAIS, "+", linha, coluna - 1); }
        if (c == '*') { avancar(); return novo(TokenTipo.OP_MULT, "*", linha, coluna - 1); }
        if (c == '/') { avancar(); return novo(TokenTipo.OP_DIV,  "/", linha, coluna - 1); }
        if (c == '=') {
            // '=' ou '=='
            if (lookaheadEq('=')) {
                avancar(); avancar();
                return novo(TokenTipo.OP_EQ, "==", linha, coluna - 2);
            } else {
                avancar();
                return novo(TokenTipo.OP_ATRIB, "=", linha, coluna - 1);
            }
        }
        if (c == '>') {
            if (lookaheadEq('=')) { avancar(); avancar(); return novo(TokenTipo.OP_GE, ">=", linha, coluna - 2); }
            avancar(); return novo(TokenTipo.OP_GT, ">", linha, coluna - 1);
        }
        if (c == '<') {
            if (lookaheadEq('=')) { avancar(); avancar(); return novo(TokenTipo.OP_LE, "<=", linha, coluna - 2); }
            avancar(); return novo(TokenTipo.OP_LT, "<", linha, coluna - 1);
        }
        if (c == '!') {
            if (lookaheadEq('=')) { avancar(); avancar(); return novo(TokenTipo.OP_NE, "!=", linha, coluna - 2); }
            // '!' sozinho não existe na MLP
            int lin = linha, col = coluna;
            avancar();
            erro(LEX_SIMBOLO_DESCONHECIDO, "símbolo não reconhecido", lin, col, "!");
            return novo(TokenTipo.INVALIDO, "!", lin, col);
        }

        // '$' e '$.' (START/END)
        if (c == '$') {
            int lin = linha, col = coluna;
            avancar();
            if (!fim() && peek() == '.') {
                avancar();
                return novo(TokenTipo.END, "$.", lin, col);
            }
            return novo(TokenTipo.START, "$", lin, col);
        }

        // Números (inteiro | real)
        if (isDigit(c) || c == '.') {
            return scanNumero();
        }

        // Identificadores / palavras reservadas / operador RESTO
        if (isLetter(c)) {
            return scanIdentOuReservado();
        }

        // Qualquer outro caractere
        int lin = linha, col = coluna;
        char inval = c;
        avancar();
        erro(LEX_SIMBOLO_DESCONHECIDO, "símbolo não reconhecido", lin, col, String.valueOf(inval));
        return novo(TokenTipo.INVALIDO, String.valueOf(inval), lin, col);
    }

    // ------------------------------------------------------
    // Scanners
    // ------------------------------------------------------

    private Token scanNumero() {
        int lin = linha, col = coluna;
        int ini = i;

        if (peek() == '.') {
            // Padrão: '.' digitos+  (válido)
            avancar(); // consome '.'
            if (!fim() && isDigit(peek())) {
                while (!fim() && isDigit(peek())) avancar();
                String lex = fonte.substring(ini, i);
                return novo(TokenTipo.NUM_REAL, lex, lin, col);
            } else {
                // '.' não seguido de dígito -> não é número válido
                erro(LEX_SIMBOLO_DESCONHECIDO, "símbolo não reconhecido", lin, col, ".");
                return novo(TokenTipo.INVALIDO, ".", lin, col);
            }
        }

        // dígitos+ (inteiro potencialmente seguido de parte fracionária)
        while (!fim() && isDigit(peek())) avancar();

        // parte fracionária?
        if (!fim() && peek() == '.') {
            // precisa ter dígitos depois do ponto para ser real
            if (lookaheadDigit()) {
                avancar(); // consome '.'
                while (!fim() && isDigit(peek())) avancar();
                String lex = fonte.substring(ini, i);
                return novo(TokenTipo.NUM_REAL, lex, lin, col);
            } else {
                // "5." -> real inválido
                String lex = fonte.substring(ini, i + 1); // inclui o '.'
                // Consome o '.' para não travar
                avancar();
                erro(LEX_REAL_INVALIDO, "número real inválido (faltam dígitos após '.')", lin, col, lex);
                // Retorna inteiro com os dígitos escaneados originalmente
                String onlyInt = fonte.substring(ini, i - 1);
                return novo(TokenTipo.NUM_INT, onlyInt, lin, col);
            }
        }

        String lex = fonte.substring(ini, i);
        return novo(TokenTipo.NUM_INT, lex, lin, col);
    }

    private Token scanIdentOuReservado() {
        int lin = linha, col = coluna;
        int ini = i;
        // primeira letra
        avancar();
        // letras/dígitos subsequentes
        while (!fim() && (isLetter(peek()) || isDigit(peek()))) {
            avancar();
        }
        String lex = fonte.substring(ini, i);

        // Palavras reservadas
        switch (lex) {
            case "se":       return novo(TokenTipo.KW_SE, lex, lin, col);
            case "entao":    return novo(TokenTipo.KW_ENTAO, lex, lin, col);
            case "senao":    return novo(TokenTipo.KW_SENAO, lex, lin, col);
            case "enquanto": return novo(TokenTipo.KW_ENQUANTO, lex, lin, col);
            case "inteiro":  return novo(TokenTipo.KW_INTEIRO, lex, lin, col);
            case "real":     return novo(TokenTipo.KW_REAL, lex, lin, col);
            case "caracter": return novo(TokenTipo.KW_CARACTER, lex, lin, col);
            case "E":        return novo(TokenTipo.KW_E, lex, lin, col);
            case "OU":       return novo(TokenTipo.KW_OU, lex, lin, col);
            case "NAO":      return novo(TokenTipo.KW_NAO, lex, lin, col);
            case "RESTO":    return novo(TokenTipo.OP_RESTO, lex, lin, col);
            default:         return novo(TokenTipo.IDENT, lex, lin, col);
        }
    }

    // ------------------------------------------------------
    // Utilitários
    // ------------------------------------------------------
    private boolean fim() { return i >= n; }
    private char peek()   { return fonte.charAt(i); }
    private void avancar() {
        char c = fonte.charAt(i++);
        if (c == '\n') { linha++; coluna = 1; } else { coluna++; }
    }
    private boolean lookaheadEq(char ch) {
        return (i + 1 < n) && (fonte.charAt(i + 1) == ch);
    }
    private boolean lookaheadDigit() {
        return (i + 1 < n) && Character.isDigit(fonte.charAt(i + 1));
    }
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z');
    }
    private int posLinha() { return linha; }
    private int posColuna(){ return coluna; }

    private void consumirEspacos() {
        while (!fim()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                avancar();
            } else {
                break;
            }
        }
    }

    private Token novo(TokenTipo tipo, String lexema, int lin, int col) {
        return new Token(tipo, lexema, lin, col);
    }

    private void erro(int codigo, String msg, int lin, int col, String lex) {
        diagnosticos.add(new Diagnostico(Tipo.LEXICO, codigo, msg, lin, col, lex));
    }

    // ------------------------------------------------------
    // NOVO: listarTodos() — para compatibilidade com o Main
    // Não altera o estado do léxico atual: usa um léxico temporário.
    // ------------------------------------------------------
    public List<Token> listarTodos() {
        List<Token> out = new ArrayList<>();
        AnalisadorLexico tmp = new AnalisadorLexico(this.fonte);
        Token t;
        do {
            t = tmp.proximo();
            out.add(t);
        } while (t.getTipo() != TokenTipo.EOF);
        // Se quiser refletir também os diagnósticos capturados nesta passagem:
        // this.diagnosticos.addAll(tmp.getDiagnosticos());
        return out;
    }
}
