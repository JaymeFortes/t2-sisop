# TP2 — Simulador de Gerenciamento de Memória · Plano de trabalho

Roteiro dividido em etapas para o grupo (3 integrantes) construir a ferramenta, rodar os experimentos e entregar tudo. As regras de cada modo já estão destrinchadas aqui, então dá para codar sem ficar voltando ao PDF. Os exemplos do enunciado viram **testes de validação** (golden tests) no fim.

> **Valor:** 10,0 · **Entrega:** .zip no Moodle com código-fonte + arquivos de teste + manual do usuário (PDF) + relatório de experimentos (PDF) · **Apresentação:** demo com arquivo do professor na hora; todos respondem sobre qualquer parte do código.

---

## Etapa 0 — Decisões iniciais (fazer juntos, ~30 min)

- [x] **Linguagem escolhida: Java.** ✔ Compila e roda em qualquer notebook com JDK; vocês já dominam.
  - **Cuidados do Java neste trabalho:** garantir que **compila sem erro** (trabalho que não compila não é avaliado); fixar a **versão do JDK** (17 LTS, compilando com `--release 17`) e registrá-la no manual; usar só a **biblioteca padrão** (nada de Maven/Gradle obrigatório — evita problema de dependência no notebook do professor).
  - **Build/execução simples, sem ferramenta externa:**
    - compilar: `javac -encoding UTF-8 --release 17 -d out $(find src -name "*.java")`
    - rodar: `java -cp out memory.Main`
    - (opcional) empacotar: `jar cfe TP2.jar memory.Main -C out .` → roda com `java -jar TP2.jar`
- [x] **Idioma do código: inglês.** ✔ Nomes de classes, métodos, variáveis, pacotes e pastas em **inglês**; **comentários e documentação** (Javadoc, README, manual, relatório) em **português**. A **saída impressa** continua em português, porque as mensagens de erro do enunciado precisam ser idênticas.
- [x] **Interface por linha de comando.** Use `.\start` no PowerShell para compilar e escolher os parâmetros no terminal. Use `.\run.cmd` para executar o caso configurado em `run.ps1`.
- [x] **Repositório Git criado.** ✔ Integrantes: Jayme Fortes, Rodrigo Gagliardi e Lucca Marques.
- [x] **Formato de saída definido.** ✔ Ver a seção **Formato de saída** logo abaixo.

**Entregável da etapa:** **Java** fixado, CLI e formato de saída definidos, tarefas distribuídas.

---

## Formato de saída (padrão do grupo)

O enunciado não fixa um layout, só o **conteúdo mínimo** de cada linha (Etapa 5) e o **texto exato das mensagens de erro**. Este é o padrão que os três modos seguem.

### Regras gerais

- Cabeçalho: `=== Modo: <nome> - memória <N> ===`
- Cada linha do arquivo vira um bloco:
  - `[nn] <comando como está no arquivo>`
  - `  -> OK: <descrição>` ou `  -> <MENSAGEM DE ERRO EXATA>`
  - o estado da memória logo abaixo, com cada linha começando por **dois espaços**
- Intervalos de endereço sempre como `[início-fim]`, **com os dois extremos incluídos**.
- No mapa, região livre aparece como `livre(tam)` e região ocupada como `ID(tam)`.
- O bloco `[00] início` mostra o estado antes da primeira linha.
- Linha malformada exibe `  -> LINHA INVÁLIDA (ignorada)` e o estado atual da memória. Linhas em branco são ignoradas.
- Fora os acentos das mensagens obrigatórias, usar **só símbolos ASCII** (`->`, `|`, `-`): setas e pontos Unicode viram `?` no terminal do Windows.
- O mapa é uma **lista de regiões**, e não uma barra `|AAAA|....|`, porque a lista funciona com memória de qualquer tamanho e com ID de mais de um caractere (ex.: `12`).

### Modo 1 · Partições variáveis (teste #1, Worst-Fit, depois do OUT(C))

```
[06] OUT(C)
  -> OK: C liberado de [12-17] (6)
  Memória: [0-7] livre(8) | [8-11] B(4) | [12-17] livre(6) | [18-22] D(5) | [23-31] livre(9)
  Livres:  8 | 6 | 9   (total 23, maior 9)
```

No Circular-Fit entra mais uma linha com o ponteiro:

```
  Ponteiro: 23
```

Erro por falta de espaço (o estado continua aparecendo, sem mudança):

```
[09] IN(G,7)
  -> ESPAÇO INSUFICIENTE DE MEMÓRIA
  Memória: [0-5] F(6) | [6-7] livre(2) | ...
  Livres:  2 | 6 | 6   (total 14, maior 6)
```

### Modo 2 · Buddy (teste #3, depois do IN(C,20))

Ocupado aparece como `ID(pedido/bloco)`, para mostrar a fragmentação interna:

```
[03] IN(C,20)
  -> OK: C alocado em [32-63] (bloco 32, pedido 20)
  Memória: [0-7] A(5/8) | [8-15] livre(8) | [16-31] B(9/16) | [32-63] C(20/32)
  Livres:  8   (total 8)
  Frag. interna total: 22
```

### Modo 3 · Paginação (teste #4, depois do IN(D,16))

```
[04] IN(D,16)
  -> OK: D com 2 páginas -> frames 5, 6
  Frames:  0:A 1:A 2:A 3:B 4:B 5:D 6:D 7:-
  Livres:  7   (1 frame)
  Tabela D: p0->f5  p1->f6
  Frag. interna total: 11
```

ACC (mostra a tabela do processo envolvido):

```
[05] ACC(B,8)
  -> OK: B end. lógico 8 = página 1, desloc. 0 -> frame 4 -> físico 32
  Frames:  0:A 1:A 2:A 3:B 4:B 5:D 6:D 7:-
  Livres:  7   (1 frame)
  Tabela B: p0->f3  p1->f4
  Frag. interna total: 11
```

Nos modos 1 e 2, ACC vira `  -> OK: AVISO: ACC ignorado neste modo`.

### Resumo final

```
=== RESUMO ===
Processos admitidos:            7
Rejeitados por falta de espaço: 0
Ocupação máxima:                25/32 (78,1%)
Fragmentação média:             4,10 posições (externa)
```

Fragmentação média: interna nos modos 2 e 3; externa no modo 1 (`total livre − maior bloco livre`).

---

## Etapa 1 — Esqueleto e leitura do arquivo (base comum) ✅ concluída

Tudo que os três modos compartilham. Faça primeiro — é a fundação.

- [x] **Parâmetros em tempo de execução** (perguntar ao usuário ou receber por argumento):
  - Modo: `1` (partições variáveis), `2` (buddy) ou `3` (paginação).
  - Tamanho da memória: **obrigatório**, **potência de 2** (validar!).
  - Política (**só modo 1**): `1` para Worst-Fit ou `2` para Circular-Fit no terminal interativo. Por argumento, use `--policy worst` ou `--policy circular`.
  - Tamanho da página (**só modo 3**): potência de 2, **≤ tamanho da memória**. Nº de frames = memória ÷ página.
  - Caminho do arquivo de requisições ou nome curto em `tests/`, como `teste1`.
  - Modo de execução: **passo a passo** (espera Enter a cada linha) ou **contínuo** (roda tudo).
- [x] **Parser do arquivo de requisições.** Um comando por linha:
  - `IN(ID, tamanho)` — aloca `tamanho` posições para o processo `ID` (guardado como **texto**, para aceitar `A` dos exemplos e `12`).
  - `OUT(ID)` — libera toda a memória do processo `ID`.
  - `ACC(ID, endereço)` — **só modo 3**; nos modos 1 e 2, **ignorar com aviso**.
  - Dica: um regex simples resolve (`IN\((\w+),\s*(\d+)\)` etc.). Ignorar linhas em branco.
- [x] **Mensagens de erro padronizadas** (o comando é descartado, mas a execução continua):

  | Situação | Mensagem exata |
  |---|---|
  | Sem espaço para o `IN` | `ESPAÇO INSUFICIENTE DE MEMÓRIA` |
  | `IN` com ID que já existe | `ERRO: PROCESSO JÁ EXISTE` |
  | `OUT`/`ACC` com ID inexistente | `ERRO: PROCESSO NÃO ENCONTRADO` |
  | `ACC` com endereço negativo ou ≥ tamanho do processo | `ERRO: ENDEREÇO INVÁLIDO` |

  > ⚠️ Processo **rejeitado por falta de espaço não é admitido** → um `OUT` posterior para ele dá `PROCESSO NÃO ENCONTRADO`. Escreva as mensagens **exatamente** como acima (acentos inclusos) — o professor testa com arquivo próprio.
- [x] **Interface comum entre os modos (Java).** Uma `interface MemoryManager` que os três modos implementam — assim o laço principal (ler linha → chamar método → imprimir) é **o mesmo** para os três (polimorfismo). Os erros comuns (ID repetido, ID inexistente, endereço inválido) ficam centralizados na classe `Simulator`, e cada modo só cuida da alocação:
  ```java
  public interface MemoryManager {
      String name();
      Result in(String id, int size);             // ok ou ESPAÇO INSUFICIENTE
      Result out(String id);
      default boolean supportsAcc() { return false; }
      default Result acc(String id, int address) { ... } // só modo 3
      String state();                              // texto no formato combinado
  }
  ```
  `Main` lê os parâmetros e instancia `VariablePartitions`, `Buddy` ou `Paging` conforme o modo.

### Estrutura de pacotes

```
src/memory/
  Main.java                  // lê parâmetros + arquivo, roda o laço, imprime
  Simulator.java             // erros comuns + contagem de admitidos/rejeitados
  MemoryManager.java         // a interface comum
  Result.java                // sucesso/erro + as mensagens exatas
  Command.java               // IN/OUT/ACC já interpretado
  Parser.java                // interpreta IN/OUT/ACC de cada linha
  mode1/VariablePartitions.java   // Worst-Fit e Circular-Fit
  mode2/Buddy.java
  mode3/Paging.java
  Statistics.java            // ocupação máxima e fragmentação média do resumo
tests/                       // example-1, -3, -4 = exemplos do enunciado; teste1, teste2, teste3 = próprios
```

**Entregável da etapa:** `Main` lê os parâmetros, lê o arquivo, despacha cada linha para um gerenciador (ainda vazio) e imprime os erros corretamente.

---

## Etapa 2 — Modo 1: Partições variáveis (2,5 pts) ✅ concluída (validada com o teste #1)

Memória começa como **um único bloco livre**. Cada `IN` corta uma partição do tamanho **exato** pedido; a sobra continua livre. Cada `OUT` libera, e **blocos livres adjacentes são fundidos**.

**Estrutura sugerida:** lista de blocos ordenada por endereço, cada bloco = `{inicio, tamanho, livre, id}`.

### Worst-Fit
- [x] Percorrer todos os blocos livres e escolher o **maior** que comporte o processo.
- [x] Empate → o de **menor endereço**.
- [x] A partição é criada **no início** do bloco escolhido; a sobra vira bloco livre logo após.

### Circular-Fit (Next-Fit)
- [x] Manter um **ponteiro** que aponta para o endereço **logo após o fim da última partição alocada** (ou `0` se esse endereço = tamanho da memória). **Começa em 0.**
- [x] A busca começa no primeiro bloco livre com `inicio >= ponteiro`, segue em ordem crescente; se chegar ao fim, **volta ao primeiro bloco livre** (busca circular).
- [x] Aloca no **primeiro** bloco que couber.
- [x] ⚠️ **A memória NÃO é circular — só a busca é.** Blocos livres do fim e do início **nunca** se somam. Só funde blocos que são **vizinhos de fato**.

### Comum aos dois
- [x] `OUT`: marcar a partição como livre e **fundir** com vizinho livre à esquerda e/ou à direita.
- [x] Atualizar o ponteiro do Circular-Fit quando necessário.

**Validação:** rodar o golden test #1 (fim do arquivo) nas duas políticas e bater **bloco a bloco** com a tabela do PDF.

---

## Etapa 3 — Modo 2: Sistema buddy (2,5 pts) ✅ concluída (validada com o teste #3)

Memória = segmento único potência de 2, dividido recursivamente em pares de **companheiros (buddies)**.

**Estrutura sugerida:** árvore binária de blocos (cada nó = `{tamanho, inicio, estado: livre/dividido/alocado, id}`), ou listas de blocos livres por tamanho (uma lista para 2, 4, 8, …).

- [x] **Arredondar** cada pedido para a **menor potência de 2 ≥ tamanho** (5→8, 16→16, 20→32).
- [x] **Alocar** no menor bloco livre que comporte o arredondado. Se for maior que o necessário, **dividir** em dois buddies repetidamente até chegar no tamanho. O bloco alocado é sempre o **da esquerda** (menor endereço).
- [x] **Liberar:** se o **buddy também estiver livre**, **coalescer** (fundir) os dois; repetir subindo enquanto der.
- [x] **Fragmentação interna** de cada processo = `bloco alocado − tamanho pedido`. **Exibir o total a cada linha.**
- [x] Neste modo o usuário só informa `IN`/`OUT` (sem política, sem página).

**Validação:** golden test #3 — conferir a árvore e a fragmentação interna total em cada passo (3 → 10 → 22 → 24 → cheia; depois coalescências até 0).

---

## Etapa 4 — Modo 3: Paginação simples (2,0 pts) ✅ concluída (validada com o teste #4)

Memória dividida em **frames** de tamanho fixo (= tamanho da página). Cada processo vira **páginas** e ganha uma **tabela de páginas** (página → frame). **Sem memória virtual:** só admite se **todas** as páginas couberem.

**Estrutura sugerida:** vetor de frames (`frame[i] = id ou livre`) + dicionário `id → tabela de páginas (lista de frames)`.

- [x] **Nº de páginas** = `teto(tamanho ÷ tamanho da página)`.
- [x] **Alocar** nos frames livres de **menor índice** (não precisam ser contíguos). Se frames livres < páginas necessárias → `ESPAÇO INSUFICIENTE DE MEMÓRIA`.
- [x] **Fragmentação interna** = sobra da última página (`páginas × tamanho_página − tamanho`). Exibir o **total** a cada linha.
- [x] `OUT`: liberar **todos** os frames do processo.
- [x] `ACC(id, endereço)`:
  - `página = endereço ÷ tamanho_página` · `deslocamento = endereço mod tamanho_página`
  - validar `0 <= endereço < tamanho do processo` (senão `ERRO: ENDEREÇO INVÁLIDO`)
  - consultar a tabela → **frame**, e **endereço físico = frame × tamanho_página + deslocamento**.

**Validação:** golden test #4 (memória 64, página 8 → 8 frames). Conferir frames atribuídos, a tradução `ACC(B,8)→físico 32`, o `ERRO: ENDEREÇO INVÁLIDO` do `ACC(A,22)`, e a realocação não-contígua do `C` após `OUT(A)`.

---

## Etapa 5 — Visualização e resumo (1,0 pt) ✅ concluída

A **cada linha**, imprimir no mínimo:
- [x] o comando e seu resultado (sucesso ou mensagem de erro);
- [x] **blocos livres contíguos** (modos 1 e 2) ou **frames livres** (modo 3), com o total;
- [x] um **mapa** (textual já basta) da memória mostrando a que processo cada região pertence; no modo 3, a **tabela de páginas** do processo envolvido;
- [x] **fragmentação interna total** (modos 2 e 3) **ou** o **maior bloco livre + total livre** (modo 1, para enxergar a fragmentação externa).

Ao **fim da execução**, um **resumo**:
- [x] nº de processos **admitidos**;
- [x] nº de processos **rejeitados** por falta de espaço;
- [x] **ocupação máxima** da memória atingida;
- [x] **fragmentação média** ao longo da execução (interna nos modos 2 e 3; externa no modo 1 = total livre − maior bloco livre).

> Seguir o **Formato de saída** definido depois da Etapa 0 (mapa como lista de regiões `[início-fim] ID(tam)`).

---

## Etapa 6 — Arquivos de teste (parte do 1,0 pt do relatório) ✅ concluída

- [x] Criar **≥ 3 arquivos** de requisições próprios, **≥ 20 linhas cada**, incluindo:
  - casos de **rejeição** por falta de espaço;
  - casos de **erro de comando** (ID repetido, OUT de inexistente, ACC inválido);
  - um arquivo que **favoreça o Circular-Fit** e outro que **favoreça o Worst-Fit** (para a comparação do relatório).
- [x] Guardar os 3 exemplos do enunciado (`example-1`, `example-3`, `example-4`) como arquivos de **regressão** (devem sempre bater).
- [x] Testes próprios na pasta `tests/`: `teste1.txt` (26 linhas, menos rejeições no Circular-Fit), `teste2.txt` (26 linhas, menos rejeições no Worst-Fit) e `teste3.txt` (27 linhas, carga mista).

---

## Etapa 7 — Relatório de experimentos (1,0 pt) — PDF, 3 a 5 páginas ▶ próximo passo

Os resultados iniciais já estão em [processo.md](processo.md). Falta analisar os dados e escrever o relatório.
1. [ ] **Arquivos de teste** usados (descrever cada um).
2. [ ] **Worst-Fit × Circular-Fit:** mesmos arquivos nas duas políticas → comparar **rejeições** e **fragmentação externa**.
3. [ ] **Buddy × partições variáveis:** discutir o trade-off **fragmentação interna × externa** e a facilidade de **fusão de blocos**.
4. [ ] **Efeito do tamanho da página:** rodar o modo 3 com **≥ 3 tamanhos de página** no mesmo arquivo → impacto na **fragmentação interna** e no **tamanho das tabelas de páginas**.
5. [ ] **Conclusões:** qual abordagem recomendar para cada carga — **muitos processos pequenos**, **poucos grandes**, **tamanhos variados** — justificando **com os dados**.

- [ ] Citar no relatório os trechos de terceiros ou gerados por IA usados no trabalho, conforme a seção 12 do enunciado.

---

## Etapa 8 — Manual do usuário (0,5 pt) + organização do código

- [ ] **Manual (PDF):** versão do **JDK** usada; atalho `.\start` no Windows; como **compilar** (`javac -encoding UTF-8 --release 17 -d out $(find src -name "*.java")`) e **executar** (`java -cp out memory.Main`); formato do arquivo de requisições; exemplos de uso dos 3 modos; significado das saídas.
- [x] **Código organizado:** separar por pacote (`Parser`, `mode1`, `mode2`, `mode3`, `Statistics`, `Main`) conforme a estrutura da Etapa 1. Nomes claros, Javadoc/comentários nos pontos-chave.

---

## Etapa 9 — Fechamento e entrega

- [ ] Rodar **todos** os golden tests e os 3 arquivos próprios uma última vez.
- [ ] Garantir que **compila/roda sem erro** (trabalho que não compila **não é avaliado**).
- [ ] Montar o **.zip** com nome contendo **nome e sobrenome dos 3**; conteúdo: código + testes + manual.pdf + relatorio.pdf.
- [ ] Cada integrante marca o grupo no Moodle; um escolhe o horário de apresentação.
- [ ] **Todos** revisam o código inteiro — na arguição, qualquer um pode ser perguntado sobre qualquer parte.

---

## Sugestão de divisão entre os 3 integrantes

A base (Etapa 1) e a visualização (Etapa 5) são compartilhadas — façam juntos ou em par no começo. Depois:

| Integrante | Responsável por | Pontos |
|---|---|---|
| **A** | Modo 1 (Worst-Fit + Circular-Fit + fusão) | 2,5 |
| **B** | Modo 2 (buddy: divisão, coalescência, frag. interna) | 2,5 |
| **C** | Modo 3 (paginação + tradução) + base/parser liderada | 2,0 + 1,0 |
| **Todos** | Visualização/resumo, relatório, manual, testes, revisão | 2,0 |

> Mesmo com donos, **todos revisam tudo** (por causa da arguição individual). Usem Git com commits de cada um.

---

## Cronograma sugerido (ajustem às datas de vocês)

1. **Dia 1:** Etapa 0 + Etapa 1 (base rodando, lê arquivo, imprime erros).
2. **Dias 2–4:** Etapas 2, 3 e 4 em paralelo (um modo por pessoa), cada um validando no seu golden test.
3. **Dia 5:** Etapa 5 (visualização/resumo) integrada aos três modos.
4. **Dia 6:** Etapa 6 (arquivos de teste) + rodar experimentos.
5. **Dia 7:** Etapas 7 e 8 (relatório + manual).
6. **Dia 8:** Etapa 9 (revisão geral, empacotamento, ensaio da apresentação).

---

## Anexo — Golden tests (exemplos do enunciado, para validar)

Use estes como testes de regressão: a saída tem que bater exatamente.

### Teste #1 — Modo 1, memória 32 (blocos livres após cada linha)

Requisições: `IN(A,8) IN(B,4) IN(C,6) IN(D,5) OUT(A) OUT(C) IN(E,3) IN(F,6) IN(G,7) OUT(B)`

| Linha | Worst-Fit (blocos livres) | Circular-Fit (blocos livres) |
|---|---|---|
| início | 32 | 32 |
| IN(A,8) | 24 | 24 |
| IN(B,4) | 20 | 20 |
| IN(C,6) | 14 | 14 |
| IN(D,5) | 9 | 9 |
| OUT(A) | 8 \| 9 | 8 \| 9 |
| OUT(C) | 8 \| 6 \| 9 | 8 \| 6 \| 9 |
| IN(E,3) | 8 \| 6 \| 6 | 8 \| 6 \| 6 |
| IN(F,6) | 2 \| 6 \| 6 | 8 \| 6 |
| IN(G,7) | **INSUFICIENTE** (2 \| 6 \| 6) | 1 \| 6 |
| OUT(B) | 12 \| 6 | 11 |

Ponto-chave: no Worst-Fit o `F` vai para o maior bloco (8, no início), então o `G(7)` é **rejeitado** mesmo com 14 livres no total (**fragmentação externa**). No Circular-Fit o `F` ocupa o bloco de 6 do fim (busca a partir do ponteiro), o ponteiro volta a 0, e o `G` cabe no bloco de 8 do início.

> **Nota:** versões antigas deste plano listavam um "Teste #2" de partições variáveis com a mesma sequência do teste #3. Foi um erro de transcrição: o enunciado tem **um** cenário de partições variáveis (o teste #1), executado nas duas políticas.

### Teste #3 — Modo 2 (buddy), memória 64

Requisições: `IN(A,5) IN(B,9) IN(C,20) IN(D,6) IN(E,3) OUT(A) OUT(D) OUT(B) OUT(C)`

Fragmentação interna total esperada por passo:
- IN(A,5)→bloco 8: **3**
- IN(B,9)→bloco 16: 3+7 = **10**
- IN(C,20)→bloco 32: 10+12 = **22**
- IN(D,6)→bloco 8: 22+2 = **24** (memória cheia)
- IN(E,3): **INSUFICIENTE** (precisa de bloco 4, não há) — árvore inalterada, frag. = 24
- OUT(A): buddy de A (8@8 = D) está ocupado → **sem fusão**, frag. = **21**
- OUT(D): 8@0 + 8@8 fundem em 16@0, frag. = **19**
- OUT(B): 16@0 + 16@16 fundem em 32@0, frag. = **12**
- OUT(C): 32@0 + 32@32 fundem em 64@0, frag. = **0**

### Teste #4 — Modo 3 (paginação), memória 64, página 8 → 8 frames

Requisições: `IN(A,20) IN(B,9) IN(C,30) IN(D,16) ACC(B,8) ACC(A,22) OUT(A) IN(C,30)`

| Linha | Resultado | Frames livres | Frag. interna total |
|---|---|---|---|
| IN(A,20) | A: páginas 0,1,2 → frames 0,1,2 | 3,4,5,6,7 | 4 |
| IN(B,9) | B: páginas 0,1 → frames 3,4 | 5,6,7 | 11 |
| IN(C,30) | **INSUFICIENTE** (precisa 4 frames, há 3) | 5,6,7 | 11 |
| IN(D,16) | D: páginas 0,1 → frames 5,6 | 7 | 11 |
| ACC(B,8) | página 1, desloc. 0 → frame 4 → **físico 32** | 7 | 11 |
| ACC(A,22) | **ERRO: ENDEREÇO INVÁLIDO** (A tem 20) | 7 | 11 |
| OUT(A) | frames 0,1,2 liberados | 0,1,2,7 | 7 |
| IN(C,30) | C: páginas 0..3 → frames 0,1,2,7 (não contíguos) | (nenhum) | 9 |

---

### Lembretes de "pegar pontos fáceis"
- Mensagens de erro **idênticas** às do enunciado (acentos, maiúsculas).
- Processo rejeitado por espaço **não entra** na memória.
- Circular-Fit: **memória não circular, só a busca**; não some fim+início.
- Buddy: bloco alocado é sempre o **da esquerda**; coalescer **enquanto** o buddy estiver livre.
- Paginação: frames de **menor índice**, sem precisar contíguos; admitir só se **todas** as páginas couberem.
- Validar que o tamanho da memória (e página) é **potência de 2**.
- Recusar `IN` maior que a memória **antes** de qualquer conta (evita estouro de `int` com tamanhos enormes).
- Saída só com ASCII além dos acentos obrigatórios (nada de `→` impresso).