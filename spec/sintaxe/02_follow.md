# FOLLOW por não-terminal  ( '$' = EOF )

FOLLOW(<programa>)      = { $ }
FOLLOW(<inicio>)        = FIRST(<lista-declar>) ∪ FIRST(<lista-comando>) ∪ { END }
                        = { KW_INTEIRO, KW_REAL, KW_CARACTER, IDENT, KW_SE, KW_ENQUANTO, END }

FOLLOW(<lista-declar>)  = FIRST(<lista-comando>) ∪ { END }
                        = { IDENT, KW_SE, KW_ENQUANTO, END }

FOLLOW(<declar>)        = FIRST(<lista-declar>) ∪ FIRST(<lista-comando>) ∪ { END }
                        = { KW_INTEIRO, KW_REAL, KW_CARACTER, IDENT, KW_SE, KW_ENQUANTO, END }

FOLLOW(<tipo>)          = { IDENT }

FOLLOW(<lista-ident>)   = { PONTO_VIRG }
FOLLOW(<lista-ident'>)  = { PONTO_VIRG }

FOLLOW(<lista-comando>) = { END }
FOLLOW(<comando>)       = FIRST(<lista-comando>) ∪ { END }
                        = { IDENT, KW_SE, KW_ENQUANTO, END }

FOLLOW(<cmd-atr>)       = FOLLOW(<comando>) = { IDENT, KW_SE, KW_ENQUANTO, END }
FOLLOW(<cmd-se>)        = FOLLOW(<comando>) = { IDENT, KW_SE, KW_ENQUANTO, END }
FOLLOW(<cmd-se'>)       = FOLLOW(<cmd-se>)  = { IDENT, KW_SE, KW_ENQUANTO, END }
FOLLOW(<cmd-enquanto>)  = FOLLOW(<comando>) = { IDENT, KW_SE, KW_ENQUANTO, END }

FOLLOW(<expressao>)     = { FECHA_PAR, PONTO_VIRG, VIRGULA, OP_EQ, OP_NE, OP_LE, OP_GE, OP_LT, OP_GT }
FOLLOW(<expressao'>)    = FOLLOW(<expressao>)
FOLLOW(<termo>)         = FIRST(<expressao'>)∖{ε} ∪ FOLLOW(<expressao>) = { OP_MAIS } ∪ FOLLOW(<expressao>)
FOLLOW(<termo'>)        = FOLLOW(<termo>)
FOLLOW(<fator>)         = FIRST(<termo'>)∖{ε} ∪ FOLLOW(<termo>) = { OP_MULT, OP_DIV, OP_RESTO } ∪ FOLLOW(<termo>)

FOLLOW(<condicao>)      = { FECHA_PAR }
FOLLOW(<disjuncao>)     = FOLLOW(<condicao>) ∪ FIRST(<disjuncao'>)∖{ε}
                        = { FECHA_PAR, KW_OU }
FOLLOW(<disjuncao'>)    = FOLLOW(<disjuncao>) = { FECHA_PAR }
FOLLOW(<conjuncao>)     = FIRST(<disjuncao'>)∖{ε} ∪ FOLLOW(<disjuncao>)
                        = { KW_OU, FECHA_PAR }
FOLLOW(<conjuncao'>)    = FOLLOW(<conjuncao>) = { KW_OU, FECHA_PAR }
FOLLOW(<negacao>)       = FIRST(<conjuncao'>)∖{ε} ∪ FOLLOW(<conjuncao>)
                        = { KW_E, KW_OU, FECHA_PAR }

FOLLOW(<relacao>)       = FOLLOW(<negacao>) = { KW_E, KW_OU, FECHA_PAR }
FOLLOW(<expr-rel>)      = FOLLOW(<relacao>) = { KW_E, KW_OU, FECHA_PAR }
FOLLOW(<opnd-rel>)      = FIRST(<op-rel>)∖{ε} ∪ FOLLOW(<expr-rel>)
                        = { OP_EQ, OP_NE, OP_LE, OP_GE, OP_LT, OP_GT, KW_E, KW_OU, FECHA_PAR }
FOLLOW(<op-rel>)        = FIRST(<opnd-rel>)∖{ε}
                        = { ABRE_PAR, IDENT, NUM_INT, NUM_REAL }
