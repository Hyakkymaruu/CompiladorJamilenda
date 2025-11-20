.DEFAULT_GOAL := help

# Maven usa <finalName> compilador-mlp (sem o sufixo de versao)
JAR  := target/compilador-mlp.jar
DIR  ?= examples
FILE ?= examples/testando.mlp

.PHONY: help build run examples clean

help:
	@echo "Targets:"
	@echo "  build     - gera o jar em target/"
	@echo "  run       - compila e roda um arquivo .mlp (use FILE=caminho)"
	@echo "  examples  - compila e roda --run-examples (use DIR=para customizar, padrao: examples)"
	@echo "  clean     - limpa artefatos do Maven"

build:
	mvn -q -DskipTests package

run: build
	@echo "Rodando $(FILE)"
	java -jar $(JAR) $(FILE)

examples: build
	java -jar $(JAR) --run-examples $(DIR)

clean:
	mvn clean
