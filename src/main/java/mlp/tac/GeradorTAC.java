package mlp.tac;

import java.util.ArrayList;
import java.util.List;

import mlp.ast.AstNode;
import mlp.Lexico.Token;
import mlp.Lexico.TokenTipo;

public class GeradorTAC {

    private final List<TacInstr> code = new ArrayList<>();
    private int tempCount = 0;
    private int labelCount = 0;

    private String newTemp()  { return "t" + (tempCount++); }
    private String newLabel() { return "L" + (labelCount++); }

    public List<TacInstr> gerar(AstNode programa) {
        code.clear();
        tempCount = 0;
        labelCount = 0;

        if (programa == null) return code;

        for (AstNode filho : programa.getFilhos()) {
            gerarComandoTopLevel(filho);
        }
        return code;
    }

    private void gerarComandoTopLevel(AstNode n) {
        switch (n.getKind()) {
            case "CmdAtrib"    -> genCmdAtrib(n);
            case "CmdSe"       -> genCmdSe(n);
            case "CmdEnquanto" -> genCmdEnquanto(n);
            // Decl etc: não geram código
            default -> {}
        }
    }

    // ---------------- Comandos ----------------

    private void genCmdAtrib(AstNode cmd) {
        if (cmd.getFilhos().size() < 2) return;

        AstNode lvalue = cmd.getFilhos().get(0);
        if (lvalue.getFilhos().isEmpty()) return;
        AstNode idNode = lvalue.getFilhos().get(0);
        Token idTk = idNode.getToken();
        if (idTk == null) return;

        String varName = idTk.getLexema();

        AstNode expr = cmd.getFilhos().get(1);
        String src = genExpr(expr);
        if (src == null) return;

        code.add(TacInstr.store(varName, src));
    }

    /** CmdSe com ou sem 'senao' */
    private void genCmdSe(AstNode cmdSe) {
        if (cmdSe.getFilhos().isEmpty()) return;

        AstNode condNode = cmdSe.getFilhos().get(0);
        String condTemp = genCond(condNode);
        if (condTemp == null) return;

        boolean hasElse = (cmdSe.getFilhos().size() > 2);
        String elseLabel = hasElse ? newLabel() : null;
        String endLabel  = newLabel();

        if (hasElse) {
            // if (cond) then ... else ...
            code.add(TacInstr.jmpFalse(condTemp, elseLabel));

            AstNode thenBlk = cmdSe.getFilhos().get(1);
            genBlocoComandos(thenBlk);

            code.add(TacInstr.jmp(endLabel));

            code.add(TacInstr.label(elseLabel));
            AstNode elseBlk = cmdSe.getFilhos().get(2);
            genBlocoComandos(elseBlk);

            code.add(TacInstr.label(endLabel));
        } else {
            // if (cond) then ...
            code.add(TacInstr.jmpFalse(condTemp, endLabel));

            AstNode thenBlk = cmdSe.getFilhos().get(1);
            genBlocoComandos(thenBlk);

            code.add(TacInstr.label(endLabel));
        }
    }

    private void genCmdEnquanto(AstNode cmd) {
        if (cmd.getFilhos().isEmpty()) return;

        String beginLabel = newLabel();
        String endLabel   = newLabel();

        code.add(TacInstr.label(beginLabel));

        AstNode condNode = cmd.getFilhos().get(0);
        String condTemp = genCond(condNode);
        if (condTemp == null) {
            code.add(TacInstr.jmp(endLabel));
            code.add(TacInstr.label(endLabel));
            return;
        }

        code.add(TacInstr.jmpFalse(condTemp, endLabel));

        if (cmd.getFilhos().size() > 1) {
            AstNode body = cmd.getFilhos().get(1);
            genBlocoComandos(body);
        }

        code.add(TacInstr.jmp(beginLabel));
        code.add(TacInstr.label(endLabel));
    }

    private void genBlocoComandos(AstNode bloco) {
        for (AstNode cmd : bloco.getFilhos()) {
            switch (cmd.getKind()) {
                case "CmdAtrib"    -> genCmdAtrib(cmd);
                case "CmdSe"       -> genCmdSe(cmd);
                case "CmdEnquanto" -> genCmdEnquanto(cmd);
                default -> {}
            }
        }
    }

    // ---------------- Expressões / Condições ----------------

    private String genExpr(AstNode e) {
        if (e == null) return null;

        return switch (e.getKind()) {
            case "Numero" -> {
                Token tk = e.getToken();
                String t = newTemp();
                code.add(TacInstr.loadi(t, tk.getLexema()));
                yield t;
            }
            case "Ident" -> {
                Token tk = e.getToken();
                String t = newTemp();
                code.add(TacInstr.load(t, tk.getLexema()));
                yield t;
            }
            case "OpMais" -> {
                String a = genExpr(e.getFilhos().get(0));
                String b = genExpr(e.getFilhos().get(1));
                String t = newTemp();
                code.add(TacInstr.add(t, a, b));
                yield t;
            }
            case "OpMenos" -> {
                String a = genExpr(e.getFilhos().get(0));
                String b = genExpr(e.getFilhos().get(1));
                String t = newTemp();
                code.add(TacInstr.sub(t, a, b));
                yield t;
            }
            case "OpMult" -> {
                String a = genExpr(e.getFilhos().get(0));
                String b = genExpr(e.getFilhos().get(1));
                String t = newTemp();
                code.add(TacInstr.mul(t, a, b));
                yield t;
            }
            case "OpDiv" -> {
                String a = genExpr(e.getFilhos().get(0));
                String b = genExpr(e.getFilhos().get(1));
                String t = newTemp();
                code.add(TacInstr.div(t, a, b));
                yield t;
            }
            case "OpResto" -> {
                String a = genExpr(e.getFilhos().get(0));
                String b = genExpr(e.getFilhos().get(1));
                String t = newTemp();
                code.add(TacInstr.rem(t, a, b));
                yield t;
            }
            default -> null;
        };
    }

            private String genCond(AstNode c) {
        if (c == null) return null;

        return switch (c.getKind()) {
            case "Rel"  -> genRel(c);       // já existia
            case "Nao"  -> genCondNao(c);   // novo: NOT
            case "OpE"  -> genCondE(c);     // novo: AND
            case "OpOU" -> genCondOu(c);    // novo: OR
            default     -> null;
        };
    }

    private String genRel(AstNode rel) {
        if (rel.getFilhos().size() < 2) return null;

        AstNode left  = rel.getFilhos().get(0);
        AstNode right = rel.getFilhos().get(1);

        String a = genExpr(left);
        String b = genExpr(right);
        if (a == null || b == null) return null;

        String t = newTemp();
        Token opTk = rel.getToken();
        TokenTipo tp = (opTk != null ? opTk.getTipo() : null);

        if (tp == TokenTipo.OP_LT) {
            code.add(TacInstr.cmplt(t, a, b));
        } else if (tp == TokenTipo.OP_GT) {
            code.add(TacInstr.cmpgt(t, a, b));
        } else if (tp == TokenTipo.OP_EQ) {
            code.add(TacInstr.cmpeq(t, a, b));
        } else if (tp == TokenTipo.OP_LE) {
            code.add(TacInstr.cmple(t, a, b));
        } else if (tp == TokenTipo.OP_GE) {
            code.add(TacInstr.cmpge(t, a, b));
        } else if (tp == TokenTipo.OP_NE) {
            code.add(TacInstr.cmpne(t, a, b));
        } else {
            // fallback: trata como "<"
            code.add(TacInstr.cmplt(t, a, b));
        }

        return t;
    }
        // NAO <cond>
    private String genCondNao(AstNode naoNode) {
        if (naoNode.getFilhos().isEmpty()) return null;

        // filho é outra condição (Rel, OpE, OpOU, ou outro NAO)
        AstNode inner = naoNode.getFilhos().get(0);
        String v = genCond(inner);
        if (v == null) return null;

        // queremos: resultado = (v == 0)
        String zero = newTemp();
        code.add(TacInstr.loadi(zero, "0"));

        String t = newTemp();
        code.add(TacInstr.cmpeq(t, v, zero)); // t = (v == 0 ? 1 : 0)

        return t;
    }

    // <cond> E <cond>
    private String genCondE(AstNode node) {
        if (node.getFilhos().size() < 2) return null;

        String a = genCond(node.getFilhos().get(0));
        String b = genCond(node.getFilhos().get(1));
        if (a == null || b == null) return null;

        // AND: (a && b) -> (a * b) != 0
        String mul = newTemp();
        code.add(TacInstr.mul(mul, a, b));

        String zero = newTemp();
        code.add(TacInstr.loadi(zero, "0"));

        String t = newTemp();
        code.add(TacInstr.cmpne(t, mul, zero)); // t = (mul != 0 ? 1 : 0)

        return t;
    }

    // <cond> OU <cond>
    private String genCondOu(AstNode node) {
        if (node.getFilhos().size() < 2) return null;

        String a = genCond(node.getFilhos().get(0));
        String b = genCond(node.getFilhos().get(1));
        if (a == null || b == null) return null;

        // OR: (a || b) -> (a + b) != 0
        String sum = newTemp();
        code.add(TacInstr.add(sum, a, b));

        String zero = newTemp();
        code.add(TacInstr.loadi(zero, "0"));

        String t = newTemp();
        code.add(TacInstr.cmpne(t, sum, zero)); // t = (sum != 0 ? 1 : 0)

        return t;
    }

}
