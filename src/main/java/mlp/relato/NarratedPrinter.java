package mlp.relato;

import java.io.PrintStream;
import java.util.*;

import mlp.Lexico.Token;
import mlp.Lexico.TokenTipo;
import mlp.ast.AstNode;
import mlp.tac.TacInstr;

/*
 * Impressões com comentários para TOKENS, AST e TAC.
 */
public class NarratedPrinter {

    private final PrintStream out;

    public NarratedPrinter(PrintStream out) {
        this.out = out;
    }

    // ============================================================
    // 1) TOKENS com narração
    // ============================================================
    public void printTokensWithNarration(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) return;

        out.println(">>> TOKENS (com comentários)");
        for (Token tk : tokens) {
            String base = String.format("  %-12s %-12s @%d:%d",
                    tk.getTipo().name(), "'" + tk.getLexema() + "'",
                    tk.getLinha(), tk.getColuna());
            String nota = comentarToken(tk);
            if (!nota.isBlank()) {
                out.println(base + "    // " + nota);
            } else {
                out.println(base);
            }
        }
        out.println();
    }

    private String comentarToken(Token tk) {
        TokenTipo t = tk.getTipo();
        switch (t) {
            case START:        return "Início do programa ($).";
            case END:          return "Fim do programa ($.).";
            case KW_INTEIRO:   return "Declaração de variáveis inteiras.";
            case KW_REAL:      return "Declaração de variáveis reais.";
            case KW_CARACTER:  return "Declaração de variáveis de caractere.";
            case IDENT:        return "Identificador encontrado.";
            case NUM_INT:      return "Constante inteira.";
            case NUM_REAL:     return "Constante real.";
            case OP_ATRIB:     return "Atribuição (=).";
            case OP_MAIS:      return "Operador soma (+).";
            case OP_MULT:      return "Operador multiplicação (*).";
            case OP_DIV:       return "Operador divisão (/).";
            case OP_RESTO:     return "Operador resto (RESTO).";
            case OP_LT:        return "Relacional: menor que (<).";
            case OP_LE:        return "Relacional: menor/igual (<=).";
            case OP_GT:        return "Relacional: maior que (>).";
            case OP_GE:        return "Relacional: maior/igual (>=).";
            case OP_EQ:        return "Relacional: igual (==).";
            case OP_NE:        return "Relacional: diferente (!=).";
            case KW_SE:        return "Início de condicional (se).";
            case KW_ENTAO:     return "Palavra-chave 'entao'.";
            case KW_SENAO:     return "Ramo alternativo 'senao'.";
            case KW_ENQUANTO:  return "Laço 'enquanto'.";
            case KW_ESCREVA:   return "Escreve";
            case KW_NAO:       return "Operador lógico de negação (NAO).";
            case KW_E:         return "Operador lógico E.";
            case KW_OU:        return "Operador lógico OU.";
            case ABRE_PAR:     return "Abre parêntese.";
            case FECHA_PAR:    return "Fecha parêntese.";
            case VIRGULA:      return "Separador de identificadores.";
            case PONTO_VIRG:   return "Fim de comando (;).";
            case EOF:          return "Fim do arquivo-fonte.";
            case INVALIDO:     return "Símbolo inválido.";
            default:           return "";
        }
    }

    // ============================================================
    // 2) AST com narração
    // ============================================================
    public void printAstWithNarration(AstNode raiz) {
        if (raiz == null) return;

        out.println(">>> AST (com comentários explicativos)");
        narrarAst(raiz, 0);
        out.println();
    }

    private void narrarAst(AstNode n, int depth) {
        String indent = "  ".repeat(depth);
        String linha = renderAstLinha(n);
        String nota  = comentarNoAst(n);

        if (!nota.isBlank()) {
            out.println(indent + linha + "    // " + nota);
        } else {
            out.println(indent + linha);
        }

        for (AstNode f : n.getFilhos()) {
            narrarAst(f, depth + 1);
        }
    }

    private String renderAstLinha(AstNode n) {
        String tk = (n.getToken() == null) ? "" :
                String.format(" [%s '%s' @%d:%d]",
                        n.getToken().getTipo().name(),
                        n.getToken().getLexema(),
                        n.getToken().getLinha(),
                        n.getToken().getColuna());
        return n.getKind() + tk;
    }

    private String comentarNoAst(AstNode n) {
        String k = n.getKind();
        switch (k) {
            case "Programa":      return "Raiz: sequência de declarações e comandos.";
            case "Decl":          return "Declaração de uma ou mais variáveis.";
            case "Tipo":          return "Tipo base das variáveis declaradas.";
            case "ListaIdent":    return "Lista de identificadores declarados para o tipo.";
            case "Ident":         return "Uso/menção a um identificador.";
            case "Numero":        return "Constante numérica (int ou real).";

            case "CmdAtrib":      return "Comando de atribuição: LValue = expressão.";
            case "LValue":        return "Destino (variável) da atribuição.";

            case "CmdSe":         return "Condicional 'se ... entao ... [senao ...]'.";
            case "Then":          return "Bloco executado quando a condição é verdadeira.";
            case "Else":          return "Bloco executado quando a condição é falsa.";

            case "CmdEnquanto":   return "Laço 'enquanto (cond) { corpo }'.";
            case "CmdEscreva":    return "Escreveu";

            case "Rel":           return "Expressão relacional (gera valor booleano).";
            case "Nao":           return "Negação lógica de uma condição.";
            case "OpE":           return "Conjunção lógica (AND) entre duas condições.";
            case "OpOU":          return "Disjunção lógica (OR) entre duas condições.";

            case "OpMais":        return "Soma aritmética.";
            case "OpMult":        return "Multiplicação aritmética.";
            case "OpDiv":         return "Divisão aritmética.";
            case "OpResto":       return "Resto de divisão inteira (mod).";

            default:              return "";
        }
    }

    // ============================================================
    // 3) TAC com narração
    // ============================================================
    public void printTacWithNarration(List<TacInstr> tac) {
        if (tac == null || tac.isEmpty()) {
            out.println(">>> CODIGO INTERMEDIARIO (TAC)");
            out.println("  (nenhuma instrucao gerada nesta etapa)");
            out.println();
            return;
        }

        out.println(">>> CODIGO INTERMEDIARIO (TAC)");

        for (TacInstr instr : tac) {
            String raw = safeToString(instr);          // ex.: "LOADI t0, 7"
            String comment = explicarInstrucaoTac(raw);

            if (comment.isBlank()) {
                out.println("  " + raw);
            } else {
                out.printf("  %-30s // %s%n", raw.trim(), comment);
            }
        }
        out.println();
    }

    private String safeToString(TacInstr t) {
        return String.valueOf(t);
    }

    private String explicarInstrucaoTac(String raw) {
        String upper = raw.trim().toUpperCase(Locale.ROOT);

        if (upper.startsWith("LOADI ")) {
            String[] parts = depoisDoEspaco(raw).split(",");
            if (parts.length >= 2) {
                String dst = parts[0].trim();
                String cte = parts[1].trim();
                return "Carrega a constante " + cte + " em " + dst + ".";
            }
            return "Carrega uma constante em um registrador temporário.";
        }

        if (upper.startsWith("LOAD ")) {
            String[] parts = depoisDoEspaco(raw).split(",");
            if (parts.length >= 2) {
                String dst = parts[0].trim();
                String var = parts[1].trim();
                return "Lê o valor de " + var + " para " + dst + ".";
            }
            return "Lê uma variável para um registrador temporário.";
        }

        if (upper.startsWith("STORE ")) {
            String[] parts = depoisDoEspaco(raw).split(",");
            if (parts.length >= 2) {
                String var = parts[0].trim();
                String src = parts[1].trim();
                return "Escreve o valor de " + src + " em " + var + ".";
            }
            return "Escreve o conteúdo de um registrador em uma variável.";
        }

        if (upper.startsWith("ADD "))  return explicarBinaria(raw, "soma");
        if (upper.startsWith("SUB "))  return explicarBinaria(raw, "subtração");
        if (upper.startsWith("MUL "))  return explicarBinaria(raw, "multiplicação");
        if (upper.startsWith("DIV "))  return explicarBinaria(raw, "divisão");
        if (upper.startsWith("REM "))  return explicarBinaria(raw, "resto (mod)");

        if (upper.startsWith("CMPLT ")) return explicarCmp(raw, "<");
        if (upper.startsWith("CMPGT ")) return explicarCmp(raw, ">");
        if (upper.startsWith("CMPEQ ")) return explicarCmp(raw, "==");
        if (upper.startsWith("CMPLE ")) return explicarCmp(raw, "<=");
        if (upper.startsWith("CMPGE ")) return explicarCmp(raw, ">=");
        if (upper.startsWith("CMPNE ")) return explicarCmp(raw, "!=");

        if (upper.startsWith("JMPFALSE ")) {
            String[] parts = depoisDoEspaco(raw).split(",");
            if (parts.length >= 2) {
                String cond = parts[0].trim();
                String lab  = parts[1].trim();
                return "Se " + cond + " == 0, desvia para " + lab + ".";
            }
            return "Salto condicional (falso).";
        }

        if (upper.startsWith("JMPTRUE ")) {
            String[] parts = depoisDoEspaco(raw).split(",");
            if (parts.length >= 2) {
                String cond = parts[0].trim();
                String lab  = parts[1].trim();
                return "Se " + cond + " != 0, desvia para " + lab + ".";
            }
            return "Salto condicional (verdadeiro).";
        }

        if (upper.startsWith("JMP ")) {
            String rest = depoisDoEspaco(raw).trim();
            return "Salto incondicional para " + rest + ".";
        }

        if (upper.startsWith("LABEL ")) {
            return "Rótulo de controle de fluxo.";
        }

        return "";
    }

    private String explicarBinaria(String s, String nome) {
        String[] parts = depoisDoEspaco(s).split(",");
        if (parts.length >= 3) {
            String dst = parts[0].trim();
            String a   = parts[1].trim();
            String b   = parts[2].trim();
            return dst + " recebe " + nome + " entre " + a + " e " + b + ".";
        }
        return "Operação binária (" + nome + ").";
    }

    private String explicarCmp(String s, String op) {
        String[] parts = depoisDoEspaco(s).split(",");
        if (parts.length >= 3) {
            String dst = parts[0].trim();
            String a   = parts[1].trim();
            String b   = parts[2].trim();
            return dst + " recebe 1 se " + a + " " + op + " " + b + ", senão 0.";
        }
        return "Comparação relacional " + op + ".";
    }

    private String depoisDoEspaco(String s) {
        int i = s.indexOf(' ');
        if (i < 0) return "";
        return s.substring(i + 1).trim();
    }
}
