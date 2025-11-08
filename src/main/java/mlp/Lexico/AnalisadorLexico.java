package mlp.Lexico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mlp.Erros.Diagnostico;
import mlp.Erros.Diagnostico.Tipo;

/**
 * Analisador léxico da MLP.
 * - Mantém compatibilidade com os tokens já usados no Sintático.
 * - Adiciona diagnósticos léxicos padronizados (códigos 01xx).
 *
 * Códigos emitidos:
 *  0101 - símbolo não reconhecido
 *  0102 - número real inválido (ex.: "5.", ".5")
 *  0103 - identificador malformado (ex.: contém '_' na MLP atual)
 *  0104 - identificador excede tamanho máximo (10 caracteres)
 */
public class AnalisadorLexico {

    // ------------------- Códigos Léxicos -------------------
    private static final int LEX_SIMBOLO_DESCONHECIDO = 101; // 0101 no catálogo
    private static final int LEX_REAL_INVALIDO        = 102; // 0102
    private static final int LEX_IDENT_MALFORMADO     = 103; // 0103
    private static final int LEX_IDENT_TAM_EXCEDIDO   = 104; // 0104

    // ------------------- Estado -------------------
    private final String fonte;
    private final int n;
    private int i = 0;
    private int linha = 1;
    private int coluna = 1;

    private final List<Diagnostico> diagnosticos = new ArrayList<>();

    public AnalisadorLexico(String fonte) {
        this.fonte = (fonte == null) ? "" : fonte;
        this.n = this.fonte.length();
    }

    public List<Diagnostico> getDiagnosticos() {
        return diagnosticos;
    }

    // ------------------- API -------------------
    public Token proximo() {
        consumirEspacos();

        if (fim()) return new Token(TokenTipo.EOF, "<eof>", linha, coluna);

        char c = peek();

        // Delimitadores/operadores simples
        switch (c) {
            case '(' -> { advance(); return tok(TokenTipo.ABRE_PAR, "("); }
            case ')' -> { advance(); return tok(TokenTipo.FECHA_PAR, ")"); }
            case ',' -> { advance(); return tok(TokenTipo.VIRGULA, ","); }
            case ';' -> { advance(); return tok(TokenTipo.PONTO_VIRG, ";"); }
            case '+' -> { advance(); return tok(TokenTipo.OP_MAIS, "+"); }
            case '*' -> { advance(); return tok(TokenTipo.OP_MULT, "*"); }
            case '/' -> { advance(); return tok(TokenTipo.OP_DIV, "/"); }
            case '=' -> {
                advance();
                if (match('=')) return tok(TokenTipo.OP_EQ, "==");
                return tok(TokenTipo.OP_ATRIB, "=");
            }
            case '!' -> {
                advance();
                if (match('=')) return tok(TokenTipo.OP_NE, "!=");
                // '!' isolado não existe na MLP
                addDiagLex(LEX_SIMBOLO_DESCONHECIDO, "símbolo não reconhecido", "!");
                return new Token(TokenTipo.INVALIDO, "!", linha, coluna);
            }
            case '<' -> {
                advance();
                if (match('=')) return tok(TokenTipo.OP_LE, "<=");
                return tok(TokenTipo.OP_LT, "<");
            }
            case '>' -> {
                advance();
                if (match('=')) return tok(TokenTipo.OP_GE, ">=");
                return tok(TokenTipo.OP_GT, ">");
            }
            case '$' -> {
                // START '$' ou END '$.'
                int lin = linha, col = coluna;
                advance();
                if (match('.')) return new Token(TokenTipo.END, "$.", lin, col);
                return new Token(TokenTipo.START, "$", lin, col);
            }
            default -> {
                // segue abaixo (ident/numero/palavra-chave/RESTO)
            }
        }

        // Palavra-chave / identificador / RESTO
        if (isLetra(c)) {
            return scanIdentOuPalavra();
        }

        // Número (int/real)
        if (isDigito(c) || c == '.') {
            return scanNumero();
        }

        // Qualquer outro símbolo é inválido
        String lex = String.valueOf(c);
        addDiagLex(LEX_SIMBOLO_DESCONHECIDO, "símbolo não reconhecido", lex);
        advance(); // consome para evitar loop
        return new Token(TokenTipo.INVALIDO, lex, linha, coluna);
    }

    // ------------------- Scanners -------------------
    private Token scanIdentOuPalavra() {
        int lin = linha, col = coluna;

        // primeira letra garantida
        StringBuilder sb = new StringBuilder();
        sb.append(peek());
        advance();

        boolean malformado = false;
        while (!fim()) {
            char ch = peek();
            if (isLetra(ch) || isDigito(ch)) {
                sb.append(ch);
                advance();
            } else if (ch == '_') {
                // não permitimos '_' na MLP -> malformado, mas seguimos consumindo '_' e prosseguimos
                malformado = true;
                sb.append(ch);
                advance();
            } else {
                break;
            }
        }
        String lex = sb.toString();

        // Palavras-chave (inclui tipos e lógicos)
        TokenTipo kw = KEYWORDS.get(lex);
        if (kw != null) {
            return new Token(kw, lex, lin, col);
        }

        // Operador "RESTO"
        if ("RESTO".equals(lex)) {
            return new Token(TokenTipo.OP_RESTO, lex, lin, col);
        }

        // Regra da MLP: identificador pode ter no máximo 10 caracteres
        if (lex.length() > 10) {
            addDiag(
                Tipo.LEXICO,
                LEX_IDENT_TAM_EXCEDIDO,
                "identificador excede tamanho máximo (10)",
                lin,
                col,
                lex
            );
            // ainda assim devolvemos IDENT para o parser continuar trabalhando
        }

        if (malformado) {
            addDiag(
                Tipo.LEXICO,
                LEX_IDENT_MALFORMADO,
                "identificador malformado",
                lin,
                col,
                lex
            );
        }

        return new Token(TokenTipo.IDENT, lex, lin, col);
    }

    private Token scanNumero() {
        int lin = linha, col = coluna;

        // Casos permitidos (adotados para este projeto):
        //  - inteiro: DIGIT+
        //  - real   : DIGIT+ '.' DIGIT+    (rejeitamos "5." e ".5")
        //
        // Observação: se vier ".5" ou "5." vamos acusar 0102 e tentar
        //             produzir NUM_REAL/INVALIDO de forma que o parser consiga seguir.

        StringBuilder sb = new StringBuilder();
        boolean temDigitosAntes = false;
        boolean temPonto = false;
        boolean temDigitosDepois = false;

        // Parte inteira (opcional se começar com '.')
        while (!fim() && isDigito(peek())) {
            temDigitosAntes = true;
            sb.append(peek());
            advance();
        }

        if (!fim() && peek() == '.') {
            temPonto = true;
            sb.append('.');
            advance();
            while (!fim() && isDigito(peek())) {
                temDigitosDepois = true;
                sb.append(peek());
                advance();
            }
        }

        String lex = sb.toString();

        if (!temPonto) {
            if (lex.isEmpty()) {
                // Começou com '.' sem dígitos antes: ".???"
                // Tentar coletar '.' + dígitos para formar um real inválido
                if (peekPrev() == '.') {
                    // coletar dígitos após o ponto para não travar
                    StringBuilder sb2 = new StringBuilder(".");
                    while (!fim() && isDigito(peek())) {
                        sb2.append(peek());
                        advance();
                    }
                    String bad = sb2.toString();
                    addDiagLex(LEX_REAL_INVALIDO, "número real inválido", bad);
                    return new Token(TokenTipo.NUM_REAL, bad, lin, col);
                }
                // fallback
                addDiagLex(LEX_SIMBOLO_DESCONHECIDO, "símbolo não reconhecido", String.valueOf(peek()));
                advance();
                return new Token(TokenTipo.INVALIDO, String.valueOf(peek()), lin, col);
            }
            // inteiro válido
            return new Token(TokenTipo.NUM_INT, lex, lin, col);
        }

        // Tem ponto: precisa ter dígitos nos dois lados
        if (temDigitosAntes && temDigitosDepois) {
            return new Token(TokenTipo.NUM_REAL, lex, lin, col);
        }

        // Casos inválidos: ".5" ou "5."
        addDiagLex(LEX_REAL_INVALIDO, "número real inválido", lex);
        // Ainda assim devolvemos NUM_REAL para o parser progredir
        return new Token(TokenTipo.NUM_REAL, lex, lin, col);
    }

    // ------------------- Utilidades -------------------
    private void consumirEspacos() {
        while (!fim()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
            } else if (c == '\n') {
                advanceLinha();
            } else {
                break;
            }
        }
    }

    private Token tok(TokenTipo tipo, String lexema) {
        return new Token(tipo, lexema, linha, coluna);
    }

    private void addDiagLex(int codigo, String msg, String lexema) {
        addDiag(Tipo.LEXICO, codigo, msg, linha, coluna, lexema);
    }

    private void addDiag(Tipo t, int codigo, String msg, int lin, int col, String lex) {
        diagnosticos.add(new Diagnostico(t, codigo, msg, lin, col, lex));
    }

    private boolean match(char esperado) {
        if (fim() || peek() != esperado) return false;
        advance();
        return true;
    }

    private boolean fim() { return i >= n; }

    private char peek() { return fonte.charAt(i); }

    private char peekPrev() { return (i == 0) ? '\0' : fonte.charAt(i - 1); }

    private void advance() {
        char c = fonte.charAt(i++);
        if (c == '\n') {
            linha++;
            coluna = 1;
        } else {
            coluna++;
        }
    }

    private void advanceLinha() {
        i++;
        linha++;
        coluna = 1;
    }

    private static boolean isLetra(char c) {
        return (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigito(char c) {
        return (c >= '0' && c <= '9');
    }

    // Mapa de palavras-chave
    private static final Map<String, TokenTipo> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("inteiro",  TokenTipo.KW_INTEIRO);
        KEYWORDS.put("real",     TokenTipo.KW_REAL);
        KEYWORDS.put("caracter", TokenTipo.KW_CARACTER);

        KEYWORDS.put("se",       TokenTipo.KW_SE);
        KEYWORDS.put("entao",    TokenTipo.KW_ENTAO);
        KEYWORDS.put("senao",    TokenTipo.KW_SENAO);
        KEYWORDS.put("enquanto", TokenTipo.KW_ENQUANTO);

        KEYWORDS.put("E",        TokenTipo.KW_E);
        KEYWORDS.put("OU",       TokenTipo.KW_OU);
        KEYWORDS.put("NAO",      TokenTipo.KW_NAO);
    }
}
