package mlp.Sintatico;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import mlp.Erros.Diagnostico;
import mlp.Erros.Diagnostico.Tipo;
import mlp.Lexico.AnalisadorLexico;
import mlp.Lexico.Token;
import mlp.Lexico.TokenTipo;
import mlp.ast.AstNode;

/** Parser LL(1) por descida recursiva para a MLP. */
public class AnalisadorSintatico {

    // ----------------- Debug -----------------
    private boolean DEBUG = false; // mude para true se quiser logar
    public void setDebug(boolean value) { this.DEBUG = value; }
    private void dbg(String msg) { if (DEBUG) System.out.println("[DBG] " + msg); }
    private static String tokStr(Token t) {
        if (t == null) return "<null>";
        return t.getTipo() + "('" + t.getLexema() + "')@" + t.getLinha() + ":" + t.getColuna();
    }

    // ----------------- Códigos locais (evita dependência de Diagnostico.*) -----------------
    private static final int SIN_COMANDO_INVALIDO   = 1001;
    private static final int SIN_ESPERAVA_START     = 1002;
    private static final int SIN_ESPERAVA_END       = 1003;
    private static final int SIN_ESPERAVA_TIPO      = 1004;
    private static final int SIN_ESPERAVA_IDENT     = 1005;
    private static final int SIN_ESPERAVA_PV_DECL   = 1006;
    private static final int SIN_ESPERAVA_IDENT_APV = 1007; // ident após vírgula
    private static final int SIN_ESPERAVA_OP_REL    = 1010;
    private static final int SIN_ESPERAVA_FP_EXP    = 1012; // fecha parêntese após expressão
    private static final int SIN_ESPERAVA_FP_COND   = 1013; // fecha parêntese após condição
    private static final int SIN_ESPERAVA_ENTAO     = 1014;
    private static final int SIN_ESPERAVA_ATR       = 1015; // '='
    private static final int SIN_FATOR_INVALIDO     = 1016;
    private static final int SIN_ESPERAVA_PV_ATR    = 1017;

    // ----------------- Estado -----------------
    private final AnalisadorLexico lx;
    private Token atual;
    private Token ultimo; // último token consumido

    private final List<Diagnostico> diagnosticos = new ArrayList<>();

    public AnalisadorSintatico(AnalisadorLexico lx) {
        this.lx = lx;
        this.atual = lx.proximo(); // priming
        dbg("init lookahead=" + tokStr(atual));
    }

    public List<Diagnostico> getDiagnosticos() { return diagnosticos; }

    // ----------------- Entrada pública -----------------
    /** programa = START { declar } { comando } END */
    public AstNode parsePrograma() {
        AstNode prog = new AstNode("Programa");

        expect(TokenTipo.START, SIN_ESPERAVA_START, "esperava início de programa '$'");

        // { declar }
        while (isTipo(atual.getTipo())) {
            prog.add(parseDeclar());
        }

        // { comando }
        while (FIRST_COMANDO.contains(atual.getTipo())) {
            prog.add(parseComando());
        }

        expect(TokenTipo.END, SIN_ESPERAVA_END, "esperava fim de programa '$.'");
        return prog;
    }

    // ----------------- Produções -----------------

    /** declar = tipo listaIdent ';' */
    private AstNode parseDeclar() {
        AstNode decl = new AstNode("Decl");

        // tipo
        TokenTipo t = atual.getTipo();
        if (isTipo(t)) {
            decl.add(new AstNode("Tipo", atual));
            advance();
        } else {
            error(SIN_ESPERAVA_TIPO, "esperava tipo ('inteiro', 'real' ou 'caracter')");
            sincronizarAte(PONTO_VIRG_SET); // tenta chegar até ';'
        }

        // listaIdent = IDENT { ',' IDENT }
        if (match(TokenTipo.IDENT)) {
            AstNode lista = new AstNode("ListaIdent").add(new AstNode("Ident", last()));
            while (match(TokenTipo.VIRGULA)) {
                if (match(TokenTipo.IDENT)) {
                    lista.add(new AstNode("Ident", last()));
                } else {
                    error(SIN_ESPERAVA_IDENT_APV, "esperava identificador após ','");
                    break;
                }
            }
            decl.add(lista);
        } else {
            error(SIN_ESPERAVA_IDENT, "esperava identificador na declaração");
        }

        expect(TokenTipo.PONTO_VIRG, SIN_ESPERAVA_PV_DECL, "esperava ';' ao final da declaração");
        return decl;
    }

    /** comando = cmdAtr | cmdSe | cmdEnquanto */
    private AstNode parseComando() {
        dbg("parseComando lookahead=" + tokStr(atual));
        switch (atual.getTipo()) {
            case IDENT:
                dbg("-> escolha: CmdAtrib");
                return parseCmdAtr();
            case KW_SE:
                dbg("-> escolha: CmdSe");
                return parseCmdSe();
            case KW_ENQUANTO:
                dbg("-> escolha: CmdEnquanto");
                return parseCmdEnquanto();
            default:
                error(SIN_COMANDO_INVALIDO, "comando inválido");
                // recuperação: consome até ';' ou início de próximo comando/END
                sincronizarAte(SYNC_COMANDO);
                if (atual.getTipo() == TokenTipo.PONTO_VIRG) advance();
                return new AstNode("ComandoInvalido");
        }
    }

    /** cmdAtr = IDENT '=' expressao ';' */
    private AstNode parseCmdAtr() {
        AstNode n = new AstNode("CmdAtrib");
        Token id = expectR(TokenTipo.IDENT, SIN_ESPERAVA_IDENT, "esperava identificador na atribuição");
        n.add(new AstNode("LValue", id));

        expect(TokenTipo.OP_ATRIB, SIN_ESPERAVA_ATR, "esperava '=' na atribuição");
        n.add(parseExpressao());

        expect(TokenTipo.PONTO_VIRG, SIN_ESPERAVA_PV_ATR, "esperava ';' ao final da atribuição");
        return n;
    }

    /** cmdSe = 'se' '(' condicao ')' 'entao' comando ['senao' comando] */
    private AstNode parseCmdSe() {
        AstNode n = new AstNode("CmdSe");

        expect(TokenTipo.KW_SE, SIN_COMANDO_INVALIDO, "esperava 'se'");
        expect(TokenTipo.ABRE_PAR, SIN_ESPERAVA_FP_COND, "esperava '(' após 'se'");
        n.add(parseCondicao());
        expect(TokenTipo.FECHA_PAR, SIN_ESPERAVA_FP_COND, "esperava ')' após condição");
        expect(TokenTipo.KW_ENTAO, SIN_ESPERAVA_ENTAO, "esperava 'entao'");

        n.add(new AstNode("Then").add(parseComando()));

        if (match(TokenTipo.KW_SENAO)) {
            n.add(new AstNode("Else").add(parseComando()));
        }
        return n; // sem ';' no final
    }

    /** cmdEnquanto = 'enquanto' '(' condicao ')' comando */
    private AstNode parseCmdEnquanto() {
        AstNode n = new AstNode("CmdEnquanto");

        expect(TokenTipo.KW_ENQUANTO, SIN_COMANDO_INVALIDO, "esperava 'enquanto'");
        expect(TokenTipo.ABRE_PAR, SIN_ESPERAVA_FP_COND, "esperava '(' após 'enquanto'");
        n.add(parseCondicao());
        expect(TokenTipo.FECHA_PAR, SIN_ESPERAVA_FP_COND, "esperava ')' após condição");
        n.add(new AstNode("Body").add(parseComando()));
        return n; // sem ';' no final
    }

    // --------- Expressões numéricas ----------
    /** expressao = termo { '+' termo } */
    private AstNode parseExpressao() {
        AstNode left = parseTermo();
        while (match(TokenTipo.OP_MAIS)) {
            Token opTok = last();
            AstNode op = new AstNode("OpMais", opTok);
            op.add(left);
            op.add(parseTermo());
            left = op;
        }
        return left;
    }

    /** termo = fator { ('*' | '/' | 'RESTO') fator } */
    private AstNode parseTermo() {
        AstNode left = parseFator();
        while (atual.getTipo() == TokenTipo.OP_MULT
            || atual.getTipo() == TokenTipo.OP_DIV
            || atual.getTipo() == TokenTipo.OP_RESTO) {

            Token opTok = atual; advance();
            String kind = switch (opTok.getTipo()) {
                case OP_MULT -> "OpMult";
                case OP_DIV  -> "OpDiv";
                case OP_RESTO-> "OpResto";
                default      -> "Op?";
            };
            AstNode op = new AstNode(kind, opTok);
            op.add(left);
            op.add(parseFator());
            left = op;
        }
        return left;
    }

    /** fator = IDENT | NUM_INT | NUM_REAL | '(' expressao ')' */
    private AstNode parseFator() {
        switch (atual.getTipo()) {
            case IDENT: {
                Token id = atual; advance();
                return new AstNode("Ident", id);
            }
            case NUM_INT:
            case NUM_REAL: {
                Token num = atual; advance();
                return new AstNode("Numero", num);
            }
            case ABRE_PAR: {
                advance();
                AstNode e = parseExpressao();
                expect(TokenTipo.FECHA_PAR, SIN_ESPERAVA_FP_EXP, "esperava ')' após expressão");
                return e;
            }
            default: {
                error(SIN_FATOR_INVALIDO, "fator inválido em expressão");
                Token err = atual; advance();
                return new AstNode("FatorInvalido", err);
            }
        }
    }

    // --------- Condições lógicas/relacionais ----------
    /** condicao = disjuncao */
    private AstNode parseCondicao() {
        return parseDisjuncao();
    }

    /** disjuncao = conjuncao { 'OU' conjuncao } */
    private AstNode parseDisjuncao() {
        AstNode left = parseConjuncao();
        while (match(TokenTipo.KW_OU)) {
            Token opTok = last();
            AstNode op = new AstNode("OpOU", opTok);
            op.add(left);
            op.add(parseConjuncao());
            left = op;
        }
        return left;
    }

    /** conjuncao = negacao { 'E' negacao } */
    private AstNode parseConjuncao() {
        AstNode left = parseNegacao();
        while (match(TokenTipo.KW_E)) {
            Token opTok = last();
            AstNode op = new AstNode("OpE", opTok);
            op.add(left);
            op.add(parseNegacao());
            left = op;
        }
        return left;
    }

    /** negacao = { 'NAO' } relacao  */
    private AstNode parseNegacao() {
        int count = 0;
        List<Token> naos = new ArrayList<>();
        while (match(TokenTipo.KW_NAO)) {
            naos.add(last());
            count++;
        }
        if (count > 0) dbg("parseNegacao: " + count + "x 'NAO' consumidos");

        AstNode base = parseRelacao(); // sem '( condicao )' aqui

        // empilha NAO como nós unários (associando à direita)
        for (int k = 0; k < count; k++) {
            AstNode nao = new AstNode("Nao", naos.get(count - 1 - k));
            nao.add(base);
            base = nao;
        }
        return base;
    }

    /** relacao = expressao opRel expressao */
    private AstNode parseRelacao() {
        dbg("parseRelacao lookahead=" + tokStr(atual));
        AstNode a = parseExpressao();
        dbg("parseRelacao: após expr-esq, lookahead=" + tokStr(atual));

        Token op = expectOneOf(REL_OPS, SIN_ESPERAVA_OP_REL, "esperava operador relacional");
        if (op.getTipo() != TokenTipo.INVALIDO) {
            dbg("parseRelacao: operador=" + op.getTipo());
        }

        AstNode b = parseExpressao();
        dbg("parseRelacao: após expr-dir, lookahead=" + tokStr(atual));

        AstNode rel = new AstNode("Rel", op);
        rel.add(a).add(b);
        return rel;
    }

    // ----------------- Utilitários de parsing -----------------

    private void advance() {
        ultimo = atual;
        atual = lx.proximo();
        dbg("advance: agora lookahead=" + tokStr(atual) + " (consumido " + tokStr(ultimo) + ")");
    }

    private Token last() { return ultimo; }

    private boolean match(TokenTipo tipo) {
        if (atual.getTipo() == tipo) {
            advance();
            return true;
        }
        return false;
    }

    private Token expect(TokenTipo tipo, int codigo, String msg) {
        if (atual.getTipo() == tipo) {
            Token t = atual;
            advance();
            return t;
        }
        error(codigo, msg);
        dbg("expect falhou: esperava " + tipo + " mas viu " + tokStr(atual) + " -> inserindo token virtual");
        return new Token(tipo, "<inserido>", atual.getLinha(), atual.getColuna());
    }

    private Token expectR(TokenTipo tipo, int codigo, String msg) {
        if (atual.getTipo() == tipo) {
            Token t = atual;
            advance();
            return t;
        }
        error(codigo, msg);
        dbg("expectR falhou: esperava " + tipo + " mas viu " + tokStr(atual) + " -> substituindo e consumindo 1");
        Token t = new Token(tipo, "<recuperado>", atual.getLinha(), atual.getColuna());
        advance();
        return t;
    }

    private Token expectOneOf(EnumSet<TokenTipo> set, int codigo, String msg) {
        if (set.contains(atual.getTipo())) {
            Token t = atual; advance(); return t;
        }
        error(codigo, msg);
        dbg("expectOneOf falhou: esperava um de " + set + " mas viu " + tokStr(atual));
        return new Token(TokenTipo.INVALIDO, "<invalido>", atual.getLinha(), atual.getColuna());
    }

    private void error(int codigo, String mensagem) {
        diagnosticos.add(new Diagnostico(
            Tipo.SINTATICO, codigo, mensagem, atual.getLinha(), atual.getColuna(), atual.getLexema()
        ));
    }

    private void sincronizarAte(EnumSet<TokenTipo> conjunto) {
        while (!conjunto.contains(atual.getTipo()) && atual.getTipo() != TokenTipo.EOF) {
            advance();
        }
    }

    // ----------------- Conjuntos auxiliares -----------------

    private static final EnumSet<TokenTipo> REL_OPS = EnumSet.of(
        TokenTipo.OP_EQ, TokenTipo.OP_NE, TokenTipo.OP_LE, TokenTipo.OP_GE, TokenTipo.OP_LT, TokenTipo.OP_GT
    );

    private static final EnumSet<TokenTipo> FIRST_COMANDO = EnumSet.of(
        TokenTipo.IDENT, TokenTipo.KW_SE, TokenTipo.KW_ENQUANTO
    );

    private static final EnumSet<TokenTipo> PONTO_VIRG_SET = EnumSet.of(TokenTipo.PONTO_VIRG);

    private static final EnumSet<TokenTipo> SYNC_COMANDO = EnumSet.of(
        TokenTipo.PONTO_VIRG, TokenTipo.IDENT, TokenTipo.KW_SE, TokenTipo.KW_ENQUANTO, TokenTipo.END
    );

    private static boolean isTipo(TokenTipo t) {
        return t == TokenTipo.KW_INTEIRO || t == TokenTipo.KW_REAL || t == TokenTipo.KW_CARACTER;
    }
}
