# Suites de testes MLP

- `positivos/`: 4 programas válidos exigidos (cobrem declarações, atribuições, condição e laço).
- `negativos/lexico`: falhas 0101–0103 (símbolo inválido, real malformado, identificador malformado).
- `negativos/sintatico`: falhas 1001, 1010–1017 (relacional ausente, ')' ausente, ';' ausente, etc.).
- `negativos/semantico`: falhas 2001–2004 (não declarado, redeclaração, tipos incompatíveis, condição não booleana).
- `extras/`: casos exploratórios usados durante a depuração.

Execução:
```bash
mvn -q -DskipTests package
java -jar target/compilador-mlp-0.1.0.jar --run-examples
java -jar target/compilador-mlp-0.1.0.jar --run-examples examples/negativos/sintatico
