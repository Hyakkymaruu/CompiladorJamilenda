package mlp.tac;

import java.util.ArrayList;
import java.util.List;

import mlp.ast.AstNode;
import mlp.Lexico.Token;

/**
 * Gerador de código intermediário (TAC) bem simples.
 *
 * Suporta, por enquanto:
 *  - Atribuições:
 *      IDENT = Numero
 *      IDENT = Ident
 *      IDENT = expr_soma/mul
 *
 *  - Expressões:
 *      Numero      -> LOADI
 *      Ident       -> LOAD
 *      OpMais      -> ADD
 *      OpMult      -> MUL
 */
public class GeradorTAC {

    private final List<TacInstr> instrs = new ArrayList<>();
    private int tempCount = 0;

    /** Ponto de entrada: gera TAC para um Programa inteiro. */
    public List<TacInstr> gerar(AstNode programa) {
        instrs.clear();
        tempCount = 0;

        if (programa == null) return instrs;

        for (AstNode filho : programa.getFilhos()) {
            switch (filho.getKind()) {
                case "CmdAtrib" -> genCmdAtrib(filho);
                // futuros:
                // case "CmdSe" -> genCmdSe(filho);
                // case "CmdEnquanto" -> genCmdEnquanto(filho);
                default -> {
                    // Decl, etc: ignorados pelo TAC
                }
            }
        }

        return instrs;
    }

    // ---------- Comandos ----------

    private void genCmdAtrib(AstNode cmd) {
        // CmdAtrib
        //   LValue
        //     Ident [IDENT 'a']
        //   <expr>
        if (cmd.getFilhos().size() < 2) return;

        AstNode lvalue = cmd.getFilhos().get(0);
        AstNode expr   = cmd.getFilhos().get(1);

        // pegar nome do destino
        String dest = "<tmp>";
        if (!lvalue.getFilhos().isEmpty()) {
            Token idTk = lvalue.getFilhos().get(0).getToken();
            if (idTk != null) dest = idTk.getLexema();
        } else if (lvalue.getToken() != null) {
            dest = lvalue.getToken().getLexema();
        }

        // gera código para a expressão do lado direito
        String srcReg = genExpr(expr);

        // garante STORE SEMPRE que tivermos um registrador fonte
        if (srcReg != null) {
            instrs.add(TacInstr.store(dest, srcReg));
        }
    }

    // ---------- Expressões ----------

    /**
     * Gera TAC para uma expressão e retorna o registrador (temp) onde está o resultado.
     */
    private String genExpr(AstNode e) {
        if (e == null) return null;

        String kind = e.getKind();

        switch (kind) {
            case "Numero" -> {
                Token tk = e.getToken();
                String temp = novoTemp();
                String lex = (tk != null ? tk.getLexema() : "0");
                instrs.add(TacInstr.loadi(temp, lex));
                return temp;
            }

            case "Ident" -> {
                Token tk = e.getToken();
                String temp = novoTemp();
                String nome = (tk != null ? tk.getLexema() : "<anon>");
                instrs.add(TacInstr.load(temp, nome));
                return temp;
            }

            case "OpMais" -> {
                // filho0 + filho1
                AstNode left = safeChild(e, 0);
                AstNode right = safeChild(e, 1);
                String r1 = genExpr(left);
                String r2 = genExpr(right);
                String r3 = novoTemp();
                instrs.add(TacInstr.add(r3, r1, r2));
                return r3;
            }

            case "OpMult" -> {
                // filho0 * filho1
                AstNode left = safeChild(e, 0);
                AstNode right = safeChild(e, 1);
                String r1 = genExpr(left);
                String r2 = genExpr(right);
                String r3 = novoTemp();
                instrs.add(TacInstr.mul(r3, r1, r2));
                return r3;
            }

            case "OpDiv" -> {
                AstNode left = safeChild(e, 0);
                AstNode right = safeChild(e, 1);
                String r1 = genExpr(left);
                String r2 = genExpr(right);
                String r3 = novoTemp();
                instrs.add(TacInstr.div(r3, r1, r2));
                return r3;
            }

            case "OpResto" -> {
                AstNode left = safeChild(e, 0);
                AstNode right = safeChild(e, 1);
                String r1 = genExpr(left);
                String r2 = genExpr(right);
                String r3 = novoTemp();
                instrs.add(TacInstr.resto(r3, r1, r2));
                return r3;
            }

            default -> {
                // Qualquer coisa que não conhecemos -> tenta primeiro filho
                if (!e.getFilhos().isEmpty()) {
                    return genExpr(e.getFilhos().get(0));
                }
                return null;
            }
        }
    }

    private AstNode safeChild(AstNode n, int idx) {
        if (n == null) return null;
        if (idx < 0 || idx >= n.getFilhos().size()) return null;
        return n.getFilhos().get(idx);
    }

    private String novoTemp() {
        return "t" + (tempCount++);
    }
}
