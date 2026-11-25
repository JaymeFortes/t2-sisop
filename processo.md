# TP2 SISOP — Progresso e próximo passo

> Documento de passagem de bastão: o que já está pronto, como rodar e o que a próxima pessoa deve fazer.
> Segue as etapas do `PLANO_TP2.md`. Última atualização: 25/11/2026.

---

## Situação geral

| Etapa do plano | Situação |
|---|---|
| 0 — Decisões iniciais | ✅ Concluída (Java 17, CLI, formato de saída, código em inglês) |
| 1 — Esqueleto e leitura do arquivo | ✅ Concluída (parâmetros, parser, erros comuns, interface) |
| 2 — Modo 1: partições variáveis (2,5 pts) | ✅ Worst-Fit e Circular-Fit prontos e validados com o teste #1 (falta o teste #2) |
| 3 — Modo 2: buddy (2,5 pts) | ✅ Pronto e validado com o teste #3 |
| 4 — Modo 3: paginação (2,0 pts) | ✅ Pronto e validado com o teste #4 |
| 5 — Visualização e resumo (1,0 pt) | 🟡 Estado por linha pronto nos 3 modos; falta **ocupação máxima** e **fragmentação média** no resumo — **próximo passo** |
| 6 — Arquivos de teste | 🟡 Parcial: 5 arquivos pequenos (ver abaixo); faltam os 3 próprios com ≥ 20 linhas |
| 7, 8, 9 — Relatório, manual, entrega | ⬜ Não iniciados |

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
  mode1/VariablePartitions.java   modo 1: Worst-Fit e Circular-Fit (pronto)
  mode2/Buddy.java             modo 2: sistema buddy (pronto)
  mode3/Paging.java            modo 3: paginação simples (pronto)
tests/
  example-1.txt                teste #1 do enunciado (golden test do modo 1)
  example-3.txt                teste #3 do enunciado (golden test do modo 2)
  example-4.txt                teste #4 do enunciado (golden test do modo 3)
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
- **IN**: páginas = `(size + pageSize - 1) / pageSize` (teto só com inteiros). Conta os frames livres **antes** de ocupar: se faltar algum, recusa sem ocupar nada (sem memória virtual). Ocupa os livres de **menor índice**, contíguos ou não.
- **OUT**: libera todos os frames da tabela do processo.
- **ACC**: página = `address / pageSize`, deslocamento = `address % pageSize`, físico = `frame * pageSize + deslocamento`. Não valida nada: o `Simulator` já checou ID e endereço.
- **Frag. interna**: soma de `páginas * pageSize - tamanho` de cada processo.
- **Tabela no estado**: `lastInvolved` guarda o processo da última operação; é limpo depois de cada `state()`, para uma linha que nem chegou ao modo (ex.: ACC inválido) não repetir a tabela anterior.
- **Validação**: o teste #4 bate linha por linha (`ACC(B,8)` → físico 32; `ACC(A,22)` → endereço inválido; último `C` em frames 0, 1, 2, 7).
- **Dado já coletado para o relatório** (mesmo `example-4.txt`, memória 64): página 8 → 8 frames, 1 rejeitado, frag. final 9; página 32 → 2 frames, 2 rejeitados, frag. final 25.

---

## Como compilar e rodar (Windows, cmd)

Salve todos os arquivos antes (Ctrl+K, S no VS Code). **Sempre apague `out` antes**: se o `javac` falhar, o `java` rodaria os `.class` antigos sem avisar.

```
rmdir /s /q out
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

## ▶ Próximo passo: resumo completo (Etapa 5 do plano)

Os três modos estão prontos. Falta o resumo final ter os 4 itens do enunciado; hoje só tem admitidos e rejeitados.

| Item do resumo | Situação |
|---|---|
| Processos admitidos | ✅ (`Simulator.admitted()`) |
| Rejeitados por falta de espaço | ✅ (`Simulator.rejected()`) |
| **Ocupação máxima** da memória | ⬜ |
| **Fragmentação média** ao longo da execução | ⬜ |

**Proposta de implementação:**

1. Acrescentar dois métodos na interface `MemoryManager`, implementados por cada modo:
   - `int usedMemory()` — memória ocupada agora (modo 1: soma das partições; modo 2: soma dos blocos alocados; modo 3: frames ocupados × página).
   - `int fragmentation()` — fragmentação agora:
     - modos 2 e 3: a **frag. interna total** (o mesmo número que já aparece no estado);
     - modo 1: **fragmentação externa** = memória livre fora do maior bloco livre (`totalLivre - maiorLivre`). É o espaço que existe mas não serve para um processo grande.
2. Criar `Statistics.java`: depois de cada linha executada, registra `usedMemory()` e `fragmentation()`; guarda o máximo de ocupação e a soma/contagem para a média.
3. No fim, o `Main` imprime:
   ```
   Ocupação máxima:                28/32 (87,5%)
   Fragmentação média:             4,3
   ```
4. Deixar claro no manual/relatório o que "fragmentação" significa em cada modo (interna × externa).

Depois disso: **Etapa 6** (3 arquivos de teste próprios, ≥ 20 linhas, um que favoreça Worst-Fit e outro Circular-Fit), **Etapa 7** (relatório) e **Etapa 8** (manual).

---

## Pendências menores

- **Modo 1:** criar `tests\example-2.txt` (teste #2 do plano) — conferir no enunciado o tamanho de memória usado.

---

## Checklist rápido para quem pegar agora

- [ ] Ler a seção *Formato de saída* e as etapas 5–8 do `PLANO_TP2.md`
- [ ] Compilar e rodar os 4 golden tests (`example-1` nas duas políticas, `example-3`, `example-4`) para ver tudo funcionando
- [ ] Implementar ocupação máxima e fragmentação média (Etapa 5) e conferir que os golden tests continuam iguais
- [ ] Commit com mensagem clara e atualizar este arquivo