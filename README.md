# Simulador de Gerenciamento de Memória — TP2 de Sistemas Operacionais

Ferramenta de linha de comando que lê um arquivo de requisições e simula, linha a linha, o comportamento da memória principal em três técnicas de gerenciamento:

| Modo | Técnica | Políticas / parâmetros |
|---|---|---|
| 1 | Partições variáveis | Worst-Fit ou Circular-Fit (Next-Fit) |
| 2 | Sistema buddy | — |
| 3 | Paginação simples (sem memória virtual) | tamanho da página |

Trabalho Prático 2 da disciplina de Sistemas Operacionais — PUCRS.

**Integrantes:** Jayme Fortes, Rodrigo Gagliardi e Lucca Marques

---

## Requisitos

- **JDK 17 ou mais novo** (o código é compilado com `--release 17`).
- Só a biblioteca padrão do Java: não precisa de Maven, Gradle nem dependências externas.

Para conferir a versão instalada:

```
javac -version
```

---

## Como compilar

Dentro da pasta do projeto (a que contém `src/` e `tests/`).

**Windows (cmd):**

```
rmdir /s /q out
javac -encoding UTF-8 --release 17 -d out src\memory\*.java src\memory\mode1\*.java src\memory\mode2\*.java src\memory\mode3\*.java
```

**Linux / macOS:**

```
rm -rf out
javac -encoding UTF-8 --release 17 -d out $(find src -name "*.java")
```

Se o `javac` não imprimir nada, a compilação deu certo. Apagar a pasta `out` antes evita rodar, sem perceber, uma versão antiga compilada.

---

## Como executar

```
java -cp out memory.Main [arquivo] [--mode 1|2|3] [--mem N] [--policy worst|circular] [--page N] [--step | --continuous]
```

Qualquer parâmetro que não for passado por argumento é **perguntado no terminal**. Rodando sem nada (`java -cp out memory.Main`), o programa pergunta tudo.

| Parâmetro | Regra |
|---|---|
| `arquivo` | caminho do arquivo de requisições |
| `--mode` | `1` (partições variáveis), `2` (buddy) ou `3` (paginação) |
| `--mem` | tamanho da memória; obrigatório; **potência de 2** |
| `--policy` | só no modo 1: `worst` ou `circular` |
| `--page` | só no modo 3: **potência de 2** e **≤ tamanho da memória** |
| `--step` / `--continuous` | passo a passo (espera Enter a cada linha) ou contínuo |

Valor inválido passado por argumento encerra o programa com uma mensagem de erro; valor inválido digitado no terminal faz o programa perguntar de novo.

### Exemplos (os exemplos do enunciado)

```
java -cp out memory.Main tests\example-1.txt --mode 1 --mem 32 --policy worst --continuous
java -cp out memory.Main tests\example-1.txt --mode 1 --mem 32 --policy circular --continuous
java -cp out memory.Main tests\example-3.txt --mode 2 --mem 64 --continuous
java -cp out memory.Main tests\example-4.txt --mode 3 --mem 64 --page 8 --step
```

> No Windows, se os acentos aparecerem quebrados no terminal, rode `chcp 65001` antes.

---

## Formato do arquivo de requisições

Um comando por linha (linhas em branco são ignoradas):

| Comando | Significado |
|---|---|
| `IN(ID, tamanho)` | aloca `tamanho` posições para o processo `ID` |
| `OUT(ID)` | libera toda a memória do processo `ID` |
| `ACC(ID, endereço)` | só no modo 3: acessa o endereço lógico do processo `ID`; nos modos 1 e 2 é ignorado com aviso |

O `ID` pode ser número ou letra (`IN(12, 8)` e `IN(A, 8)` funcionam). Linhas que não seguem esse formato aparecem como `LINHA INVÁLIDA (ignorada)` e a execução continua.

### Mensagens de erro

O comando é descartado e as demais linhas continuam sendo processadas:

| Situação | Mensagem |
|---|---|
| Não há espaço para o `IN` | `ESPAÇO INSUFICIENTE DE MEMÓRIA` |
| `IN` com ID que já está na memória | `ERRO: PROCESSO JÁ EXISTE` |
| `OUT` ou `ACC` com ID que não está na memória | `ERRO: PROCESSO NÃO ENCONTRADO` |
| `ACC` com endereço ≥ tamanho do processo | `ERRO: ENDEREÇO INVÁLIDO` |

Um processo rejeitado por falta de espaço não é admitido; um `OUT` posterior para ele resulta em `ERRO: PROCESSO NÃO ENCONTRADO`.

---

## Como ler a saída

Cada linha do arquivo gera um bloco: o comando, o resultado (`-> OK: ...` ou a mensagem de erro) e o estado da memória depois dele. Intervalos de endereço aparecem como `[início-fim]`, com os dois extremos incluídos.

**Modo 1 — partições variáveis**

```
[06] OUT(C)
  -> OK: C liberado de [12-17] (6)
  Memória: [0-7] livre(8) | [8-11] B(4) | [12-17] livre(6) | [18-22] D(5) | [23-31] livre(9)
  Livres:  8 | 6 | 9   (total 23, maior 9)
```

`Livres` lista os blocos livres contíguos, o total livre e o maior bloco. Quando o total é grande mas o maior bloco é pequeno, há fragmentação externa. No Circular-Fit aparece também a linha `Ponteiro:`.

**Modo 2 — buddy**

```
[03] IN(C,20)
  -> OK: C alocado em [32-63] (bloco 32, pedido 20)
  Memória: [0-7] A(5/8) | [8-15] livre(8) | [16-31] B(9/16) | [32-63] C(20/32)
  Livres:  8   (total 8)
  Frag. interna total: 22
```

Um bloco ocupado aparece como `ID(pedido/bloco)`. A fragmentação interna é a soma de `bloco − pedido`. No `OUT`, a mensagem mostra em qual bloco o espaço terminou depois das fusões com os buddies.

**Modo 3 — paginação**

```
[05] ACC(B,8)
  -> OK: B end. lógico 8 = página 1, desloc. 0 -> frame 4 -> físico 32
  Frames:  0:A 1:A 2:A 3:B 4:B 5:D 6:D 7:-
  Livres:  7   (1 frame)
  Tabela B: p0->f3  p1->f4
  Frag. interna total: 11
```

`Frames` mostra o dono de cada frame (`-` = livre). A tabela de páginas exibida é a do processo envolvido no comando. A fragmentação interna é a sobra da última página de cada processo.

**Resumo final**

Ao fim da execução aparece o número de processos admitidos e de rejeitados por falta de espaço. *(Ocupação máxima e fragmentação média estão em implementação — Etapa 5.)*

---

## Estrutura do código

```
src/memory/
  Main.java                     lê os parâmetros, roda o laço principal, imprime estado e resumo
  Parser.java                   transforma cada linha de texto em um Command (regex)
  Command.java                  comando já interpretado: tipo (IN/OUT/ACC) + id + valor
  Simulator.java                trata os erros comuns aos três modos e repassa a alocação ao modo
  MemoryManager.java            interface que os três modos implementam
  Result.java                   sucesso/erro + as mensagens exatas do enunciado
  mode1/VariablePartitions.java partições variáveis: Worst-Fit e Circular-Fit
  mode2/Buddy.java              sistema buddy (árvore binária de blocos)
  mode3/Paging.java             paginação simples (vetor de frames + tabelas de páginas)
tests/
  example-1.txt                 exemplo do enunciado, modo 1 (seção 5.2)
  example-3.txt                 exemplo do enunciado, modo 2 (seção 6.1)
  example-4.txt                 exemplo do enunciado, modo 3 (seção 7.1)
  invalid.txt                   linhas válidas e inválidas misturadas
  errors.txt                    ID repetido, OUT/ACC de inexistente, endereço inválido
  test.txt                      pedido maior que a memória (caso de borda)
```

**Como as peças se encaixam:**

```
linha do arquivo -> Parser -> Command -> Simulator -> MemoryManager (modo escolhido) -> Result -> impressão
```

O `Simulator` concentra as validações iguais para todos os modos (processo repetido, processo inexistente, endereço inválido). Cada modo implementa a interface `MemoryManager` e só cuida da alocação, da liberação e da exibição do seu estado. Por isso o `Main` não precisa saber qual modo está rodando (polimorfismo).

**Convenções:** código (classes, métodos, variáveis) em inglês; comentários, documentação e saída em português.

---

## Validação

Os três exemplos do enunciado foram usados como testes de regressão e a saída bate linha por linha com as tabelas do PDF:

- **Modo 1 (seção 5.2):** as duas colunas, Worst-Fit e Circular-Fit — o `IN(G,7)` é rejeitado no Worst-Fit (fragmentação externa) e aceito no Circular-Fit.
- **Modo 2 (seção 6.1):** fragmentação interna 3 → 10 → 22 → 24 → (E rejeitado) 24 → 21 → 19 → 12 → 0, com as coalescências nos pontos esperados.
- **Modo 3 (seção 7.1):** `ACC(B,8)` → endereço físico 32; `ACC(A,22)` → endereço inválido; a última alocação de `C` usa os frames 0, 1, 2 e 7 (não contíguos).

---

## Uso de inteligência artificial

Conforme a seção 12 do enunciado (Integridade acadêmica), declaramos que este trabalho contou com o apoio de ferramentas de IA — **Claude (Anthropic)**, incluindo o **Claude Code** — nas seguintes atividades:

- **Ajuda com o código:** sugestão e geração de partes da implementação (leitura dos parâmetros, parser, interface comum, e as classes dos três modos), além de revisão do código e identificação de erros. Um exemplo: a revisão apontou que um pedido de tamanho muito grande estourava o limite do `int` no cálculo do número de páginas, o que foi corrigido.
- **Divisão do trabalho em etapas:** organização do trabalho em um plano com etapas menores (base comum, um modo por vez, visualização, testes, relatório e manual), com critérios de validação para cada uma, usando os exemplos do enunciado como gabarito.
- **Explicação de conceitos e de trechos de código:** explicações sobre o funcionamento do Circular-Fit, do sistema buddy, da paginação e da tradução de endereços, e sobre partes do código que não estavam claras (por exemplo, a fusão recursiva de buddies, o teto da divisão feito com inteiros e o teste de potência de 2 com operações de bits).

Todo o código gerado com ajuda de IA foi lido, testado contra os exemplos do enunciado e compreendido pelo grupo, que está apto a explicar qualquer parte dele na apresentação.