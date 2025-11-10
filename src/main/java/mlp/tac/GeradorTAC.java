package mlp.tac;

import java.util.ArrayList;
import java.util.List;

import mlp.Lexico.Token;
import mlp.ast.AstNode;

/**
 * Gerador de código intermediário (TAC) para a MLP.
 *
 * Nesta primeira etapa, gera:
 *  - Atribuições simples: x = 1;  x = y; x = y + 1; etc.
 *  - Expressões aritméticas: +, *, /, RESTO
 *
 * Convenções:
 *  - Variáveis da linguagem: usamos o próprio nome (ex: "a", "b").
 *  - Temporários: t0, t1, t2, ...
 *
 * Mnemônicos utilizados (coerentes com a tabela do professor):
 *  - LOADI R, const
 *  - LOAD  R, var
 *  - STORE var, R
 *  - ADD   R, R1, R2
 *  - SUB   R, R1, R2   (reservado, ainda não usado)
 *  - MUL   R, R1, R2
 *  - DIV   R, R1, R2
 *  - RESTO R, R1, R2
 */
public class GeradorTAC {

    private final List<TacInstr> codigo = new ArrayList<>();
    private int tempCount = 0;

    /** Gera TAC para um programa inteiro (nó "Programa"). */
    public List<TacInstr> gerar(AstNode programa) {
        codigo.clear();
        tempCount = 0;

        if (programa == null) {
            return new ArrayList<>();
        }

        // Percorre filhos do Programa: Decl, CmdAtrib, CmdSe, CmdEnquanto...
        for (AstNode filho : programa.getFilhos()) {
            switch (filho.getKind()) {
                case "CmdAtrib"    -> gerarCmdAtrib(filho);
                // "CmdSe" e "CmdEnquanto" virão em etapas posteriores
                default -> {
                    // Declarações em si não geram código (apenas tabela de símbolos)
                }
            }
        }

        return new ArrayList<>(codigo);
    }

    // ------------------- Comandos -------------------

    /** Gera TAC para um comando de atribuição: CmdAtrib. */
    private void gerarCmdAtrib(AstNode cmd) {
        if (cmd.getFilhos().size() < 2) return;

        AstNode lvalue = cmd.getFilhos().get(0);
        AstNode expr   = cmd.getFilhos().get(1);

        String destino = extrairNomeIdent(lvalue);
        String origem  = gerarExpr(expr);

        // Se origem for null, não gera nada.
        if (origem == null || destino == null) return;

        // Convenção:
        //   STORE var, Rorigem
        codigo.add(new TacInstr("STORE", destino, origem, null));
    }

    // ------------------- Expressões -------------------

    /**
     * Gera TAC para uma expressão aritmética e retorna o nome do "registrador"
     * (temporário) onde o resultado fica.
     */
    private String gerarExpr(AstNode e) {
        if (e == null) return null;

        String kind = e.getKind();

        switch (kind) {
            case "Numero" -> {
                Token tk = e.getToken();
                if (tk == null) return null;
                String lex = tk.getLexema();
                String t = novoTemp();
                // LOADI t, constante
                codigo.add(new TacInstr("LOADI", t, lex, null));
                return t;
            }

            case "Ident" -> {
                Token tk = e.getToken();
                if (tk == null) return null;
                String nome = tk.getLexema();
                String t = novoTemp();
                // LOAD t, var
                codigo.add(new TacInstr("LOAD", t, nome, null));
                return t;
            }

            case "OpMais" -> {
                // a + b
                if (e.getFilhos().size() < 2) return null;
                String r1 = gerarExpr(e.getFilhos().get(0));
                String r2 = gerarExpr(e.getFilhos().get(1));
                if (r1 == null || r2 == null) return null;
                String t = novoTemp();
                codigo.add(new TacInstr("ADD", t, r1, r2));
                return t;
            }

            case "OpMult" -> {
                if (e.getFilhos().size() < 2) return null;
                String r1 = gerarExpr(e.getFilhos().get(0));
                String r2 = gerarExpr(e.getFilhos().get(1));
                if (r1 == null || r2 == null) return null;
                String t = novoTemp();
                codigo.add(new TacInstr("MUL", t, r1, r2));
                return t;
            }

            case "OpDiv" -> {
                if (e.getFilhos().size() < 2) return null;
                String r1 = gerarExpr(e.getFilhos().get(0));
                String r2 = gerarExpr(e.getFilhos().get(1));
                if (r1 == null || r2 == null) return null;
                String t = novoTemp();
                codigo.add(new TacInstr("DIV", t, r1, r2));
                return t;
            }

            case "OpResto" -> {
                if (e.getFilhos().size() < 2) return null;
                String r1 = gerarExpr(e.getFilhos().get(0));
                String r2 = gerarExpr(e.getFilhos().get(1));
                if (r1 == null || r2 == null) return null;
                String t = novoTemp();
                // Usamos o mnemônico RESTO, seguindo o operador da linguagem
                codigo.add(new TacInstr("RESTO", t, r1, r2));
                return t;
            }

            default -> {
                // Por enquanto, outras construções (lógicas, relacionais, etc.)
                // não são tratadas aqui; serão implementadas nas próximas etapas.
                // Para não travar o gerador, tentamos olhar o primeiro filho.
                if (!e.getFilhos().isEmpty()) {
                    return gerarExpr(e.getFilhos().get(0));
                }
                return null;
            }
        }
    }

    // ------------------- Helpers -------------------

    /** Gera um novo nome de temporário: t0, t1, t2, ... */
    private String novoTemp() {
        return "t" + (tempCount++);
    }

    /**
     * Extrai o nome do identificador a partir de um nó LValue ou Ident.
     * Seu parser cria CmdAtrib assim:
     *   CmdAtrib
     *     LValue
     *       Ident
     *     (expressão)
     */
    private String extrairNomeIdent(AstNode no) {
        if (no == null) return null;

        // Caso já seja um Ident
        if ("Ident".equals(no.getKind())) {
            Token tk = no.getToken();
            return (tk != null) ? tk.getLexema() : null;
        }

        // Caso seja LValue com filho Ident
        if ("LValue".equals(no.getKind()) && !no.getFilhos().isEmpty()) {
            AstNode idNo = no.getFilhos().get(0);
            if (idNo != null && "Ident".equals(idNo.getKind())) {
                Token tk = idNo.getToken();
                return (tk != null) ? tk.getLexema() : null;
            }
        }

        // Fallback: procura recursivamente
        for (AstNode f : no.getFilhos()) {
            String nome = extrairNomeIdent(f);
            if (nome != null) return nome;
        }

        return null;
    }
}
