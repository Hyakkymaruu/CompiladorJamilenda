package mlp;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import mlp.Lexico.AnalisadorLexico;
import mlp.Lexico.Token;
import mlp.Lexico.TokenTipo;
import mlp.Erros.Diagnostico;
import mlp.Sintatico.AnalisadorSintatico;
import mlp.Semantico.AnalisadorSemantico;
import mlp.ast.AstNode;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Uso:");
            System.err.println("  java -jar compilador-mlp.jar <arquivo.mlp>");
            System.err.println("  java -jar compilador-mlp.jar --run-examples   (roda todos em ./examples)");
            System.exit(1);
        }

        if ("--run-examples".equals(args[0])) {
            Path dir = Paths.get("examples");
            boolean anyErrors = false;
            try (var stream = Files.list(dir)) {
                for (Path p : (Iterable<Path>) stream.filter(f -> f.toString().endsWith(".mlp")).sorted()::iterator) {
                    try {
                        boolean hadErrors = processarArquivo(p);
                        anyErrors |= hadErrors;
                    } catch (Exception e) {
                        e.printStackTrace();
                        anyErrors = true;
                    }
                }
            }
            System.exit(anyErrors ? 2 : 0);
        }

        boolean hadErrors = processarArquivo(Paths.get(args[0]));
        System.exit(hadErrors ? 2 : 0);
    }

    /** Processa um arquivo .mlp e retorna true se houve qualquer diagnóstico. */
    private static boolean processarArquivo(Path path) throws Exception {
        System.out.println("\n==================================================");
        System.out.println("ARQUIVO: " + path);
        System.out.println("==================================================");

        String source = Files.readString(path, StandardCharsets.UTF_8);

        // 1) LÉXICO (coleta de tokens para relatório)
        AnalisadorLexico lxTokens = new AnalisadorLexico(source);
        List<Token> tokens = new ArrayList<>();
        while (true) {
            Token t = lxTokens.proximo();
            tokens.add(t);
            if (t.getTipo() == TokenTipo.EOF) break;
        }
        List<Diagnostico> diagsLexColeta = lxTokens.getDiagnosticos();

        // 2) SINTÁTICO (novo léxico para o parser)
        AnalisadorLexico lx = new AnalisadorLexico(source);
        AnalisadorSintatico ps = new AnalisadorSintatico(lx);
        AstNode programa = ps.parsePrograma();
        List<Diagnostico> diagsLex = lx.getDiagnosticos();    // do parsing real
        List<Diagnostico> diagsSint = ps.getDiagnosticos();

        // 3) SEMÂNTICO (se já existir implementação)
        AnalisadorSemantico sem = new AnalisadorSemantico();
        sem.analisar(programa);
        List<Diagnostico> diagsSem = sem.getDiagnosticos();

        // -------- RELATÓRIOS --------

        // TOKENS
        System.out.println(">>> TOKENS (ordem de leitura)");
        for (Token t : tokens) {
            System.out.printf("  %-12s %-12s @%d:%d\n",
                t.getTipo(), quote(t.getLexema()), t.getLinha(), t.getColuna());
        }

        // PALAVRAS-RESERVADAS
        System.out.println(">>> PALAVRAS-RESERVADAS ENCONTRADAS");
        for (Token t : tokens) {
            if (isPalavraReservada(t.getTipo())) {
                System.out.printf("  %-12s %-12s @%d:%d\n",
                    t.getTipo(), quote(t.getLexema()), t.getLinha(), t.getColuna());
            }
        }

        // AST
        System.out.println(">>> AST");
        System.out.print(programa.toTreeString());

        // TABELA DE SÍMBOLOS
        System.out.println(">>> TABELA DE SIMBOLOS");
        if (sem.getTabela() != null && sem.getTabela().todas() != null && !sem.getTabela().todas().isEmpty()) {
            for (var e : sem.getTabela().todas().values()) {
                System.out.printf("  %-12s : %-7s @%d:%d\n", e.nome, e.tipo, e.linha, e.coluna);
            }
        } else {
            System.out.println("  (vazia ou não construída nesta etapa)");
        }

        // DIAGNÓSTICOS (léxico da coleta + léxico do parsing + sintático + semântico)
        List<Diagnostico> all = new ArrayList<>();
        all.addAll(diagsLexColeta);
        all.addAll(diagsLex);
        all.addAll(diagsSint);
        all.addAll(diagsSem);

        System.out.println(">>> DIAGNOSTICOS");
        if (all.isEmpty()) {
            System.out.println("  (nenhum)");
        } else {
            for (Diagnostico d : all) System.out.println("  " + d);
        }

        // RESUMO + retorno (exit code será tratado no main)
        int cLex = 0, cSin = 0, cSem = 0;
        for (Diagnostico d : all) {
            switch (d.getTipo()) {
                case LEXICO -> cLex++;
                case SINTATICO -> cSin++;
                case SEMANTICO -> cSem++;
            }
        }
        int total = cLex + cSin + cSem;

        System.out.println(">>> RESUMO");
        System.out.printf("  LEXICO: %d  SINTATICO: %d  SEMANTICO: %d  TOTAL: %d\n",
                cLex, cSin, cSem, total);

        return total > 0;
    }

    // -------- utilidades leves (não criamos novas classes) --------

    private static boolean isPalavraReservada(TokenTipo tp) {
        return switch (tp) {
            case KW_SE, KW_ENTAO, KW_SENAO, KW_ENQUANTO,
                 KW_INTEIRO, KW_REAL, KW_CARACTER,
                 KW_E, KW_OU, KW_NAO -> true;
            default -> false;
        };
    }

    private static String quote(String s) {
        if (s == null) return "null";
        return "'" + s.replace("\n","\\n").replace("\r","\\r").replace("\t","\\t") + "'";
    }
}
