# TP2 SISOP — Progresso e próximo passo

> Documento de passagem de bastão: o que já está pronto, como rodar e o que a próxima pessoa deve fazer.
> Segue as etapas do `PLANO_TP2.md`. Última atualização: 25/11/2026 (Etapas 5 e 6 concluídas).

---

## Situação geral

| Etapa do plano | Situação |
|---|---|
| 0 — Decisões iniciais | ✅ Concluída (Java 17, CLI, formato de saída, código em inglês) |
| 1 — Esqueleto e leitura do arquivo | ✅ Concluída (parâmetros, parser, erros comuns, interface) |
| 2 — Modo 1: partições variáveis (2,5 pts) | ✅ Worst-Fit e Circular-Fit prontos e validados com o teste #1 |
| 3 — Modo 2: buddy (2,5 pts) | ✅ Pronto e validado com o teste #3 |
| 4 — Modo 3: paginação (2,0 pts) | ✅ Pronto e validado com o teste #4 |
| 5 — Visualização e resumo (1,0 pt) | ✅ Estado por linha nos 3 modos + resumo completo (admitidos, rejeitados, ocupação máxima, fragmentação média) |
| 6 — Arquivos de teste | ✅ 3 arquivos próprios (26–27 linhas, com rejeições e erros) + 3 exemplos do PDF + testes de borda |
| 7 — Relatório de experimentos (1,0 pt) | ⬜ **próximo passo** (todos os números já coletados, ver abaixo) |
| 8 — Manual do usuário (0,5 pt) | ⬜ README pronto serve de base |
| 9 — Fechamento e entrega | ⬜ |

---

## Convenções do projeto (seguir sempre)

- **Código em inglês:** nomes de classes, métodos, variáveis, pacotes e pastas.
- **Comentários e documentação em português.**
- **Saída impressa em português**, seguindo a seção *Formato de saída* do plano.
- **Mensagens de erro:** usar sempre as constantes de `Result` (`Result.NO_SPACE` etc.). Nunca digitar o texto à mão, para não errar acento.
- **Só ASCII na saída** além dos acentos (`->` em vez de `→`), porque o terminal do Windows quebra símbolos Unicode.
- **Java 17, só biblioteca padrão** (sem Maven/Gradle).

---

## O que já foi feito

### Estrutura atual

```
src/memory/
  Main.java                    lê os parâmetros, roda o laço, imprime estado e resumo
  Command.java                 comando já interpretado (tipo + id + valor)
  Parser.java                  transforma a linha de texto em Command (regex)
  Result.java                  sucesso/erro + as 4 mensagens exatas do enunciado
  MemoryManager.java           interface que os 3 modos implementam
  Simulator.java               trata os erros comuns e repassa a alocação ao modo
  Statistics.java              ocupação máxima e fragmentação média do resumo final
  mode1/VariablePartitions.java   modo 1: Worst-Fit e Circular-Fit (pronto)
  mode2/Buddy.java             modo 2: sistema buddy (pronto)
  mode3/Paging.java            modo 3: paginação simples (pronto)
tests/
  example-1.txt                teste #1 do enunciado (golden test do modo 1)
  example-3.txt                teste #3 do enunciado (golden test do modo 2)
  example-4.txt                teste #4 do enunciado (golden test do modo 3)
  favorece-circular.txt        26 linhas: sequência em que o Circular-Fit rejeita menos
  favorece-worst.txt           26 linhas: sequência em que o Worst-Fit rejeita menos
  misto.txt                    27 linhas: todos os modos, todos os erros, 3 tamanhos de página
  test.txt                     caso de borda: IN maior que a memória
  invalid.txt                  linhas válidas e inválidas misturadas (testa o Parser)
  errors.txt                   ID repetido, OUT/ACC de inexistente, endereço inválido
```

### Como as peças se encaixam

```
linha do arquivo -> Parser -> Command -> Simulator -> MemoryManager (modo) -> Result -> impressão
```

- **`Main`**: lê os parâmetros por argumento; o que faltar, pergunta no terminal.
  - `parseArgs` transforma os argumentos num `Map` (`--mode 1` → `mode=1`; `--step` é flag; argumento solto = arquivo).
  - `ask` valida o valor: se veio por argumento e é inválido, encerra com erro; se veio do teclado, pergunta de novo.
  - Memória e página precisam ser **potência de 2** (`n > 0 && (n & (n - 1)) == 0`); a página também precisa ser **≤ memória**.
  - Política só é pedida no modo 1; página só no modo 3.
  - Um `switch` cria o gerenciador do modo escolhido. **Ao implementar um modo novo, o `Main` não muda.**
  - Passo a passo espera o Enter a cada linha (um único `BufferedReader` para teclado).
- **`Parser`**: reconhece `IN(id,tam)`, `OUT(id)` e `ACC(id,end)`. Linha que não bate devolve `null`, e o `Main` imprime `LINHA INVÁLIDA (ignorada)` sem parar. O ID é **texto** (aceita `A` e `12`). `IN` com tamanho 0 é inválido.
- **`Simulator`**: guarda os processos admitidos (`Map` id → tamanho) e responde aos erros **comuns aos três modos**:
  - `ERRO: PROCESSO JÁ EXISTE` (IN de ID existente)
  - `ERRO: PROCESSO NÃO ENCONTRADO` (OUT/ACC de ID inexistente)
  - `ERRO: ENDEREÇO INVÁLIDO` (ACC com endereço ≥ tamanho)
  - nos modos 1 e 2, o ACC vira `AVISO: ACC ignorado neste modo`
  - processo rejeitado por falta de espaço **não entra** no `Map` (um OUT depois dá "não encontrado")
  - conta admitidos e rejeitados para o resumo
- **`MemoryManager`**: contrato de cada modo. Quem implementa um modo **só cuida da alocação**; os erros acima já chegam filtrados.

  ```java
  String name();                    // nome no cabeçalho
  Result in(String id, int size);   // ok ou Result.error(Result.NO_SPACE)
  Result out(String id);            // o id com certeza existe
  default boolean supportsAcc()     // só o modo 3 devolve true
  default Result acc(String id, int address) // só o modo 3 implementa; id existe e address < tamanho
  String state();                   // estado formatado, cada linha com 2 espaços
  default void setInvolved(String id) // processo da linha atual (só o modo 3 usa)
  int usedMemory();                 // memória ocupada agora (para a ocupação máxima)
  int fragmentation();              // fragmentação agora (para a média)
  default String fragmentationType() // "interna" (modos 2 e 3) ou "externa" (modo 1)
  ```

### Modo 1 (`VariablePartitions`)

- Memória = **lista de blocos** em ordem de endereço; cada bloco tem `start`, `size` e `id` (`null` = livre).
- **IN**: escolhe o bloco pela política, corta o tamanho exato no início dele; a sobra vira bloco livre logo depois.
- **OUT**: libera e funde com o vizinho livre da direita e depois da esquerda (nessa ordem, por causa dos índices da lista).
- **Worst-Fit**: maior bloco livre que caiba; empate fica com o de menor endereço (`>` estrito).
- **Circular-Fit**: ponteiro começa em 0 e vai para logo depois da última partição alocada (volta a 0 no fim da memória). A busca começa no primeiro bloco com início ≥ ponteiro e dá a volta na lista com `%`. **Só a busca é circular**: blocos do fim e do início nunca se juntam.
- **Validação**: as duas colunas do teste #1 do plano batem linha por linha (o `G(7)` é rejeitado no Worst-Fit e aceito no Circular-Fit).

### Modo 2 (`Buddy`)

- Memória = **árvore binária**: a raiz é a memória inteira; um nó dividido tem dois filhos com metade do tamanho (os *buddies*). Só as **folhas** são blocos reais (livre = `id == null`, alocada = tem `id`).
- O estado do nó sai das referências: tem filhos = dividido; folha com `id` = alocada; folha sem `id` = livre.
- **IN**: arredonda para a menor potência de 2 ≥ pedido; `findSmallestFree` percorre as folhas da esquerda para a direita e fica com a **menor** que caiba (empate = menor endereço, `<` estrito); divide ao meio descendo **sempre pela esquerda** até o tamanho certo.
- **OUT**: libera a folha e chama `coalesce(root)`, que resolve os filhos antes do pai: se os dois filhos ficaram livres, o nó volta a ser folha. Como a recursão volta subindo, a fusão se repete enquanto o buddy estiver livre. Só filhos do **mesmo nó** se fundem.
- **Frag. interna**: cada folha guarda `requested`; o `state()` soma `size - requested` das ocupadas. Ocupado aparece como `ID(pedido/bloco)`.
- **Validação**: o teste #3 bate linha por linha (frag. 3 → 10 → 22 → 24 → 24 → 21 → 19 → 12 → 0; fusões em `[0-15]`, `[0-31]`, `[0-63]`).

### Modo 3 (`Paging`)

- Memória = **vetor de frames** (`String[] frames`, `null` = livre) + `Map` id → **tabela de páginas** (`List<Integer>`: posição = página, valor = frame) + `Map` id → tamanho pedido.
- **IN**: recusa `size > memória` antes de tudo (sem isso, tamanhos perto do limite do `int` estouravam a conta e o processo era admitido sem frames). Páginas = `(size + pageSize - 1) / pageSize` (teto só com inteiros). Conta os frames livres **antes** de ocupar: se faltar algum, recusa sem ocupar nada (sem memória virtual). Ocupa os livres de **menor índice**, contíguos ou não.
- **OUT**: libera todos os frames da tabela do processo.
- **ACC**: página = `address / pageSize`, deslocamento = `address % pageSize`, físico = `frame * pageSize + deslocamento`. Não valida nada: o `Simulator` já checou ID e endereço.
- **Frag. interna**: soma de `páginas * pageSize - tamanho` de cada processo.
- **Tabela no estado**: o `Simulator` chama `manager.setInvolved(id)` no início de cada comando (método `default` vazio na interface; só o modo 3 usa). O estado mostra a tabela desse processo se ele estiver na memória, **inclusive quando a linha deu erro** (ex.: `ACC(A,22)`, `IN` com ID repetido). A referência é limpa depois de cada `state()`.
- **Validação**: o teste #4 bate linha por linha (`ACC(B,8)` → físico 32; `ACC(A,22)` → endereço inválido; último `C` em frames 0, 1, 2, 7).
- **Dado já coletado para o relatório** (mesmo `example-4.txt`, memória 64): página 8 → 8 frames, 1 rejeitado, frag. final 9; página 32 → 2 frames, 2 rejeitados, frag. final 25.

### Resumo final (`Statistics`)

- Cada modo informa `usedMemory()` (modo 1: soma das partições; modo 2: soma dos blocos alocados inteiros; modo 3: frames ocupados × página) e `fragmentation()` (modos 2 e 3: interna total; modo 1: **externa = total livre − maior bloco livre**).
- O `Main` chama `statistics.record(...)` depois de **cada comando executado, inclusive os que deram erro** (a memória continua ocupada naquela linha). Linhas inválidas e em branco ficam de fora.
- Resumo: `Ocupação máxima: 25/32 (78,1%)` e `Fragmentação média: 4,10 posições (externa)` (formato brasileiro, com vírgula).

---

## Como compilar e rodar (Windows, cmd)

Salve todos os arquivos antes (Ctrl+K, S no VS Code). **Sempre apague `out` antes**: se o `javac` falhar, o `java` rodaria os `.class` antigos sem avisar.

```
rmdir /s /q out          (no PowerShell: Remove-Item -Recurse -Force out)
javac -encoding UTF-8 --release 17 -d out src\memory\*.java src\memory\mode1\*.java src\memory\mode2\*.java src\memory\mode3\*.java
```

Se o `javac` não imprimir nada, compilou. Exemplos de execução:

```
java -cp out memory.Main tests\example-1.txt --mode 1 --mem 32 --policy worst --continuous
java -cp out memory.Main tests\example-1.txt --mode 1 --mem 32 --policy circular --step
java -cp out memory.Main tests\example-3.txt --mode 2 --mem 64 --continuous
java -cp out memory.Main tests\example-4.txt --mode 3 --mem 64 --page 8 --continuous
java -cp out memory.Main          (sem nada: pergunta tudo)
```

> Pelo botão Run do VS Code só o arquivo é passado; o resto é perguntado no terminal.

---

## ▶ Próximo passo: relatório de experimentos (Etapa 7 do plano)

PDF de **3 a 5 páginas** (seção 9 do enunciado). Todos os números abaixo já foram conferidos rodando o simulador.

### Dados coletados (memória 64, exceto os exemplos do PDF)

**Exemplos do PDF (regressão)**

| Execução | Admitidos | Rejeitados | Ocupação máx. | Frag. média |
|---|---|---|---|---|
| example-1, mem 32, worst | 6 | 1 | 23/32 (71,9%) | 5,60 (externa) |
| example-1, mem 32, circular | 7 | 0 | 25/32 (78,1%) | 4,10 (externa) |
| example-3, buddy | 4 | 1 | 64/64 (100,0%) | 15,00 (interna) |
| example-4, página 8 | 4 | 1 | 64/64 (100,0%) | 9,38 (interna) |

**Worst-Fit × Circular-Fit (item 2)**

| Arquivo | Política | Admitidos | Rejeitados | Ocupação máx. | Frag. externa média |
|---|---|---|---|---|---|
| favorece-circular | Worst-Fit | 7 | 7 | 64/64 (100,0%) | 1,56 |
| favorece-circular | Circular-Fit | 11 | 3 | 59/64 (92,2%) | 2,36 |
| favorece-worst | Worst-Fit | 12 | 4 | 60/64 (93,8%) | 4,68 |
| favorece-worst | Circular-Fit | 8 | 8 | 60/64 (93,8%) | 2,32 |
| misto | Worst-Fit | 9 | 3 | 55/64 (85,9%) | 4,12 |
| misto | Circular-Fit | 9 | 3 | 55/64 (85,9%) | 3,08 |

**Buddy × partições variáveis (item 3)** — `misto.txt`: buddy 7 admitidos, 5 rejeitados, frag. interna média 15,35; partições 9 admitidos, 3 rejeitados, frag. externa média 4,12 (worst) / 3,08 (circular). No example-3 o buddy chega a 100% de ocupação com só 40 posições pedidas (24 de fragmentação interna).

**Efeito do tamanho da página (item 4)** — `misto.txt`:

| Página | Frames (= linhas máx. da tabela) | Admitidos | Rejeitados | Frag. interna média |
|---|---|---|---|---|
| 4 | 16 | 8 | 4 | 7,04 |
| 8 | 8 | 7 | 5 | 13,27 |
| 16 | 4 | 6 | 6 | 26,27 |

(Também no example-4: página 8 → 1 rejeitado, frag. final 9; página 32 → 2 rejeitados, frag. final 25.)

### Estrutura sugerida do relatório (seção 9 do PDF)

1. **Arquivos de teste** — descrever os 3 próprios + exemplos do PDF.
2. **Worst-Fit × Circular-Fit** — nenhuma é sempre melhor; menos rejeições não implica menos fragmentação externa.
3. **Buddy × partições variáveis** — fragmentação interna × externa; fusão de buddies (rápida, só com o companheiro) × fusão de vizinhos livres.
4. **Efeito do tamanho da página** — página maior: mais fragmentação interna e rejeições, tabela de páginas menor.
5. **Conclusões** — recomendação para muitos processos pequenos, poucos grandes e tamanhos variados, com os dados.
6. **Uso de IA** — exigido pela seção 12 (mesmo texto do README).

Depois: **Etapa 8** (manual em PDF — o README já cobre quase tudo) e **Etapa 9** (zip `JaymeFortes_RodrigoGagliardi_LuccaMarques.zip` com código, testes, manual.pdf e relatorio.pdf).

---

## Checklist rápido para quem pegar agora

- [ ] Ler as etapas 7–9 do `PLANO_TP2.md` e a seção 9 do enunciado
- [ ] Compilar e rodar os 4 golden tests (`example-1` nas duas políticas, `example-3`, `example-4`) para ver tudo funcionando
- [ ] Escrever o relatório (Etapa 7) com os dados acima e gerar o PDF
- [ ] Commit com mensagem clara e atualizar este arquivo