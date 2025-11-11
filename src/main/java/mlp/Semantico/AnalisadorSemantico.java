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

    // Códigos padronizados
    private static final int SEM_VAR_NAO_DECL          = 2001;
    private static final int SEM_VAR_REDECLARADA       = 2002;
    private static final int SEM_TIPO_INCOMPATIVEL     = 2003;
    private static final int SEM_COND_NAO_BOLEANA      = 2004;
    private static final int SEM_PROFUNDIDADE_EXCEDIDA = 2005; // NOVO

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
                case "Decl"        -> analisarDecl(filho);
                case "CmdAtrib"    -> analisarCmdAtrib(filho);
                case "CmdSe"       -> analisarCmdSe(filho, 1);        // profundidade 1
                case "CmdEnquanto" -> analisarCmdEnquanto(filho, 1);  // profundidade 1
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
                    diagnosticos.add(new Diagnostico(
                        Tipo.SEMANTICO, SEM_VAR_REDECLARADA,
                        "variável já declarada: " + nome,
                        tk.getLinha(), tk.getColuna(), nome
                    ));
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
        // Se o comando de atribuição está incompleto (por erro sintático),
        // ele pode ter só o LValue como filho. Nesse caso, não há o que checar.
        if (n == null || n.getFilhos().size() < 2) {
            return; // evita IndexOutOfBoundsException
        }

        // Destino
        AstNode lvalue = n.getFilhos().get(0);
        // LValue pode ter o token diretamente ou ter um filho "Ident"
        Token idTk;
        if (lvalue.getFilhos().isEmpty()) {
            idTk = lvalue.getToken(); // parser coloca o IDENT aqui
        } else {
            idTk = lvalue.getFilhos().get(0).getToken(); // fallback se houver filho
        }
        String nome = (idTk == null ? "<desconhecido>" : idTk.getLexema());
        TipoSimples tDest = tipoDeIdent(idTk);

        // Expressão
        AstNode expr = n.getFilhos().get(1);
        TipoSimples tExpr = tipoExpr(expr);

        // Compatibilidade: igual ou (INT -> REAL)
        if (!compatAtrib(tDest, tExpr)) {
            diagnosticos.add(new Diagnostico(
                Tipo.SEMANTICO, SEM_TIPO_INCOMPATIVEL,
                "tipos incompatíveis na atribuição: " + tDest + " = " + tExpr,
                (idTk != null ? idTk.getLinha() : 0),
                (idTk != null ? idTk.getColuna() : 0),
                nome
            ));
        }
    }

    private void analisarCmdSe(AstNode n, int profundidade) {
        // Checa profundidade máxima (10) apenas para comandos de controle
        if (profundidade > 10) {
            Token t = n.getToken();
            diagnosticos.add(new Diagnostico(
                Tipo.SEMANTICO, SEM_PROFUNDIDADE_EXCEDIDA,
                "profundidade máxima de comandos (10) excedida",
                (t != null ? t.getLinha() : 0),
                (t != null ? t.getColuna() : 0),
                null
            ));
        }

        // Estrutura vinda do parser:
        // CmdSe
        //   (condição)
        //   Then -> (comandos)
        //   [Else -> (comandos)]
        if (n.getFilhos().isEmpty()) return;

        AstNode cond = n.getFilhos().get(0);
        TipoSimples tCond = tipoCond(cond);
        if (tCond != TipoSimples.BOOL && tCond != TipoSimples.ERRO) {
            Token t = cond.getToken();
            diagnosticos.add(new Diagnostico(
                Tipo.SEMANTICO, SEM_COND_NAO_BOLEANA,
                "condição de 'se' deve ser booleana",
                (t != null ? t.getLinha() : 0),
                (t != null ? t.getColuna() : 0),
                null
            ));
        }

        // analisar comandos internos (then/else), se existirem
        for (int i = 1; i < n.getFilhos().size(); i++) {
            AstNode bloco = n.getFilhos().get(i);
            for (AstNode cmd : bloco.getFilhos()) {
                switch (cmd.getKind()) {
                    case "CmdAtrib"     -> analisarCmdAtrib(cmd);
                    case "CmdSe"        -> analisarCmdSe(cmd, profundidade + 1);
                    case "CmdEnquanto"  -> analisarCmdEnquanto(cmd, profundidade + 1);
                    default -> {}
                }
            }
        }
    }

    private void analisarCmdEnquanto(AstNode n, int profundidade) {
        // Checa profundidade máxima (10) apenas para comandos de controle
        if (profundidade > 10) {
            Token t = n.getToken();
            diagnosticos.add(new Diagnostico(
                Tipo.SEMANTICO, SEM_PROFUNDIDADE_EXCEDIDA,
                "profundidade máxima de comandos (10) excedida",
                (t != null ? t.getLinha() : 0),
                (t != null ? t.getColuna() : 0),
                null
            ));
        }

        // CmdEnquanto
        //   (condição)
        //   Body -> (comandos)
        if (n.getFilhos().isEmpty()) return;

        AstNode cond = n.getFilhos().get(0);
        TipoSimples tCond = tipoCond(cond);
        if (tCond != TipoSimples.BOOL && tCond != TipoSimples.ERRO) {
            Token t = cond.getToken();
            diagnosticos.add(new Diagnostico(
                Tipo.SEMANTICO, SEM_COND_NAO_BOLEANA,
                "condição de 'enquanto' deve ser booleana",
                (t != null ? t.getLinha() : 0),
                (t != null ? t.getColuna() : 0),
                null
            ));
        }

        // corpo
        if (n.getFilhos().size() > 1) {
            AstNode body = n.getFilhos().get(1);
            for (AstNode cmd : body.getFilhos()) {
                switch (cmd.getKind()) {
                    case "CmdAtrib"     -> analisarCmdAtrib(cmd);
                    case "CmdSe"        -> analisarCmdSe(cmd, profundidade + 1);
                    case "CmdEnquanto"  -> analisarCmdEnquanto(cmd, profundidade + 1);
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
                    case NUM_INT  -> TipoSimples.INT;
                    case NUM_REAL -> TipoSimples.REAL;
                    default       -> TipoSimples.ERRO;
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
                    diagnosticos.add(new Diagnostico(
                        Tipo.SEMANTICO, SEM_TIPO_INCOMPATIVEL,
                        "RESTO requer operandos inteiros",
                        (t != null ? t.getLinha() : 0),
                        (t != null ? t.getColuna() : 0),
                        null
                    ));
                    return TipoSimples.ERRO;
                }
                return TipoSimples.INT;
            }
            default -> {
                // Nó lógico/relacional indevido dentro de expressão numérica
                if ("Rel".equals(k) || "Nao".equals(k) || "OpE".equals(k) || "OpOU".equals(k)) {
                    Token t = e.getToken();
                    diagnosticos.add(new Diagnostico(
                        Tipo.SEMANTICO, SEM_TIPO_INCOMPATIVEL,
                        "expressão numérica inválida (nó lógico/relacional em expressão)",
                        (t != null ? t.getLinha() : 0),
                        (t != null ? t.getColuna() : 0),
                        null
                    ));
                    return TipoSimples.ERRO;
                }
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
                // Rel -> expr opRel expr
                TipoSimples a = tipoOpndRel(c.getFilhos().get(0));
                TipoSimples b = tipoOpndRel(c.getFilhos().get(1));
                if (!ehNumerico(a) || !ehNumerico(b)) return TipoSimples.ERRO;
                return TipoSimples.BOOL;
            }
            default -> {
                // Se veio expressão numérica pura como condição, é erro
                if ("Ident".equals(k) || "Numero".equals(k)
                    || "OpMais".equals(k) || "OpMult".equals(k)
                    || "OpDiv".equals(k) || "OpResto".equals(k)) {
                    return TipoSimples.ERRO;
                }
                // fallback: tenta filho
                if (!c.getFilhos().isEmpty()) {
                    return tipoCond(c.getFilhos().get(0));
                }
                return TipoSimples.ERRO;
            }
        }
    }

    private TipoSimples tipoOpndRel(AstNode opnd) {
        // opndRel = IDENT | NUM_INT | NUM_REAL | '(' expressao ')'
        return tipoExpr(opnd);
    }

    // ---------------- Utilidades de tipo ----------------

    private TipoSimples tipoDeIdent(Token idToken) {
        if (idToken == null) return TipoSimples.ERRO;
        String nome = idToken.getLexema();
        TabelaSimbolos.Entrada e = ts.obter(nome);
        if (e == null) {
            diagnosticos.add(new Diagnostico(
                Tipo.SEMANTICO, SEM_VAR_NAO_DECL,
                "variável não declarada: " + nome,
                idToken.getLinha(), idToken.getColuna(), nome
            ));
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
            diagnosticos.add(new Diagnostico(
                Tipo.SEMANTICO, SEM_TIPO_INCOMPATIVEL,
                "soma requer operandos numéricos",
                (t != null ? t.getLinha() : 0),
                (t != null ? t.getColuna() : 0),
                null
            ));
            return TipoSimples.ERRO;
        }
        return (a == TipoSimples.REAL || b == TipoSimples.REAL) ? TipoSimples.REAL : TipoSimples.INT;
    }

    private TipoSimples promoverMulDiv(TipoSimples a, TipoSimples b, AstNode no) {
        if (!ehNumerico(a) || !ehNumerico(b)) {
            Token t = no.getToken();
            diagnosticos.add(new Diagnostico(
                Tipo.SEMANTICO, SEM_TIPO_INCOMPATIVEL,
                "multiplicação/divisão requerem operandos numéricos",
                (t != null ? t.getLinha() : 0),
                (t != null ? t.getColuna() : 0),
                null
            ));
            return TipoSimples.ERRO;
        }
        return (a == TipoSimples.REAL || b == TipoSimples.REAL) ? TipoSimples.REAL : TipoSimples.INT;
    }

    private boolean compatAtrib(TipoSimples destino, TipoSimples expr) {
        if (destino == TipoSimples.ERRO || expr == TipoSimples.ERRO) return true; // evita cascata
        if (destino == expr) return true;
        // promoção permitida: INT -> REAL
        return (destino == TipoSimples.REAL && expr == TipoSimples.INT);
    }
}
