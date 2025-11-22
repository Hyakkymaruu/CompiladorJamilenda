package mlp.Sintatico;

import java.util.ArrayList;
import java.util.List;

import mlp.Erros.Diagnostico;
import mlp.Erros.Diagnostico.Tipo;
import mlp.Lexico.AnalisadorLexico;
import mlp.Lexico.Token;
import mlp.Lexico.TokenTipo;
import mlp.ast.AstNode;

/**
 * Parser recursivo-descendente com recuperação de erros.
 * Mantém diagnóstico rico (não “para” no 1º erro).
 */
public class AnalisadorSintatico {

    private final AnalisadorLexico lx;
    private final List<Diagnostico> diagnosticos = new ArrayList<>();
    private Token atual;

    public AnalisadorSintatico(AnalisadorLexico lx) {
        this.lx = lx;
        this.atual = lx.proximo();
    }

    public List<Diagnostico> getDiagnosticos() {
        return diagnosticos;
    }

    // ---------- Núcleo utilitário ----------
    private boolean aceita(TokenTipo tipo) {
        if (atual.getTipo() == tipo) {
            atual = lx.proximo();
            return true;
        }
        return false;
    }

    private Token consome(TokenTipo tipo, int cod, String msg) {
        if (atual.getTipo() == tipo) {
            Token t = atual;
            atual = lx.proximo();
            return t;
        } else {
            emitir(cod, msg, atual);
            return null;
        }
    }

    private void emitir(int codigo, String msg, Token t) {
        diagnosticos.add(new Diagnostico(
                Tipo.SINTATICO, codigo, msg,
                t != null ? t.getLinha() : 0,
                t != null ? t.getColuna() : 0,
                t != null ? t.getLexema() : null
        ));
    }

    /** Sincroniza até o fim de comando: ';', 'se', 'enquanto', END, EOF. */
    private void syncAteFimComando() {
        while (true) {
            TokenTipo tp = atual.getTipo();
            if (tp == TokenTipo.PONTO_VIRG
                    || tp == TokenTipo.KW_SE
                    || tp == TokenTipo.KW_ENQUANTO
                    || tp == TokenTipo.END
                    || tp == TokenTipo.EOF) {
                return;
            }
            atual = lx.proximo();
        }
    }

    /**
     * Sincroniza especificamente após erro em declaração:
     * para em ';' ou em tokens que iniciam comando (IDENT, SE, ENQUANTO), ou em END/EOF.
     * Se encontrar ';', consome; se encontrar início de comando, NÃO consome
     * (deixa para o próximo passo).
     */
    private void syncDeclFimOuInicioComando() {
        while (true) {
            TokenTipo tp = atual.getTipo();
            if (tp == TokenTipo.PONTO_VIRG) {
                aceita(TokenTipo.PONTO_VIRG);
                return;
            }
            if (tp == TokenTipo.IDENT
             || tp == TokenTipo.KW_SE
             || tp == TokenTipo.KW_ENQUANTO
             || tp == TokenTipo.END
             || tp == TokenTipo.EOF) {
                return; // não consome; deixa o próximo passo tratar
            }
            atual = lx.proximo(); // come lixo
        }
    }

    /** Após corpo/expressão, exigir ';' e emitir 1017 se faltar (sem parar a análise). */
    private void exigirPontoVirgulaSePossivel() {
        if (!aceita(TokenTipo.PONTO_VIRG)) {
            emitir(1017, "esperava ';' ao final da atribuição", atual);
        }
    }

    // ---------- Entrada ----------
    public AstNode parsePrograma() {
        AstNode prog = new AstNode("Programa", tokenClone(atual));
        // START
        if (!aceita(TokenTipo.START)) {
            emitir(1002, "esperava início de programa '$'", atual);
        }

        // corpo
        while (atual.getTipo() != TokenTipo.END
            && atual.getTipo() != TokenTipo.EOF) {

            if (isInicioDecl()) {
                AstNode d = parseDecl();
                if (d != null) prog.addFilho(d);
            } else if (isInicioComando()) {
                AstNode c = parseComando();
                if (c != null) prog.addFilho(c);
            } else {
                // token inesperado no corpo do programa
                emitir(1003, "esperava fim de programa '$.'", atual);
                syncAteFimComando();
                if (aceita(TokenTipo.PONTO_VIRG)) {
                    // ok, consumiu um ';' perdido
                } else if (atual.getTipo() == TokenTipo.END || atual.getTipo() == TokenTipo.EOF) {
                    break;
                } else {
                    atual = lx.proximo();
                }
            }
        }

        // END
        if (!aceita(TokenTipo.END)) {
            emitir(1003, "esperava fim de programa '$.'", atual);
        }

        // EOF
        if (!aceita(TokenTipo.EOF)) {
            emitir(1003, "esperava fim de programa '$.'", atual);
            while (atual.getTipo() != TokenTipo.EOF) atual = lx.proximo();
            aceita(TokenTipo.EOF);
        }

        return prog;
    }

    private boolean isInicioDecl() {
        return atual.getTipo() == TokenTipo.KW_INTEIRO
            || atual.getTipo() == TokenTipo.KW_REAL
            || atual.getTipo() == TokenTipo.KW_CARACTER;
    }

    private boolean isInicioComando() {
        return atual.getTipo() == TokenTipo.IDENT
            || atual.getTipo() == TokenTipo.KW_SE
            || atual.getTipo() == TokenTipo.KW_ENQUANTO
            || atual.getTipo() == TokenTipo.KW_ESCREVA;
    }

    // ---------- Declarações ----------
    private AstNode parseDecl() {
        // Decl -> Tipo ListaIdent ';'
        Token tTipo = atual;
        AstNode decl = new AstNode("Decl", tTipo);
        AstNode tipoNo = parseTipo();
        if (tipoNo != null) decl.addFilho(tipoNo);

        AstNode lista = parseListaIdent();
        if (lista != null) decl.addFilho(lista);

        if (!aceita(TokenTipo.PONTO_VIRG)) {
            // faltou ';' na declaração
            emitir(1006, "esperava ';' ao final da declaração", atual);
            syncDeclFimOuInicioComando();
        }

        return decl;
    }

    private AstNode parseTipo() {
        Token t = atual;
        if (aceita(TokenTipo.KW_INTEIRO) ||
            aceita(TokenTipo.KW_REAL)    ||
            aceita(TokenTipo.KW_CARACTER)) {
            return new AstNode("Tipo", t);
        }
        return null;
    }

    private AstNode parseListaIdent() {
        AstNode lista = new AstNode("ListaIdent", atual);
        if (atual.getTipo() == TokenTipo.IDENT) {
            lista.addFilho(parseIdent());
            while (aceita(TokenTipo.VIRGULA)) {
                if (atual.getTipo() == TokenTipo.IDENT) {
                    lista.addFilho(parseIdent());
                } else {
                    emitir(1007, "esperava identificador após ','", atual);
                    break;
                }
            }
        } else {
            emitir(1007, "esperava identificador", atual);
        }
        return lista;
    }

    private AstNode parseIdent() {
        Token t = consome(TokenTipo.IDENT, 1007, "esperava identificador");
        return new AstNode("Ident", t);
    }

    // ---------- Comandos ----------
    private AstNode parseComando() {
        return switch (atual.getTipo()) {
            case IDENT -> parseAtrib();
            case KW_SE -> parseSe();
            case KW_ENQUANTO -> parseEnquanto();
            case KW_ESCREVA -> parseEscreva();
            default -> {
                emitir(1001, "comando inválido", atual);
                syncAteFimComando();
                aceita(TokenTipo.PONTO_VIRG);
                yield null;
            }
        };
    }

    private AstNode parseEscreva(){
        Token t = atual;
        aceita(TokenTipo.KW_ESCREVA);
        AstNode cmd = new AstNode("CmdEscreva", t);

        if (!aceita(TokenTipo.ABRE_PAR)) {
            emitir(1011, "escreva: esperava '('", atual);
        }

        AstNode expr = parseExpressaoOuFatorInvalido();
        cmd.addFilho(expr);

        if (!aceita(TokenTipo.FECHA_PAR)) {
            emitir(1012, "escreva: esperava ')'", atual);
        }
        if (!aceita(TokenTipo.PONTO_VIRG)) {
            emitir(1017, "escreva: esperava ';'", atual);
        }

        return cmd;
    }

    /** CmdAtrib -> IDENT '=' expressao ';' */
    private AstNode parseAtrib() {
        Token identTok = atual;
        AstNode cmd = new AstNode("CmdAtrib", identTok);

        // LValue
        AstNode lvalue = new AstNode("LValue", identTok);
        lvalue.addFilho(parseIdent());
        cmd.addFilho(lvalue);

        // '='
        if (!aceita(TokenTipo.OP_ATRIB)) {
            // Erro: faltou '='
            emitir(1015, "esperava '=' na atribuição", atual);
            emitir(1016, "fator inválido em expressão", atual);

            // Recuperação: consumir até o fim do comando
            syncAteFimComando();
            aceita(TokenTipo.PONTO_VIRG); // consome ';' se achar

            return cmd;
        }

        // expressão
        AstNode expr = parseExpressaoOuFatorInvalido();
        cmd.addFilho(expr);

        // Exigir ';' (com diagnóstico, sem parar a análise)
        exigirPontoVirgulaSePossivel();

        return cmd;
    }

    /** CmdSe -> 'se' '(' cond ')' 'entao' comando ['senao' comando] */
    private AstNode parseSe() {
        Token tSe = atual;
        aceita(TokenTipo.KW_SE);
        AstNode cmdSe = new AstNode("CmdSe", tSe);

        // '('
        consome(TokenTipo.ABRE_PAR, 1011, "esperava '(' após 'se'");

        // condição
        AstNode cond = parseCondOuRelInvalido();
        cmdSe.addFilho(cond);

        // ')' — sempre checar e diagnosticar (não parar)
        if (!aceita(TokenTipo.FECHA_PAR)) {
            emitir(1013, "esperava ')' após condição", atual);
            // pequena recuperação
            while (atual.getTipo() != TokenTipo.FECHA_PAR
                && atual.getTipo() != TokenTipo.KW_ENTAO
                && atual.getTipo() != TokenTipo.KW_SENAO
                && atual.getTipo() != TokenTipo.KW_SE
                && atual.getTipo() != TokenTipo.KW_ENQUANTO
                && atual.getTipo() != TokenTipo.END
                && atual.getTipo() != TokenTipo.EOF) {
                atual = lx.proximo();
            }
            aceita(TokenTipo.FECHA_PAR); // consome se encontrado
        }

        // 'entao' — sempre checar e diagnosticar (não parar)
        if (!aceita(TokenTipo.KW_ENTAO)) {
            emitir(1014, "esperava 'entao'", atual);
        }

        // Then (um comando)
        AstNode thenBlk = new AstNode("Then", tSe);
        if (isInicioComando()) {
            AstNode c = parseComando();
            if (c != null) thenBlk.addFilho(c);
        } else {
            emitir(1001, "comando inválido", atual);
        }
        cmdSe.addFilho(thenBlk);

        // Opcional: 'senao' comando
        if (atual.getTipo() == TokenTipo.KW_SENAO) {
            aceita(TokenTipo.KW_SENAO); // consome 'senao'
            AstNode elseBlk = new AstNode("Else", tSe);
            if (isInicioComando()) {
                AstNode cElse = parseComando();
                if (cElse != null) elseBlk.addFilho(cElse);
            } else {
                emitir(1001, "comando inválido após 'senao'", atual);
            }
            cmdSe.addFilho(elseBlk);
        }

        return cmdSe;
    }

    /** CmdEnquanto -> 'enquanto' '(' cond ')' comando */
    private AstNode parseEnquanto() {
        Token t = atual;
        aceita(TokenTipo.KW_ENQUANTO);
        AstNode cmd = new AstNode("CmdEnquanto", t);

        consome(TokenTipo.ABRE_PAR, 1011, "esperava '(' após 'enquanto'");

        AstNode cond = parseCondOuRelInvalido();
        cmd.addFilho(cond);

        if (!aceita(TokenTipo.FECHA_PAR)) {
            emitir(1013, "esperava ')' após condição", atual);
            while (atual.getTipo() != TokenTipo.FECHA_PAR
                && atual.getTipo() != TokenTipo.KW_SE
                && atual.getTipo() != TokenTipo.KW_ENQUANTO
                && atual.getTipo() != TokenTipo.END
                && atual.getTipo() != TokenTipo.EOF) {
                atual = lx.proximo();
            }
            aceita(TokenTipo.FECHA_PAR);
        }

        // corpo: um comando
        AstNode body = new AstNode("Body", t);
        if (isInicioComando()) {
            AstNode c = parseComando();
            if (c != null) body.addFilho(c);
        } else {
            emitir(1001, "comando inválido", atual);
        }
        cmd.addFilho(body);
        return cmd;
    }

    // ---------- Expressões e condições (resumo) ----------
    private AstNode parseExpressaoOuFatorInvalido() {
        AstNode left = parseExprSoma();
        if (left != null) return left;

        AstNode inv = new AstNode("FatorInvalido", atual);
        if (atual.getTipo() != TokenTipo.PONTO_VIRG
         && atual.getTipo() != TokenTipo.END
         && atual.getTipo() != TokenTipo.EOF) {
            atual = lx.proximo(); // consome 1 para avançar
        }
        return inv;
    }

    /**
     * Wrapper usado por 'se' e 'enquanto':
     * tenta parsear uma condição completa (NAO/E/OU/Rel).
     * Se falhar, devolve um Rel inválido com FatorInvalido.
     */
    private AstNode parseCondOuRelInvalido() {
        AstNode cond = parseCond();
        if (cond != null) {
            return cond;
        }

        // fallback: Rel inválido
        Token start = atual;
        AstNode relInv = new AstNode(
            "Rel",
            new Token(TokenTipo.INVALIDO, "<invalido>", start.getLinha(), start.getColuna())
        );
        relInv.addFilho(new AstNode("FatorInvalido", atual));
        return relInv;
    }

    // ---------- Condições lógicas ----------

    // cond -> cond_ou
    private AstNode parseCond() {
        return parseCondOu();
    }

    // cond_ou -> cond_e { 'OU' cond_e }
    private AstNode parseCondOu() {
        AstNode left = parseCondE();
        if (left == null) return null;

        while (atual.getTipo() == TokenTipo.KW_OU) {
            Token t = atual;
            aceita(TokenTipo.KW_OU);
            AstNode n = new AstNode("OpOU", t);
            n.addFilho(left);

            AstNode right = parseCondE();
            if (right == null) {
                n.addFilho(new AstNode("FatorInvalido", atual));
                return n;
            }
            n.addFilho(right);
            left = n;
        }
        return left;
    }

    // cond_e -> cond_nao { 'E' cond_nao }
    private AstNode parseCondE() {
        AstNode left = parseCondNao();
        if (left == null) return null;

        while (atual.getTipo() == TokenTipo.KW_E) {
            Token t = atual;
            aceita(TokenTipo.KW_E);
            AstNode n = new AstNode("OpE", t);
            n.addFilho(left);

            AstNode right = parseCondNao();
            if (right == null) {
                n.addFilho(new AstNode("FatorInvalido", atual));
                return n;
            }
            n.addFilho(right);
            left = n;
        }
        return left;
    }

    // cond_nao -> 'NAO' cond_nao | '(' cond ')' | rel
    private AstNode parseCondNao() {
        // NAO <cond_nao>
        if (atual.getTipo() == TokenTipo.KW_NAO) {
            Token t = atual;
            aceita(TokenTipo.KW_NAO);
            AstNode n = new AstNode("Nao", t);

            AstNode inner = parseCondNao();
            if (inner == null) {
                n.addFilho(new AstNode("FatorInvalido", atual));
                return n;
            }
            n.addFilho(inner);
            return n;
        }

        // '(' cond ')'  -> parênteses em volta de condição lógica
        if (atual.getTipo() == TokenTipo.ABRE_PAR) {
            aceita(TokenTipo.ABRE_PAR);
            AstNode inner = parseCond();
            consome(TokenTipo.FECHA_PAR, 1012, "esperava ')' após expressão");
            return inner;
        }

        // rel
        return parseRel();
    }

    // rel -> opndRel opRel opndRel   (com tratamento de erro)
    private AstNode parseRel() {

        AstNode left = parseOpndRel();
        if (left == null) {
            // não conseguimos nem ler o primeiro operando
            return null;
        }

        // Operador relacional
        Token relop = atual;
        if (!isRelop(relop.getTipo())) {
            emitir(1010, "esperava operador relacional", atual);
            AstNode relInv = new AstNode(
                "Rel",
                new Token(TokenTipo.INVALIDO, "<invalido>", relop.getLinha(), relop.getColuna())
            );
            relInv.addFilho(left);
            relInv.addFilho(new AstNode("FatorInvalido", atual));
            return relInv;
        }
        aceita(relop.getTipo());

        AstNode right = parseOpndRel();
        if (right == null) {
            AstNode relInv = new AstNode(
                "Rel",
                new Token(TokenTipo.INVALIDO, "<invalido>", relop.getLinha(), relop.getColuna())
            );
            relInv.addFilho(left);
            relInv.addFilho(new AstNode("FatorInvalido", atual));
            return relInv;
        }

        AstNode rel = new AstNode("Rel", relop);
        rel.addFilho(left);
        rel.addFilho(right);
        return rel;
    }

    private boolean isRelop(TokenTipo tp) {
        return tp == TokenTipo.OP_EQ
            || tp == TokenTipo.OP_NE
            || tp == TokenTipo.OP_LT
            || tp == TokenTipo.OP_LE
            || tp == TokenTipo.OP_GT
            || tp == TokenTipo.OP_GE;
    }

    private AstNode parseOpndRel() {
        if (atual.getTipo() == TokenTipo.IDENT) {
            return parseIdent();
        }
        if (atual.getTipo() == TokenTipo.NUM_INT || atual.getTipo() == TokenTipo.NUM_REAL) {
            Token t = atual; aceita(t.getTipo());
            AstNode n = new AstNode("Numero", t);
            return n;
        }
        if (aceita(TokenTipo.ABRE_PAR)) {
            AstNode e = parseExpressaoOuFatorInvalido();
            consome(TokenTipo.FECHA_PAR, 1012, "esperava ')' após expressão");
            return e;
        }
        return null;
    }

    private AstNode parseExprSoma() {
        AstNode left = parseExprMul();
        if (left == null) return null;

        while (atual.getTipo() == TokenTipo.OP_MAIS
            || atual.getTipo() == TokenTipo.OP_MENOS) {

            Token t = atual;

            if (atual.getTipo() == TokenTipo.OP_MAIS) {
                aceita(TokenTipo.OP_MAIS);
            } else {
                aceita(TokenTipo.OP_MENOS);
            }

            // Decide o nome do nó pelo operador
            String nomeOp = (t.getTipo() == TokenTipo.OP_MAIS)
                    ? "OpMais"
                    : "OpMenos";

            AstNode bin = new AstNode(nomeOp, t);
            bin.addFilho(left);

            AstNode right = parseExprMul();
            if (right == null) {
                bin.addFilho(new AstNode("FatorInvalido", atual));
                return bin;
            }

            bin.addFilho(right);
            left = bin;
        }

        return left;
    }


    private AstNode parseExprMul() {
        AstNode left = parseFator();
        if (left == null) return null;

        while (atual.getTipo() == TokenTipo.OP_MULT
            || atual.getTipo() == TokenTipo.OP_DIV
            || atual.getTipo() == TokenTipo.OP_RESTO) {

            Token t = atual;
            if (aceita(TokenTipo.OP_MULT)) {
                AstNode bin = new AstNode("OpMult", t);
                bin.addFilho(left);
                AstNode r = parseFator();
                if (r == null) { bin.addFilho(new AstNode("FatorInvalido", atual)); return bin; }
                bin.addFilho(r);
                left = bin;
            } else if (aceita(TokenTipo.OP_DIV)) {
                AstNode bin = new AstNode("OpDiv", t);
                bin.addFilho(left);
                AstNode r = parseFator();
                if (r == null) { bin.addFilho(new AstNode("FatorInvalido", atual)); return bin; }
                bin.addFilho(r);
                left = bin;
            } else {
                aceita(TokenTipo.OP_RESTO);
                AstNode bin = new AstNode("OpResto", t);
                bin.addFilho(left);
                AstNode r = parseFator();
                if (r == null) { bin.addFilho(new AstNode("FatorInvalido", atual)); return bin; }
                bin.addFilho(r);
                left = bin;
            }
        }
        return left;
    }

    private AstNode parseFator() {
        if (atual.getTipo() == TokenTipo.IDENT) return parseIdent();
        if (atual.getTipo() == TokenTipo.NUM_INT || atual.getTipo() == TokenTipo.NUM_REAL) {
            Token t = atual; aceita(t.getTipo());
            return new AstNode("Numero", t);
        }
        if (aceita(TokenTipo.ABRE_PAR)) {
            AstNode e = parseExpressaoOuFatorInvalido();
            consome(TokenTipo.FECHA_PAR, 1012, "esperava ')' após expressão");
            return e;
        }
        return null;
    }

    // ---------- helper ----------
    private static Token tokenClone(Token t) {
        return t;
    }
}
