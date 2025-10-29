# FIRST por não-terminal

FIRST(<programa>)      = { START }
FIRST(<inicio>)        = { START }
FIRST(<fim>)           = { END }

FIRST(<lista-declar>)  = { KW_INTEIRO, KW_REAL, KW_CARACTER, ε }
FIRST(<declar>)        = { KW_INTEIRO, KW_REAL, KW_CARACTER }
FIRST(<tipo>)          = { KW_INTEIRO, KW_REAL, KW_CARACTER }
FIRST(<lista-ident>)   = { IDENT }
FIRST(<lista-ident'>)  = { VIRGULA, ε }

FIRST(<lista-comando>) = { IDENT, KW_SE, KW_ENQUANTO, ε }
FIRST(<comando>)       = { IDENT, KW_SE, KW_ENQUANTO }
FIRST(<cmd-atr>)       = { IDENT }
FIRST(<cmd-se>)        = { KW_SE }
FIRST(<cmd-se'>)       = { KW_SENAO, PONTO_VIRG }
FIRST(<cmd-enquanto>)  = { KW_ENQUANTO }

FIRST(<expressao>)     = { IDENT, NUM_INT, NUM_REAL, ABRE_PAR }
FIRST(<expressao'>)    = { OP_MAIS, ε }
FIRST(<termo>)         = { IDENT, NUM_INT, NUM_REAL, ABRE_PAR }
FIRST(<termo'>)        = { OP_MULT, OP_DIV, OP_RESTO, ε }
FIRST(<fator>)         = { IDENT, NUM_INT, NUM_REAL, ABRE_PAR }

FIRST(<condicao>)      = { KW_NAO, IDENT, NUM_INT, NUM_REAL, ABRE_PAR }
FIRST(<disjuncao>)     = { KW_NAO, IDENT, NUM_INT, NUM_REAL, ABRE_PAR }
FIRST(<disjuncao'>)    = { KW_OU, ε }
FIRST(<conjuncao>)     = { KW_NAO, IDENT, NUM_INT, NUM_REAL, ABRE_PAR }
FIRST(<conjuncao'>)    = { KW_E, ε }
FIRST(<negacao>)       = { KW_NAO, ABRE_PAR, IDENT, NUM_INT, NUM_REAL }

FIRST(<relacao>)       = { ABRE_PAR, IDENT, NUM_INT, NUM_REAL }
FIRST(<expr-rel>)      = { ABRE_PAR, IDENT, NUM_INT, NUM_REAL }
FIRST(<opnd-rel>)      = { ABRE_PAR, IDENT, NUM_INT, NUM_REAL }
FIRST(<op-rel>)        = { OP_EQ, OP_NE, OP_LE, OP_GE, OP_LT, OP_GT }
