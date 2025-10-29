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
    /** Ative para ver decisões do parser no console. */
    private boolean DEBUG = true; // mude para true se quiser logar
    public void setDebug(boolean value) { this.DEBUG = value; }
    private void dbg(String msg) { if (DEBUG) System.out.println("[DBG] " + msg); }
    private static String tokStr(Token t) {
        if (t == null) return "<null>";
        return t.getTipo() + "('" + t.getLexema() + "')@" + t.getLinha() + ":" + t.getColuna();
    }

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

        expect(TokenTipo.START, "esperava início de programa '$'");

        // { declar }
        while (isTipo(atual.getTipo())) {
            prog.add(parseDeclar());
        }

        // { comando }
        while (FIRST_COMANDO.contains(atual.getTipo())) {
            prog.add(parseComando());
        }

        expect(TokenTipo.END, "esperava fim de programa '$.'");
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
            error("esperava tipo ('inteiro', 'real' ou 'caracter')");
            sincronizarAte(PONTO_VIRG_SET); // tenta chegar até ';'
        }

        // listaIdent = IDENT { ',' IDENT }
        if (match(TokenTipo.IDENT)) {
            AstNode lista = new AstNode("ListaIdent").add(new AstNode("Ident", last()));
            while (match(TokenTipo.VIRGULA)) {
                if (match(TokenTipo.IDENT)) {
                    lista.add(new AstNode("Ident", last()));
                } else {
                    error("esperava identificador após ','");
                    break;
                }
            }
            decl.add(lista);
        } else {
            error("esperava identificador na declaração");
        }

        expect(TokenTipo.PONTO_VIRG, "esperava ';' ao final da declaração");
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
                error("comando inválido");
                // recuperação: consome até ';' ou início de próximo comando/END
                sincronizarAte(SYNC_COMANDO);
                if (atual.getTipo() == TokenTipo.PONTO_VIRG) advance();
                return new AstNode("ComandoInvalido");
        }
    }

    /** cmdAtr = IDENT '=' expressao ';' */
    private AstNode parseCmdAtr() {
        AstNode n = new AstNode("CmdAtrib");
        Token id = expectR(TokenTipo.IDENT, "esperava identificador na atribuição");
        n.add(new AstNode("LValue", id));

        expect(TokenTipo.OP_ATRIB, "esperava '=' na atribuição");
        n.add(parseExpressao());

        expect(TokenTipo.PONTO_VIRG, "esperava ';' ao final da atribuição");
        return n;
    }

    /** cmdSe = 'se' '(' condicao ')' 'entao' comando ['senao' comando] */
    private AstNode parseCmdSe() {
        AstNode n = new AstNode("CmdSe");

        expect(TokenTipo.KW_SE, "esperava 'se'");
        expect(TokenTipo.ABRE_PAR, "esperava '(' após 'se'");
        n.add(parseCondicao());
        expect(TokenTipo.FECHA_PAR, "esperava ')' após condição");
        expect(TokenTipo.KW_ENTAO, "esperava 'entao'");

        n.add(new AstNode("Then").add(parseComando()));

        if (match(TokenTipo.KW_SENAO)) {
            n.add(new AstNode("Else").add(parseComando()));
        }
        return n; // sem ';' no final
    }

    /** cmdEnquanto = 'enquanto' '(' condicao ')' comando */
    private AstNode parseCmdEnquanto() {
        AstNode n = new AstNode("CmdEnquanto");

        expect(TokenTipo.KW_ENQUANTO, "esperava 'enquanto'");
        expect(TokenTipo.ABRE_PAR, "esperava '(' após 'enquanto'");
        n.add(parseCondicao());
        expect(TokenTipo.FECHA_PAR, "esperava ')' após condição");
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
                expect(TokenTipo.FECHA_PAR, "esperava ')' após expressão");
                return e;
            }
            default: {
                error("fator inválido em expressão");
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

        Token op = expectOneOf(REL_OPS, "esperava operador relacional");
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

    private Token expect(TokenTipo tipo, String msg) {
        if (atual.getTipo() == tipo) {
            Token t = atual;
            advance();
            return t;
        }
        error(msg);
        dbg("expect falhou: esperava " + tipo + " mas viu " + tokStr(atual) + " -> inserindo token virtual");
        return new Token(tipo, "<inserido>", atual.getLinha(), atual.getColuna());
    }

    private Token expectR(TokenTipo tipo, String msg) {
        if (atual.getTipo() == tipo) {
            Token t = atual;
            advance();
            return t;
        }
        error(msg);
        dbg("expectR falhou: esperava " + tipo + " mas viu " + tokStr(atual) + " -> substituindo e consumindo 1");
        Token t = new Token(tipo, "<recuperado>", atual.getLinha(), atual.getColuna());
        advance();
        return t;
    }

    private Token expectOneOf(EnumSet<TokenTipo> set, String msg) {
        if (set.contains(atual.getTipo())) {
            Token t = atual; advance(); return t;
        }
        error(msg);
        dbg("expectOneOf falhou: esperava um de " + set + " mas viu " + tokStr(atual));
        return new Token(TokenTipo.INVALIDO, "<invalido>", atual.getLinha(), atual.getColuna());
    }

    private void error(String mensagem) {
        diagnosticos.add(new Diagnostico(Tipo.SINTATICO, 10, mensagem,
                atual.getLinha(), atual.getColuna(), atual.getLexema()));
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
