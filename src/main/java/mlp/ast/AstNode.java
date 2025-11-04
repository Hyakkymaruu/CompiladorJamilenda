package mlp.ast;

import java.util.ArrayList;
import java.util.List;

import mlp.Lexico.Token;

public class AstNode {
    private final String kind;
    private final Token token;
    private final List<AstNode> filhos = new ArrayList<>();

    public AstNode(String kind, Token token) {
        this.kind = kind;
        this.token = token;
    }

    // --- getters esperados pelo restante do projeto ---
    public String getKind() { return kind; }
    public Token getToken() { return token; }
    public List<AstNode> getFilhos() { return filhos; }

    // --- NOVO: compat layer para o parser ---
    public void addFilho(AstNode n) {
        if (n != null) filhos.add(n);
    }

    // (opcionais) alias para compatibilidade, caso em algum ponto usem outros nomes
    public void addChild(AstNode n) { addFilho(n); }
    public void add(AstNode n) { addFilho(n); }

    // Impressão em árvore usada pelo Main.toTreeString()
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        toTreeString(sb, 0);
        return sb.toString();
    }

    private void toTreeString(StringBuilder sb, int ident) {
        for (int i = 0; i < ident; i++) sb.append(' ');
        sb.append(kind);
        if (token != null) {
            sb.append(" [")
              .append(token.getTipo()).append(" ")
              .append("'").append(token.getLexema()).append("' ")
              .append("@").append(token.getLinha()).append(":").append(token.getColuna())
              .append("]");
        }
        sb.append("\n");
        for (AstNode f : filhos) {
            f.toTreeString(sb, ident + 2);
        }
    }
}
