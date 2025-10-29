package mlp.Semantico;

import java.util.ArrayList;
import java.util.List;

import mlp.Erros.Diagnostico;
import mlp.Erros.Diagnostico.Tipo;
import mlp.Lexico.Token;
import mlp.Simbolos.TabelaSimbolos;
import mlp.Simbolos.TabelaSimbolos.TipoSimples;
import mlp.ast.AstNode;

public class AnalisadorSemantico {

    private final TabelaSimbolos ts = new TabelaSimbolos();
    private final List<Diagnostico> diagnosticos = new ArrayList<>();

    public TabelaSimbolos getTabela() { return ts; }
    public List<Diagnostico> getDiagnosticos() { return diagnosticos; }

    /** Dispara a análise a partir do nó Programa. */
    public void analisar(AstNode programa) {
        if (programa == null) return;
        // Percorre filhos do Programa (Decl e Comandos)
        for (AstNode filho : programa.getFilhos()) {
            switch (filho.getKind()) {
                case "Decl" -> analisarDecl(filho);
                case "CmdAtrib" -> analisarCmdAtrib(filho);
                case "CmdSe" -> analisarCmdSe(filho);
                case "CmdEnquanto" -> analisarCmdEnquanto(filho);
                default -> { /* ignorar outros rótulos (ex.: ComandoInvalido) */ }
            }
        }
    }

    // ---------------- Declarações ----------------

    private void analisarDecl(AstNode decl) {
        // Estrutura esperada:
        // Decl
        //   Tipo [KW_* 'inteiro|real|caracter']
        //   ListaIdent
        //     Ident [IDENT 'x']
        //     Ident [IDENT 'y'] ...
        if (decl.getFilhos().isEmpty()) return;

        // 1) Tipo
        AstNode tipoNo = decl.getFilhos().get(0);
        TipoSimples tipo = mapearTipoBasico(tipoNo.getToken());

        // 2) Identificadores
        if (decl.getFilhos().size() > 1) {
            AstNode lista = decl.getFilhos().get(1);
            for (AstNode idNo : lista.getFilhos()) {
                if (!"Ident".equals(idNo.getKind())) continue;
                Token tk = idNo.getToken();
                String nome = tk.getLexema();

                if (!ts.declarar(nome, tipo, tk.getLinha(), tk.getColuna())) {
                    diagnosticos.add(new Diagnostico(Tipo.SEMANTICO, 21,
                        "identificador já declarado: " + nome,
                        tk.getLinha(), tk.getColuna(), nome));
                }
            }
        }
    }

    private TipoSimples mapearTipoBasico(Token tipoToken) {
        if (tipoToken == null) return TipoSimples.ERRO;
        return switch (tipoToken.getTipo()) {
            case KW_INTEIRO -> TipoSimples.INT;
            case KW_REAL    -> TipoSimples.REAL;
            case KW_CARACTER-> TipoSimples.CHAR;
            default         -> TipoSimples.ERRO;
        };
    }

    // ---------------- Comandos ----------------

    private void analisarCmdAtrib(AstNode n) {
        // CmdAtrib
        //   LValue
        //     Ident [IDENT 'x']
        //   <expressão>
        if (n.getFilhos().size() < 2) return;

        // Destino
        AstNode lvalue = n.getFilhos().get(0);
        Token idTk = (lvalue.getFilhos().isEmpty() ? null : lvalue.getFilhos().get(0).getToken());
        String nome = (idTk == null ? "<desconhecido>" : idTk.getLexema());
        TipoSimples tDest = tipoDeIdent(idTk);

        // Expressão
        AstNode expr = n.getFilhos().get(1);
        TipoSimples tExpr = tipoExpr(expr);

        // Compatibilidade: igual ou (INT -> REAL)
        if (!compatAtrib(tDest, tExpr)) {
            diagnosticos.add(new Diagnostico(Tipo.SEMANTICO, 22,
                "tipos incompatíveis na atribuição: " + tDest + " = " + tExpr,
                (idTk != null ? idTk.getLinha() : 0), (idTk != null ? idTk.getColuna() : 0), nome));
        }
    }

    private void analisarCmdSe(AstNode n) {
        // CmdSe
        //   Then( cond já analisada antes? não – a cond é filho direto do CmdSe )
        // Estrutura vinda do parser:
        // CmdSe
        //   (condição)
        //   Then -> (comando)
        //   [Else -> (comando)]
        if (n.getFilhos().isEmpty()) return;

        AstNode cond = n.getFilhos().get(0);
        TipoSimples tCond = tipoCond(cond);
        if (tCond != TipoSimples.BOOL && tCond != TipoSimples.ERRO) {
            Token t = cond.getToken();
            diagnosticos.add(new Diagnostico(Tipo.SEMANTICO, 22,
                "condição de 'se' deve ser booleana",
                (t != null ? t.getLinha() : 0), (t != null ? t.getColuna() : 0), null));
        }

        // analisar comandos internos (then/else), se existirem
        for (int i = 1; i < n.getFilhos().size(); i++) {
            AstNode bloco = n.getFilhos().get(i);
            for (AstNode cmd : bloco.getFilhos()) {
                switch (cmd.getKind()) {
                    case "CmdAtrib" -> analisarCmdAtrib(cmd);
                    case "CmdSe" -> analisarCmdSe(cmd);
                    case "CmdEnquanto" -> analisarCmdEnquanto(cmd);
                    default -> {}
                }
            }
        }
    }

    private void analisarCmdEnquanto(AstNode n) {
        // CmdEnquanto
        //   (condição)
        //   Body -> (comando)
        if (n.getFilhos().isEmpty()) return;

        AstNode cond = n.getFilhos().get(0);
        TipoSimples tCond = tipoCond(cond);
        if (tCond != TipoSimples.BOOL && tCond != TipoSimples.ERRO) {
            Token t = cond.getToken();
            diagnosticos.add(new Diagnostico(Tipo.SEMANTICO, 22,
                "condição de 'enquanto' deve ser booleana",
                (t != null ? t.getLinha() : 0), (t != null ? t.getColuna() : 0), null));
        }

        // corpo
        if (n.getFilhos().size() > 1) {
            AstNode body = n.getFilhos().get(1);
            for (AstNode cmd : body.getFilhos()) {
                switch (cmd.getKind()) {
                    case "CmdAtrib" -> analisarCmdAtrib(cmd);
                    case "CmdSe" -> analisarCmdSe(cmd);
                    case "CmdEnquanto" -> analisarCmdEnquanto(cmd);
                    default -> {}
                }
            }
        }
    }

    // ---------------- Tipagem de expressões/condições ----------------

    /** Retorna o tipo de uma expressão numérica (ou ERRO). */
    private TipoSimples tipoExpr(AstNode e) {
        if (e == null) return TipoSimples.ERRO;

        String k = e.getKind();
        switch (k) {
            case "Ident" -> {
                return tipoDeIdent(e.getToken());
            }
            case "Numero" -> {
                Token tk = e.getToken();
                if (tk == null) return TipoSimples.ERRO;
                return switch (tk.getTipo()) {
                    case NUM_INT -> TipoSimples.INT;
                    case NUM_REAL -> TipoSimples.REAL;
                    default -> TipoSimples.ERRO;
                };
            }
            case "OpMais" -> {
                TipoSimples a = tipoExpr(e.getFilhos().get(0));
                TipoSimples b = tipoExpr(e.getFilhos().get(1));
                return promoverSoma(a, b, e);
            }
            case "OpMult", "OpDiv" -> {
                TipoSimples a = tipoExpr(e.getFilhos().get(0));
                TipoSimples b = tipoExpr(e.getFilhos().get(1));
                return promoverMulDiv(a, b, e);
            }
            case "OpResto" -> {
                TipoSimples a = tipoExpr(e.getFilhos().get(0));
                TipoSimples b = tipoExpr(e.getFilhos().get(1));
                if (a != TipoSimples.INT || b != TipoSimples.INT) {
                    Token t = e.getToken();
                    diagnosticos.add(new Diagnostico(Tipo.SEMANTICO, 22,
                        "RESTO requer operandos inteiros",
                        (t != null ? t.getLinha() : 0), (t != null ? t.getColuna() : 0), null));
                    return TipoSimples.ERRO;
                }
                return TipoSimples.INT;
            }
            default -> {
                // Se for um nó de relação usado por engano em expressão, marque erro
                if ("Rel".equals(k) || "Nao".equals(k) || "OpE".equals(k) || "OpOU".equals(k)) {
                    Token t = e.getToken();
                    diagnosticos.add(new Diagnostico(Tipo.SEMANTICO, 22,
                        "expressão numérica inválida (nó lógico/relacional em expressão)",
                        (t != null ? t.getLinha() : 0), (t != null ? t.getColuna() : 0), null));
                    return TipoSimples.ERRO;
                }
                // Nós inválidos/placeholder
                return TipoSimples.ERRO;
            }
        }
    }

    /** Retorna o tipo de uma condição (BOOL ou ERRO). */
    private TipoSimples tipoCond(AstNode c) {
        if (c == null) return TipoSimples.ERRO;
        String k = c.getKind();

        switch (k) {
            case "Nao" -> {
                TipoSimples t = tipoCond(c.getFilhos().get(0));
                if (t != TipoSimples.BOOL) return TipoSimples.ERRO;
                return TipoSimples.BOOL;
            }
            case "OpE", "OpOU" -> {
                TipoSimples a = tipoCond(c.getFilhos().get(0));
                TipoSimples b = tipoCond(c.getFilhos().get(1));
                if (a != TipoSimples.BOOL || b != TipoSimples.BOOL) return TipoSimples.ERRO;
                return TipoSimples.BOOL;
            }
            case "Rel" -> {
                // Rel -> opndRel opRel opndRel
                // opndRel pode ser IDENT/NUM/expr parentizada
                TipoSimples a = tipoOpndRel(c.getFilhos().get(0));
                TipoSimples b = tipoOpndRel(c.getFilhos().get(1));
                if (!ehNumerico(a) || !ehNumerico(b)) return TipoSimples.ERRO;
                return TipoSimples.BOOL;
            }
            default -> {
                // Pode ser uma parênteseada que virou expressão numérica por engano
                if ("Ident".equals(k) || "Numero".equals(k) || "OpMais".equals(k)
                    || "OpMult".equals(k) || "OpDiv".equals(k) || "OpResto".equals(k)) {
                    // condição com expressão numérica pura => ERRO
                    return TipoSimples.ERRO;
                }
                // Tentar avaliar recursivamente filhos (casos inesperados)
                if (!c.getFilhos().isEmpty()) {
                    return tipoCond(c.getFilhos().get(0));
                }
                return TipoSimples.ERRO;
            }
        }
    }

    private TipoSimples tipoOpndRel(AstNode opnd) {
        // opndRel = IDENT | NUM_INT | NUM_REAL | '(' expressao ')'
        String k = opnd.getKind();
        if ("Ident".equals(k) || "Numero".equals(k)
            || "OpMais".equals(k) || "OpMult".equals(k) || "OpDiv".equals(k) || "OpResto".equals(k)) {
            // Se for número/ident, ou expressão numérica
            return tipoExpr(opnd);
        }
        // Parênteses já foram resolvidos pelo parser como a própria expressão
        return tipoExpr(opnd);
    }

    // ---------------- Utilidades de tipo ----------------

    private TipoSimples tipoDeIdent(Token idToken) {
        if (idToken == null) return TipoSimples.ERRO;
        String nome = idToken.getLexema();
        TabelaSimbolos.Entrada e = ts.obter(nome);
        if (e == null) {
            diagnosticos.add(new Diagnostico(Tipo.SEMANTICO, 20,
                "identificador não declarado: " + nome,
                idToken.getLinha(), idToken.getColuna(), nome));
            return TipoSimples.ERRO;
        }
        return e.tipo;
    }

    private boolean ehNumerico(TipoSimples t) {
        return t == TipoSimples.INT || t == TipoSimples.REAL;
    }

    private TipoSimples promoverSoma(TipoSimples a, TipoSimples b, AstNode no) {
        if (!ehNumerico(a) || !ehNumerico(b)) {
            Token t = no.getToken();
            diagnosticos.add(new Diagnostico(Tipo.SEMANTICO, 22,
                "soma requer operandos numéricos",
                (t != null ? t.getLinha() : 0), (t != null ? t.getColuna() : 0), null));
            return TipoSimples.ERRO;
        }
        return (a == TipoSimples.REAL || b == TipoSimples.REAL) ? TipoSimples.REAL : TipoSimples.INT;
    }

    private TipoSimples promoverMulDiv(TipoSimples a, TipoSimples b, AstNode no) {
        if (!ehNumerico(a) || !ehNumerico(b)) {
            Token t = no.getToken();
            diagnosticos.add(new Diagnostico(Tipo.SEMANTICO, 22,
                "multiplicação/divisão requerem operandos numéricos",
                (t != null ? t.getLinha() : 0), (t != null ? t.getColuna() : 0), null));
            return TipoSimples.ERRO;
        }
        // divisão pode produzir real se algum operando for real
        return (a == TipoSimples.REAL || b == TipoSimples.REAL) ? TipoSimples.REAL : TipoSimples.INT;
    }

    private boolean compatAtrib(TipoSimples destino, TipoSimples expr) {
        if (destino == TipoSimples.ERRO || expr == TipoSimples.ERRO) return true; // evita cascata
        if (destino == expr) return true;
        // promoção permitida: INT -> REAL
        return (destino == TipoSimples.REAL && expr == TipoSimples.INT);
    }
}
