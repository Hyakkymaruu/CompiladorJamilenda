package mlp.tac;

import java.util.ArrayList;
import java.util.List;

import mlp.ast.AstNode;
import mlp.Lexico.Token;
import mlp.Lexico.TokenTipo;

/**
 * Gera código intermediário (TAC) a partir da AST.
 * - Atualmente cobre:
 *   - Atribuições simples e com expressões (+, *, /, RESTO)
 *   - Comando 'se' (sem 'senao' por enquanto)
 *   - Comando 'enquanto'
 */
public class GeradorTAC {

    private final List<TacInstr> codigo = new ArrayList<>();
    private int proxTemp = 0;
    private int proxRotulo = 0;

    public List<TacInstr> gerar(AstNode programa) {
        codigo.clear();
        proxTemp = 0;
        proxRotulo = 0;

        if (programa == null) return codigo;

        for (AstNode filho : programa.getFilhos()) {
            gerarComandoNoTopo(filho);
        }

        return codigo;
    }

    // ---------- geração de comandos de topo ----------

    private void gerarComandoNoTopo(AstNode n) {
        if (n == null) return;
        switch (n.getKind()) {
            case "CmdAtrib"    -> gerarCmdAtrib(n);
            case "CmdSe"       -> gerarCmdSe(n);
            case "CmdEnquanto" -> gerarCmdEnquanto(n);
            // "Decl" e outros nós são ignorados no TAC
            default -> { /* ignora */ }
        }
    }

    private void gerarComando(AstNode n) {
        if (n == null) return;
        switch (n.getKind()) {
            case "CmdAtrib"    -> gerarCmdAtrib(n);
            case "CmdSe"       -> gerarCmdSe(n);
            case "CmdEnquanto" -> gerarCmdEnquanto(n);
            default -> { /* ignora outros nós */ }
        }
    }

    // ---------- auxiliares de nomes ----------

    private String novoTemp() {
        return "t" + (proxTemp++);
    }

    private String novoRotulo() {
        return "L" + (proxRotulo++);
    }

    // ---------- comandos ----------

    /** CmdAtrib -> LValue expr */
    private void gerarCmdAtrib(AstNode cmd) {
        if (cmd.getFilhos().size() < 2) return;

        AstNode lvalue = cmd.getFilhos().get(0);
        AstNode expr   = cmd.getFilhos().get(1);

        // LValue -> Ident
        String nomeVar;
        if (lvalue.getFilhos().isEmpty()) {
            Token tk = lvalue.getToken();
            nomeVar = (tk != null ? tk.getLexema() : "<tmp>");
        } else {
            Token tk = lvalue.getFilhos().get(0).getToken();
            nomeVar = (tk != null ? tk.getLexema() : "<tmp>");
        }

        String resultado = gerarExpr(expr);
        codigo.add(TacInstr.store(nomeVar, resultado));
    }

    /** CmdSe -> 'se' cond 'entao' comando  (AST: CmdSe, filho[0]=cond, filho[1]=Then) */
    private void gerarCmdSe(AstNode cmdSe) {
        if (cmdSe.getFilhos().isEmpty()) return;

        AstNode cond = cmdSe.getFilhos().get(0);
        String tCond = gerarCondicao(cond);

        String rotuloFim = novoRotulo();
        codigo.add(TacInstr.jmpFalse(tCond, rotuloFim));

        // Then
        if (cmdSe.getFilhos().size() > 1) {
            AstNode thenBlk = cmdSe.getFilhos().get(1);
            for (AstNode cmd : thenBlk.getFilhos()) {
                gerarComando(cmd);
            }
        }

        codigo.add(TacInstr.label(rotuloFim));
    }

    /** CmdEnquanto -> 'enquanto' cond comando (AST: filho[0]=cond, filho[1]=Body) */
    private void gerarCmdEnquanto(AstNode cmdEnquanto) {
        if (cmdEnquanto.getFilhos().isEmpty()) return;

        String rotuloInicio = novoRotulo();
        String rotuloFim    = novoRotulo();

        codigo.add(TacInstr.label(rotuloInicio));

        AstNode cond = cmdEnquanto.getFilhos().get(0);
        String tCond = gerarCondicao(cond);

        codigo.add(TacInstr.jmpFalse(tCond, rotuloFim));

        // Corpo do laço
        if (cmdEnquanto.getFilhos().size() > 1) {
            AstNode body = cmdEnquanto.getFilhos().get(1);
            for (AstNode cmd : body.getFilhos()) {
                gerarComando(cmd);
            }
        }

        codigo.add(TacInstr.jmp(rotuloInicio));
        codigo.add(TacInstr.label(rotuloFim));
    }

    // ---------- expressões ----------

    private String gerarExpr(AstNode e) {
        if (e == null) return novoTemp();

        String k = e.getKind();
        switch (k) {
            case "Numero" -> {
                Token tk = e.getToken();
                String lex = (tk != null ? tk.getLexema() : "0");
                String t = novoTemp();
                codigo.add(TacInstr.loadi(t, lex));
                return t;
            }
            case "Ident" -> {
                Token tk = e.getToken();
                String nome = (tk != null ? tk.getLexema() : "<anon>");
                String t = novoTemp();
                codigo.add(TacInstr.load(t, nome));
                return t;
            }
            case "OpMais" -> {
                String a = gerarExpr(e.getFilhos().get(0));
                String b = gerarExpr(e.getFilhos().get(1));
                String t = novoTemp();
                codigo.add(TacInstr.add(t, a, b));
                return t;
            }
            case "OpMult" -> {
                String a = gerarExpr(e.getFilhos().get(0));
                String b = gerarExpr(e.getFilhos().get(1));
                String t = novoTemp();
                codigo.add(TacInstr.mul(t, a, b));
                return t;
            }
            case "OpDiv" -> {
                String a = gerarExpr(e.getFilhos().get(0));
                String b = gerarExpr(e.getFilhos().get(1));
                String t = novoTemp();
                codigo.add(TacInstr.div(t, a, b));
                return t;
            }
            case "OpResto" -> {
                String a = gerarExpr(e.getFilhos().get(0));
                String b = gerarExpr(e.getFilhos().get(1));
                String t = novoTemp();
                codigo.add(TacInstr.rem(t, a, b));
                return t;
            }
            default -> {
                // fallback: tenta usar primeiro filho como expressão
                if (!e.getFilhos().isEmpty()) {
                    return gerarExpr(e.getFilhos().get(0));
                }
                String t = novoTemp();
                codigo.add(TacInstr.nop());
                return t;
            }
        }
    }

    // ---------- condições ----------

    /** Gera código para condição (atualmente só Rel) e devolve o registrador com 0/1. */
    private String gerarCondicao(AstNode cond) {
        if (cond == null) {
            String t = novoTemp();
            codigo.add(TacInstr.loadi(t, "0"));
            return t;
        }

        if ("Rel".equals(cond.getKind())) {
            if (cond.getFilhos().size() < 2) {
                String t = novoTemp();
                codigo.add(TacInstr.loadi(t, "0"));
                return t;
            }

            AstNode left  = cond.getFilhos().get(0);
            AstNode right = cond.getFilhos().get(1);

            String tLeft  = gerarExpr(left);
            String tRight = gerarExpr(right);

            // Usamos o próprio tLeft como destino da comparação
            Token opTk = cond.getToken();
            TokenTipo tp = (opTk != null ? opTk.getTipo() : null);

            if (tp == TokenTipo.OP_LT) {
                codigo.add(TacInstr.cmplt(tLeft, tLeft, tRight));
            } else if (tp == TokenTipo.OP_GT) {
                codigo.add(TacInstr.cmpgt(tLeft, tLeft, tRight));
            } else if (tp == TokenTipo.OP_EQ) {
                codigo.add(TacInstr.cmpeq(tLeft, tLeft, tRight));
            } else {
                // fallback: trata como '<'
                codigo.add(TacInstr.cmplt(tLeft, tLeft, tRight));
            }
            return tLeft;
        }

        // fallback: condição numérica (!=0 é verdadeiro)
        return gerarExpr(cond);
    }
}
