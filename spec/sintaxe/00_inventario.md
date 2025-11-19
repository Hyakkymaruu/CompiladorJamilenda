# Inventário sintático (LL(1))

**Símbolo inicial:** `<programa>`

## Terminais (tokens do léxico)
START, END, ABRE_PAR, FECHA_PAR, VIRGULA, PONTO_VIRG, OP_ATRIB,
KW_SE, KW_ENTAO, KW_SENAO, KW_ENQUANTO,
KW_INTEIRO, KW_REAL, KW_CARACTER,
KW_E, KW_OU, KW_NAO,
OP_EQ, OP_NE, OP_LE, OP_GE, OP_LT, OP_GT,
OP_MAIS, OP_MULT, OP_DIV, OP_RESTO,
IDENT, NUM_INT, NUM_REAL, EOF

## Não-terminais
<programa>, <inicio>, <fim>,
<lista-declar>, <declar>, <tipo>, <lista-ident>, <lista-ident'>,
<lista-comando>, <comando>, <cmd-atr>, <cmd-se>, <cmd-se'>, <cmd-enquanto>,
<expressao>, <expressao'>, <termo>, <termo'>, <fator>,
<condicao>, <disjuncao>, <disjuncao'>, <conjuncao>, <conjuncao'>, <negacao>,
<relacao>, <expr-rel>, <opnd-rel>, <op-rel>

## Gramática (a mesma já acordada)
<programa>      ::= <inicio> <lista-declar> <lista-comando> <fim>
<inicio>        ::= START
<fim>           ::= END

<lista-declar>  ::= <declar> <lista-declar> | ε
<declar>        ::= <tipo> <lista-ident> PONTO_VIRG
<tipo>          ::= KW_INTEIRO | KW_REAL | KW_CARACTER
<lista-ident>   ::= IDENT <lista-ident'>
<lista-ident'>  ::= VIRGULA IDENT <lista-ident'> | ε

<lista-comando> ::= <comando> <lista-comando> | ε
<comando>       ::= <cmd-atr> | <cmd-se> | <cmd-enquanto>
<cmd-atr>       ::= IDENT OP_ATRIB <expressao> PONTO_VIRG

<cmd-se>        ::= KW_SE ABRE_PAR <condicao> FECHA_PAR KW_ENTAO <comando> <cmd-se'>
<cmd-se'>       ::= KW_SENAO <comando> PONTO_VIRG | PONTO_VIRG

<cmd-enquanto>  ::= KW_ENQUANTO ABRE_PAR <condicao> FECHA_PAR <comando> PONTO_VIRG

<expressao>     ::= <termo> <expressao'>
<expressao'>    ::= OP_MAIS <termo> <expressao'> | ε
<termo>         ::= <fator> <termo'>
<termo'>        ::= (OP_MULT | OP_DIV | OP_RESTO) <fator> <termo'> | ε
<fator>         ::= IDENT | NUM_INT | NUM_REAL | ABRE_PAR <expressao> FECHA_PAR

<condicao>      ::= <disjuncao>
<disjuncao>     ::= <conjuncao> <disjuncao'>
<disjuncao'>    ::= KW_OU <conjuncao> <disjuncao'> | ε
<conjuncao>     ::= <negacao> <conjuncao'>
<conjuncao'>    ::= KW_E <negacao> <conjuncao'> | ε
<negacao>       ::= KW_NAO <negacao> | <relacao>

<relacao>       ::= ABRE_PAR <relacao> FECHA_PAR | <expr-rel>
<expr-rel>      ::= <opnd-rel> <op-rel> <opnd-rel>
<opnd-rel>      ::= IDENT | NUM_INT | NUM_REAL | ABRE_PAR <expressao> FECHA_PAR
<op-rel>        ::= OP_EQ | OP_NE | OP_LE | OP_GE | OP_LT | OP_GT
