# Tabela Preditiva LL(1) — entradas não vazias

## Bloco Programa
M[<programa>, START] = <inicio> <lista-declar> <lista-comando> <fim>

M[<inicio>, START] = START
M[<fim>, END]      = END

## Declarações
M[<lista-declar>, KW_INTEIRO]  = <declar> <lista-declar>
M[<lista-declar>, KW_REAL]     = <declar> <lista-declar>
M[<lista-declar>, KW_CARACTER] = <declar> <lista-declar>
M[<lista-declar>, IDENT]       = ε
M[<lista-declar>, KW_SE]       = ε
M[<lista-declar>, KW_ENQUANTO] = ε
M[<lista-declar>, END]         = ε

M[<declar>, KW_INTEIRO]  = <tipo> <lista-ident> PONTO_VIRG
M[<declar>, KW_REAL]     = <tipo> <lista-ident> PONTO_VIRG
M[<declar>, KW_CARACTER] = <tipo> <lista-ident> PONTO_VIRG

M[<tipo>, KW_INTEIRO]  = KW_INTEIRO
M[<tipo>, KW_REAL]     = KW_REAL
M[<tipo>, KW_CARACTER] = KW_CARACTER

M[<lista-ident>, IDENT] = IDENT <lista-ident'>

M[<lista-ident'>, VIRGULA] = VIRGULA IDENT <lista-ident'>
M[<lista-ident'>, PONTO_VIRG] = ε

## Comandos
M[<lista-comando>, IDENT]       = <comando> <lista-comando>
M[<lista-comando>, KW_SE]       = <comando> <lista-comando>
M[<lista-comando>, KW_ENQUANTO] = <comando> <lista-comando>
M[<lista-comando>, END]         = ε

M[<comando>, IDENT]       = <cmd-atr>
M[<comando>, KW_SE]       = <cmd-se>
M[<comando>, KW_ENQUANTO] = <cmd-enquanto>

M[<cmd-atr>, IDENT] = IDENT OP_ATRIB <expressao> PONTO_VIRG

M[<cmd-se>, KW_SE] = KW_SE ABRE_PAR <condicao> FECHA_PAR KW_ENTAO <comando> <cmd-se'>

M[<cmd-se'>, KW_SENAO]   = KW_SENAO <comando> PONTO_VIRG
M[<cmd-se'>, PONTO_VIRG] = PONTO_VIRG

M[<cmd-enquanto>, KW_ENQUANTO] =
  KW_ENQUANTO ABRE_PAR <condicao> FECHA_PAR <comando> PONTO_VIRG

## Expressões numéricas
M[<expressao>, IDENT]   = <termo> <expressao'>
M[<expressao>, NUM_INT] = <termo> <expressao'>
M[<expressao>, NUM_REAL]= <termo> <expressao'>
M[<expressao>, ABRE_PAR]= <termo> <expressao'>

M[<expressao'>, OP_MAIS] = OP_MAIS <termo> <expressao'>
M[<expressao'>, FECHA_PAR] = ε
M[<expressao'>, PONTO_VIRG] = ε
M[<expressao'>, VIRGULA]    = ε
M[<expressao'>, OP_EQ] = ε
M[<expressao'>, OP_NE] = ε
M[<expressao'>, OP_LE] = ε
M[<expressao'>, OP_GE] = ε
M[<expressao'>, OP_LT] = ε
M[<expressao'>, OP_GT] = ε

M[<termo>, IDENT]    = <fator> <termo'>
M[<termo>, NUM_INT]  = <fator> <termo'>
M[<termo>, NUM_REAL] = <fator> <termo'>
M[<termo>, ABRE_PAR] = <fator> <termo'>

M[<termo'>, OP_MULT]  = OP_MULT  <fator> <termo'>
M[<termo'>, OP_DIV]   = OP_DIV   <fator> <termo'>
M[<termo'>, OP_RESTO] = OP_RESTO <fator> <termo'>
M[<termo'>, OP_MAIS]      = ε
M[<termo'>, FECHA_PAR]    = ε
M[<termo'>, PONTO_VIRG]   = ε
M[<termo'>, VIRGULA]      = ε
M[<termo'>, OP_EQ] = ε
M[<termo'>, OP_NE] = ε
M[<termo'>, OP_LE] = ε
M[<termo'>, OP_GE] = ε
M[<termo'>, OP_LT] = ε
M[<termo'>, OP_GT] = ε

M[<fator>, IDENT]    = IDENT
M[<fator>, NUM_INT]  = NUM_INT
M[<fator>, NUM_REAL] = NUM_REAL
M[<fator>, ABRE_PAR] = ABRE_PAR <expressao> FECHA_PAR

## Condições (lógica/relacional)
M[<condicao>, KW_NAO]   = <disjuncao>
M[<condicao>, IDENT]    = <disjuncao>
M[<condicao>, NUM_INT]  = <disjuncao>
M[<condicao>, NUM_REAL] = <disjuncao>
M[<condicao>, ABRE_PAR] = <disjuncao>

M[<disjuncao>, KW_NAO]   = <conjuncao> <disjuncao'>
M[<disjuncao>, IDENT]    = <conjuncao> <disjuncao'>
M[<disjuncao>, NUM_INT]  = <conjuncao> <disjuncao'>
M[<disjuncao>, NUM_REAL] = <conjuncao> <disjuncao'>
M[<disjuncao>, ABRE_PAR] = <conjuncao> <disjuncao'>

M[<disjuncao'>, KW_OU]     = KW_OU <conjuncao> <disjuncao'>
M[<disjuncao'>, FECHA_PAR] = ε

M[<conjuncao>, KW_NAO]   = <negacao> <conjuncao'>
M[<conjuncao>, IDENT]    = <negacao> <conjuncao'>
M[<conjuncao>, NUM_INT]  = <negacao> <conjuncao'>
M[<conjuncao>, NUM_REAL] = <negacao> <conjuncao'>
M[<conjuncao>, ABRE_PAR] = <negacao> <conjuncao'>

M[<conjuncao'>, KW_E]       = KW_E <negacao> <conjuncao'>
M[<conjuncao'>, KW_OU]      = ε
M[<conjuncao'>, FECHA_PAR]  = ε

M[<negacao>, KW_NAO]   = KW_NAO <negacao>
M[<negacao>, ABRE_PAR] = <relacao>
M[<negacao>, IDENT]    = <relacao>
M[<negacao>, NUM_INT]  = <relacao>
M[<negacao>, NUM_REAL] = <relacao>

M[<relacao>, ABRE_PAR] = ABRE_PAR <relacao> FECHA_PAR
M[<relacao>, IDENT]    = <expr-rel>
M[<relacao>, NUM_INT]  = <expr-rel>
M[<relacao>, NUM_REAL] = <expr-rel>

M[<expr-rel>, ABRE_PAR] = <opnd-rel> <op-rel> <opnd-rel>
M[<expr-rel>, IDENT]    = <opnd-rel> <op-rel> <opnd-rel>
M[<expr-rel>, NUM_INT]  = <opnd-rel> <op-rel> <opnd-rel>
M[<expr-rel>, NUM_REAL] = <opnd-rel> <op-rel> <opnd-rel>

M[<opnd-rel>, ABRE_PAR] = ABRE_PAR <expressao> FECHA_PAR
M[<opnd-rel>, IDENT]    = IDENT
M[<opnd-rel>, NUM_INT]  = NUM_INT
M[<opnd-rel>, NUM_REAL] = NUM_REAL

M[<op-rel>, OP_EQ] = OP_EQ
M[<op-rel>, OP_NE] = OP_NE
M[<op-rel>, OP_LE] = OP_LE
M[<op-rel>, OP_GE] = OP_GE
M[<op-rel>, OP_LT] = OP_LT
M[<op-rel>, OP_GT] = OP_GT
