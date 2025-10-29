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
            try (var stream = Files.list(dir)) {
                stream.filter(p -> p.toString().endsWith(".mlp"))
                      .sorted()
                      .forEach(p -> {
                          try { processarArquivo(p); }
                          catch (Exception e) { e.printStackTrace(); }
                      });
            }
            return;
        }

        processarArquivo(Paths.get(args[0]));
    }

    private static void processarArquivo(Path path) throws Exception {
        System.out.println("\n==================================================");
        System.out.println("ARQUIVO: " + path);
        System.out.println("==================================================");

        String source = Files.readString(path, StandardCharsets.UTF_8);

        // 1) LÉXICO (coleta de tokens para relatório)
        AnalisadorLexico lxTokens = new AnalisadorLexico(source);
        List<Token> tokens = lxTokens.listarTodos();
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
        for (var e : sem.getTabela().todas().values()) {
            System.out.printf("  %-12s : %-5s  @%d:%d\n", e.nome, e.tipo, e.linha, e.coluna);
        }

        // DIAGNÓSTICOS
        List<Diagnostico> all = new ArrayList<>();
        all.addAll(diagsLexColeta);  // do primeiro léxico (coleta de tokens)
        all.addAll(diagsLex);        // do léxico usado no parser (deve ser igual; mantido por segurança)
        all.addAll(diagsSint);
        all.addAll(diagsSem);

        if (all.isEmpty()) {
            System.out.println(">>> DIAGNOSTICOS");
            System.out.println("  (nenhum)");
        } else {
            System.out.println(">>> DIAGNOSTICOS");
            for (Diagnostico d : all) System.out.println("  " + d);
        }
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
