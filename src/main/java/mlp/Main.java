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
import mlp.tac.GeradorTAC;
import mlp.tac.InterpretadorTAC;
import mlp.tac.TacInstr;
import mlp.relato.NarratedPrinter;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Uso:");
            System.err.println("  java -jar compilador-mlp.jar <arquivo.mlp>");
            System.err.println("  java -jar compilador-mlp.jar --run-examples   (roda todos em ./examples)");
            System.exit(1);
        }

        if ("--run-examples".equals(args[0])) {
            Path base = Paths.get(args.length > 1 ? args[1] : "examples");

            if (!Files.exists(base)) {
                System.err.println("[run-examples] Diretório não existe: " + base.toAbsolutePath());
                System.exit(1);
            }

            System.out.println("[run-examples] Varredo recursivamente: " + base.toAbsolutePath());

            List<Path> arquivos;
            try (var stream = Files.walk(base)) {
                arquivos = stream
                    .filter(f -> f.toString().endsWith(".mlp"))
                    .sorted()
                    .toList();
            }

            if (arquivos.isEmpty()) {
                System.out.println("[run-examples] Nenhum arquivo .mlp encontrado.");
                System.exit(0);
            }

            System.out.println("[run-examples] Encontrados " + arquivos.size() + " arquivos .mlp");
            boolean anyErrors = false;

            for (Path p : arquivos) {
                try {
                    boolean hadErrors = processarArquivo(p);
                    anyErrors |= hadErrors;
                } catch (Exception e) {
                    e.printStackTrace();
                    anyErrors = true;
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

        // 3) SEMÂNTICO
        AnalisadorSemantico sem = new AnalisadorSemantico();
        sem.analisar(programa);
        List<Diagnostico> diagsSem = sem.getDiagnosticos();

        NarratedPrinter np = new NarratedPrinter(System.out);

        // -------- RELATÓRIOS --------

        // TOKENS: apenas versão comentada
        np.printTokensWithNarration(tokens);

        // PALAVRAS-RESERVADAS
        System.out.println(">>> PALAVRAS-RESERVADAS ENCONTRADAS");
        for (Token t : tokens) {
            if (isPalavraReservada(t.getTipo())) {
                System.out.printf("  %-12s %-12s @%d:%d\n",
                    t.getTipo(), quote(t.getLexema()), t.getLinha(), t.getColuna());
            }
        }

        // AST: apenas versão comentada
        np.printAstWithNarration(programa);

        // TABELA DE SÍMBOLOS
        System.out.println(">>> TABELA DE SIMBOLOS");
        if (sem.getTabela() != null && sem.getTabela().todas() != null && !sem.getTabela().todas().isEmpty()) {
            for (var e : sem.getTabela().todas().values()) {
                System.out.printf("  %-12s : %-7s @%d:%d\n", e.nome, e.tipo, e.linha, e.coluna);
            }
        } else {
            System.out.println("  (nao gerada devido a erros lexico/sintaticos ou tabela vazia)");
        }

        // DIAGNÓSTICOS
        List<Diagnostico> all = new ArrayList<>();
        all.addAll(diagsLexColeta);
        all.addAll(diagsLex);
        all.addAll(diagsSint);
        all.addAll(diagsSem);

        // remover duplicados (mesmo texto de diagnóstico)
        List<Diagnostico> allUnique = new ArrayList<>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (Diagnostico d : all) {
            String key = d.toString();
            if (seen.add(key)) {
                allUnique.add(d);
            }
        }

        System.out.println(">>> DIAGNOSTICOS");
        if (allUnique.isEmpty()) {
            System.out.println("  (nenhum)");
        } else {
            for (Diagnostico d : allUnique) {
                System.out.println("  " + d);
            }
        }

        int cLex = 0, cSin = 0, cSem = 0;
        for (Diagnostico d : allUnique) {
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

        // --- GERAÇÃO DE CÓDIGO INTERMEDIÁRIO (TAC) ---
        if (total == 0) {
            GeradorTAC gerador = new GeradorTAC();
            List<TacInstr> tac = gerador.gerar(programa);

            // Imprime TAC comentado
            np.printTacWithNarration(tac);

            // Executa o código TAC
            System.out.println("SAIDA DO PROGRAMA");
            try {
            InterpretadorTAC interpretador = new InterpretadorTAC();
            interpretador.executar(tac);
            } catch (Exception e) {
                System.err.println("deu erro aqui:");
                e.printStackTrace();
            }
        }

        return total > 0;
    }

    // -------- utilidades --------

    private static boolean isPalavraReservada(TokenTipo tp) {
        return switch (tp) {
            case KW_SE, KW_ENTAO, KW_SENAO, KW_ENQUANTO, KW_ESCREVA,
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
