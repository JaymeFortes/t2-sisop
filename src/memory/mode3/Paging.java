package memory.mode3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import memory.MemoryManager;
import memory.Result;

/**
 * Modo 3 — Paginação simples (sem memória virtual).
 *
 * A memória é dividida em frames de tamanho fixo (= tamanho da página).
 * Cada processo é dividido em páginas e ganha uma tabela de páginas
 * (página -> frame). Só é admitido se TODAS as páginas couberem.
 */
public class Paging implements MemoryManager {

    private final int memorySize;
    private final int pageSize;
    private final int frameCount;

    /** frames[i] = id do processo dono do frame i, ou null se livre. */
    private final String[] frames;

    /** Tabela de páginas de cada processo: posição = nº da página, valor = nº do frame. */
    private final Map<String, List<Integer>> pageTables = new HashMap<>();

    /** Tamanho pedido por cada processo (para a fragmentação interna). */
    private final Map<String, Integer> sizes = new HashMap<>();

    /** Processo do comando da linha atual (avisado pelo Simulator), para mostrar a tabela dele. */
    private String lastInvolved;

    /** Processo liberado nesta linha, para o estado mostrar "Tabela X: liberada". */
    private String lastReleased;

    public Paging(int memorySize, int pageSize) {
        this.memorySize = memorySize;
        this.pageSize = pageSize;
        this.frameCount = memorySize / pageSize;
        this.frames = new String[frameCount]; // todos começam null = livres
    }

    @Override
    public String name() {
        return "Paginação simples (página " + pageSize + ", " + frameCount + " frames)";
    }

    // ------------------------------------------------------------------
    // IN
    // ------------------------------------------------------------------

    @Override
    public Result in(String id, int size) {
        // Maior que a memória inteira nunca cabe. Testar antes da conta evita
        // estouro de int em (size + pageSize - 1) com tamanhos enormes.
        if (size > memorySize) {
            return Result.error(Result.NO_SPACE);
        }

        // Nº de páginas = teto(size / pageSize), feito só com inteiros
        int pagesNeeded = (size + pageSize - 1) / pageSize;

        // Sem memória virtual: precisa haver frame livre para TODAS as páginas
        List<Integer> freeFrames = freeFrames();
        if (freeFrames.size() < pagesNeeded) {
            return Result.error(Result.NO_SPACE);
        }

        // Ocupa os frames livres de menor índice (não precisam ser contíguos)
        List<Integer> table = new ArrayList<>();
        for (int page = 0; page < pagesNeeded; page++) {
            int frame = freeFrames.get(page);
            frames[frame] = id;
            table.add(frame);
        }
        pageTables.put(id, table);
        sizes.put(id, size);

        return Result.ok(id + " com " + pagesNeeded + " página" + (pagesNeeded > 1 ? "s" : "")
                + " -> frame" + (pagesNeeded > 1 ? "s " : " ") + joinNumbers(table, ", "));
    }

    /** Índices dos frames livres, em ordem crescente. */
    private List<Integer> freeFrames() {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            if (frames[i] == null) {
                free.add(i);
            }
        }
        return free;
    }

    // ------------------------------------------------------------------
    // OUT
    // ------------------------------------------------------------------

    @Override
    public Result out(String id) {
        List<Integer> table = pageTables.remove(id);
        if (table == null) {
            return Result.error(Result.PROCESS_NOT_FOUND); // o Simulator já evita isso
        }
        for (int frame : table) {
            frames[frame] = null;
        }
        sizes.remove(id);
        lastReleased = id;
        return Result.ok(id + " liberado: frame" + (table.size() > 1 ? "s " : " ")
                + joinNumbers(table, ", "));
    }

    // ------------------------------------------------------------------
    // ACC
    // ------------------------------------------------------------------

    @Override
    public boolean supportsAcc() {
        return true; // só o modo 3 aceita ACC
    }

    /** O Simulator já garantiu que o processo existe e que 0 <= address < tamanho. */
    @Override
    public Result acc(String id, int address) {
        int page = address / pageSize;
        int offset = address % pageSize;
        int frame = pageTables.get(id).get(page);
        int physical = frame * pageSize + offset;

        return Result.ok(id + " end. lógico " + address + " = página " + page
                + ", desloc. " + offset + " -> frame " + frame + " -> físico " + physical);
    }

    // ------------------------------------------------------------------
    // Estado
    // ------------------------------------------------------------------

    /** O Simulator avisa qual processo está envolvido em cada linha, mesmo quando a linha dá erro. */
    @Override
    public void setInvolved(String id) {
        lastInvolved = id;
    }

    /**
     * Mostra os frames, os livres, a tabela do processo envolvido na linha
     * (se ele estiver na memória, inclusive quando a linha deu erro, como um
     * ACC com endereço inválido; ou "liberada", se ele acabou de sair) e a
     * fragmentação interna total. Depois de impressas, as referências são
     * limpas para não vazar para a próxima linha.
     */
    @Override
    public String state() {
        // Mapa dos frames: "0:A 1:A 2:- ..."
        List<String> map = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            map.add(i + ":" + (frames[i] == null ? "-" : frames[i]));
        }

        List<Integer> free = freeFrames();
        String freeText = free.isEmpty() ? "nenhum" : joinNumbers(free, ", ");
        String freeCount = free.size() + (free.size() == 1 ? " frame" : " frames");

        int internalFragmentation = fragmentation();

        StringBuilder text = new StringBuilder();
        text.append("  Frames:  ").append(String.join(" ", map)).append("\n");
        text.append("  Livres:  ").append(freeText).append("   (").append(freeCount).append(")\n");

        // Tabela de páginas do processo envolvido
        if (lastInvolved != null && pageTables.containsKey(lastInvolved)) {
            List<Integer> table = pageTables.get(lastInvolved);
            List<String> entries = new ArrayList<>();
            for (int page = 0; page < table.size(); page++) {
                entries.add("p" + page + "->f" + table.get(page));
            }
            text.append("  Tabela ").append(lastInvolved).append(": ")
                .append(String.join("  ", entries)).append("\n");
        } else if (lastInvolved != null && lastInvolved.equals(lastReleased)) {
            // O processo acabou de sair: a tabela dele deixou de existir
            text.append("  Tabela ").append(lastInvolved).append(": liberada\n");
        }
        lastInvolved = null;
        lastReleased = null;

        text.append("  Frag. interna total: ").append(internalFragmentation);
        return text.toString();
    }

    /** Frames ocupados x tamanho da página (o frame inteiro conta, mesmo com sobra). */
    @Override
    public int usedMemory() {
        return (frameCount - freeFrames().size()) * pageSize;
    }

    /** Fragmentação interna total: sobra da última página de cada processo. */
    @Override
    public int fragmentation() {
        int total = 0;
        for (Map.Entry<String, Integer> entry : sizes.entrySet()) {
            int pages = pageTables.get(entry.getKey()).size();
            total += pages * pageSize - entry.getValue();
        }
        return total;
    }

    /** Junta uma lista de números com o separador dado: [0, 1, 2] -> "0, 1, 2". */
    private static String joinNumbers(List<Integer> numbers, String separator) {
        List<String> parts = new ArrayList<>();
        for (int n : numbers) {
            parts.add(String.valueOf(n));
        }
        return String.join(separator, parts);
    }
}