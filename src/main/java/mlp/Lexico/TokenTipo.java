package mlp.Lexico;

public enum TokenTipo {
    // Delimitadores de programa
    START,          // $
    END,            // $.

    // Palavras-reservadas (minúsculas)
    KW_SE,          // se
    KW_ENTAO,       // entao
    KW_SENAO,       // senao
    KW_ENQUANTO,    // enquanto
    KW_ESCREVA,     // escreva
    KW_INTEIRO,     // inteiro
    KW_REAL,        // real
    KW_CARACTER,    // caracter

    // Conectivos lógicos (maiúsculas)
    KW_E,           // E
    KW_OU,          // OU
    KW_NAO,         // NAO

    // Operadores relacionais
    OP_EQ,          // ==
    OP_NE,          // !=
    OP_LE,          // <=
    OP_GE,          // >=
    OP_LT,          // <
    OP_GT,          // >

    // Operadores aritméticos
    OP_MAIS,        // +
    OP_MULT,        // *
    OP_DIV,         // /
    OP_RESTO,       // RESTO

    // Atribuição
    OP_ATRIB,       // =

    // Separadores
    ABRE_PAR,       // (
    FECHA_PAR,      // )
    VIRGULA,        // ,
    PONTO_VIRG,     // ;

    // Léxicos gerais
    IDENT,
    NUM_INT,
    NUM_REAL,

    // Controle
    EOF,
    INVALIDO
}
