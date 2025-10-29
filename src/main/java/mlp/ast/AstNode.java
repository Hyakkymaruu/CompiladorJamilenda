package mlp.ast;

import java.util.ArrayList;
import java.util.List;
import mlp.Lexico.Token;

/** Nó genérico da AST (única classe de AST que usaremos). */
public class AstNode {

    /** Tipo lógico do nó (ex.: "Programa", "Decl", "CmdSe", "Expr", etc.) */
    private final String kind;

    /** Token associado opcionalmente (para folhas como IDENT, NUM_INT, etc.) */
    private final Token token;

    /** Filhos (ordem significativa) */
    private final List<AstNode> filhos = new ArrayList<>();

    public AstNode(String kind) {
        this(kind, null);
    }
    public AstNode(String kind, Token token) {
        this.kind = kind;
        this.token = token;
    }

    public String getKind() { return kind; }
    public Token getToken() { return token; }
    public List<AstNode> getFilhos() { return filhos; }

    public AstNode add(AstNode child) {
        if (child != null) filhos.add(child);
        return this;
    }

    /** Impressão hierárquica simples da árvore. */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        toTreeString(sb, 0);
        return sb.toString();
    }

    private void toTreeString(StringBuilder sb, int depth) {
        sb.append("  ".repeat(Math.max(0, depth)));
        sb.append(kind);
        if (token != null) {
            sb.append(" [");
            sb.append(token.getTipo()).append(" '").append(token.getLexema()).append("'");
            sb.append(" @").append(token.getLinha()).append(":").append(token.getColuna()).append("]");
        }
        sb.append("\n");
        for (AstNode f : filhos) {
            f.toTreeString(sb, depth + 1);
        }
    }

    @Override
    public String toString() {
        return toTreeString();
    }
}
