# Catálogo de Erros — MLP

Padronização de códigos:
- **Léxico:** `01xx`
- **Sintático:** `10xx`
- **Semântico:** `20xx`

> Cada diagnóstico informa: **[TIPO] [linha:coluna] COD.#### — mensagem (lexema='...')**

## Léxico (01xx)
| Código | Mensagem (exemplo) | Observação |
|-------:|---------------------|------------|
| 0101 | símbolo não reconhecido | caractere fora do alfabeto |
| 0102 | número real inválido | formatos não aceitos (ex.: `5.` ou `.5`) |
| 0103 | identificador malformado | se regras forem expandidas no futuro |

## Sintático (10xx)
| Código | Mensagem (exemplo) | Onde usamos |
|-------:|---------------------|-------------|
| 1001 | comando inválido | `parseComando` (fallback) |
| 1002 | esperava início de programa '$' | `parsePrograma` |
| 1003 | esperava fim de programa '$.' | `parsePrograma` |
| 1004 | esperava tipo ('inteiro', 'real' ou 'caracter') | `parseDeclar` |
| 1005 | esperava identificador na declaração/atribuição | `parseDeclar`/`parseCmdAtr` |
| 1006 | esperava ';' ao final da declaração | `parseDeclar` |
| 1007 | esperava identificador após ',' | `parseDeclar` |
| 1010 | esperava operador relacional | `parseRelacao` |
| 1011 | operando relacional inválido | (não deve mais ocorrer com `expressao opRel expressao`, mas reservado) |
| 1012 | esperava ')' após expressão | `parseFator`/operandos |
| 1013 | esperava ')' após condição | `parseCmdSe`/`parseCmdEnquanto` |
| 1014 | esperava 'entao' | `parseCmdSe` |
| 1015 | esperava '=' na atribuição | `parseCmdAtr` |
| 1016 | fator inválido em expressão | `parseFator` |
| 1017 | esperava ';' ao final da atribuição | `parseCmdAtr` |

## Semântico (20xx)
| Código | Mensagem (exemplo) | Observação |
|-------:|---------------------|------------|
| 2001 | variável não declarada | uso antes de declarar |
| 2002 | variável já declarada | redefinição em mesmo escopo |
| 2003 | incompatibilidade de tipos | ex.: `real` em operador `RESTO` |
| 2004 | expressão condicional não booleana | se tiparmos `condicao` futuramente |

> Observação: começamos padronizando **Sintático** (já em uso). Léxico/Semântico entram na sequência do Item 3.
