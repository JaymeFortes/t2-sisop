# TP2 SISOP — Progresso e próximo passo

> Documento de passagem de bastão: o que já está pronto, como rodar e o que a próxima pessoa deve fazer.
> Segue as etapas do `PLANO_TP2.md`. Última atualização: 22/11/2026.

---

## Situação geral

| Etapa do plano | Situação |
|---|---|
| 0 — Decisões iniciais | ✅ Concluída (Java 17, CLI, formato de saída, código em inglês) |
| 1 — Esqueleto e leitura do arquivo | ✅ Concluída (parâmetros, parser, erros comuns, interface) |
| 2 — Modo 1: partições variáveis (2,5 pts) | ✅ Worst-Fit e Circular-Fit prontos e validados com o teste #1 (falta o teste #2) |
| 3 — Modo 2: buddy (2,5 pts) | ⬜ Só o esqueleto (`mode2/Buddy.java`) — **próximo passo** |
| 4 — Modo 3: paginação (2,0 pts) | ⬜ Só o esqueleto (`mode3/Paging.java`) — **próximo passo** |
| 5 — Visualização e resumo (1,0 pt) | 🟡 Parcial: estado por linha no modo 1 + admitidos/rejeitados no resumo |
| 6 — Arquivos de teste | 🟡 Parcial: 3 arquivos pequenos (ver abaixo) |
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
  mode2/Buddy.java             modo 2: esqueleto (devolve "NÃO IMPLEMENTADO")
  mode3/Paging.java            modo 3: esqueleto (devolve "NÃO IMPLEMENTADO")
tests/
  example-1.txt                teste #1 do enunciado (golden test do modo 1)
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

## ▶ Próximo passo: modos 2 e 3 (podem ser feitos em paralelo)

Cada modo é uma classe que **já existe como esqueleto** e implementa `MemoryManager`. Basta preencher `in`, `out` e `state` (e `acc` no modo 3). `Main` e `Simulator` **não mudam**.

### Modo 2 — `mode2/Buddy.java` (Etapa 3 do plano, 2,5 pts)

1. **Estrutura**: árvore binária de blocos (nó = `start`, `size`, livre/dividido/alocado, `id`, `requested`) **ou** listas de blocos livres por tamanho.
2. **IN**: arredondar para a menor potência de 2 ≥ tamanho (5→8, 20→32); pegar o menor bloco livre que caiba; dividir ao meio até chegar no tamanho; alocar sempre o **da esquerda**. Sem bloco → `Result.error(Result.NO_SPACE)`.
3. **OUT**: liberar; se o buddy também estiver livre, fundir; repetir subindo.
4. **`state()`** no formato do plano:
   ```
     Memória: [0-7] A(5/8) | [8-15] livre(8) | [16-31] B(9/16) | [32-63] C(20/32)
     Livres:  8   (total 8)
     Frag. interna total: 22
   ```
   (ocupado = `ID(pedido/bloco)`; frag. interna = soma de `bloco - pedido`).
5. **Validar** com o teste #3 (criar `tests\example-3.txt`):
   `IN(A,5) IN(B,9) IN(C,20) IN(D,6) IN(E,3) OUT(A) OUT(D) OUT(B) OUT(C)`, memória 64.
   Frag. interna esperada: 3 → 10 → 22 → 24 → (E rejeitado) 24 → 21 → 19 → 12 → 0.

### Modo 3 — `mode3/Paging.java` (Etapa 4 do plano, 2,0 pts)

1. **Estrutura**: vetor de frames (`String[] frames`, `null` = livre) + `Map<String, List<Integer>>` id → tabela de páginas.
2. **IN**: páginas = `ceil(size / pageSize)`; se frames livres < páginas → `NO_SPACE`; senão ocupa os frames livres de **menor índice** (não precisam ser contíguos).
3. **OUT**: libera todos os frames do processo.
4. **ACC** (`supportsAcc()` já devolve `true`): página = `address / pageSize`, deslocamento = `address % pageSize`, frame = tabela[página], físico = `frame * pageSize + deslocamento`. O `Simulator` já valida ID e endereço.
5. **`state()`** no formato do plano:
   ```
     Frames:  0:A 1:A 2:A 3:B 4:B 5:D 6:D 7:-
     Livres:  7   (1 frame)
     Tabela D: p0->f5  p1->f6
     Frag. interna total: 11
   ```
   (a tabela mostrada é a do processo da última linha; frag. interna = soma de `páginas * pageSize - size`).
6. **Validar** com o teste #4 (criar `tests\example-4.txt`):
   `IN(A,20) IN(B,9) IN(C,30) IN(D,16) ACC(B,8) ACC(A,22) OUT(A) IN(C,30)`, memória 64, página 8.
   Conferir: `ACC(B,8)` → físico 32; `ACC(A,22)` → `ERRO: ENDEREÇO INVÁLIDO`; o último `C` em frames 0,1,2,7.

---

## Pendências menores

- **Modo 1:** criar `tests\example-2.txt` (teste #2 do plano) — conferir no enunciado o tamanho de memória usado.
- **Etapa 5:** falta **ocupação máxima** e **fragmentação média** no resumo final (criar `Statistics.java`). Melhor fazer depois que os 3 modos existirem.

---

## Checklist rápido para quem pegar agora

- [ ] Ler a seção *Formato de saída* e as etapas 3–5 do `PLANO_TP2.md`
- [ ] Compilar e rodar `example-1.txt` nas duas políticas para ver tudo funcionando
- [ ] Escolher o modo (2 ou 3), preencher o esqueleto e validar com o teste do plano
- [ ] Commit com mensagem clara e atualizar este arquivo