package mlp.Simbolos;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tabela de símbolos global (escopo único). */
public class TabelaSimbolos {

    public enum TipoSimples { INT, REAL, CHAR, BOOL, ERRO }

    public static class Entrada {
        public final String nome;
        public final TipoSimples tipo;
        public final int linha;
        public final int coluna;

        public Entrada(String nome, TipoSimples tipo, int linha, int coluna) {
            this.nome = nome;
            this.tipo = tipo;
            this.linha = linha;
            this.coluna = coluna;
        }
    }

    private final Map<String, Entrada> mapa = new LinkedHashMap<>();

    /** Declara um símbolo. Retorna false se já existir. */
    public boolean declarar(String nome, TipoSimples tipo, int linha, int coluna) {
        if (mapa.containsKey(nome)) return false;
        mapa.put(nome, new Entrada(nome, tipo, linha, coluna));
        return true;
    }

    public boolean existe(String nome) {
        return mapa.containsKey(nome);
    }

    public Entrada obter(String nome) {
        return mapa.get(nome);
    }

    public Map<String, Entrada> todas() {
        return mapa;
    }
}
