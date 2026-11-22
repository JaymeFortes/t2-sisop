package memory.mode1;

import java.util.ArrayList;
import java.util.List;

import memory.MemoryManager;
import memory.Result;

/**
 * Modo 1 — Partições variáveis.
 * A memória é uma lista de blocos contíguos, em ordem de endereço.
 * Cada IN corta um bloco livre no tamanho exato; cada OUT libera e funde vizinhos livres.
 */
public class VariablePartitions implements MemoryManager {

    /** Um pedaço contíguo da memória: livre (id == null) ou ocupado por um processo. */
    private static class Block {
        int start;
        int size;
        String id;

        Block(int start, int size, String id) {
            this.start = start;
            this.size = size;
            this.id = id;
        }

        boolean isFree() { return id == null; }

        /** Último endereço do bloco (inclusive). */
        int end() { return start + size - 1; }
    }

    private final int memorySize;
    private final List<Block> blocks = new ArrayList<>(); // sempre ordenada por endereço

    public VariablePartitions(int memorySize) {
        this.memorySize = memorySize;
        blocks.add(new Block(0, memorySize, null)); // começa como um único bloco livre
    }

    @Override
    public String name() {
        return "Partições variáveis (Worst-Fit)";
    }

    @Override
    public Result in(String id, int size) {
        int index = findWorstFit(size);
        if (index < 0) {
            return Result.error(Result.NO_SPACE);
        }

        Block chosen = blocks.get(index);
        int leftover = chosen.size - size;

        // A partição fica no início do bloco escolhido
        chosen.id = id;
        chosen.size = size;

        // A sobra vira um bloco livre logo depois
        if (leftover > 0) {
            blocks.add(index + 1, new Block(chosen.start + size, leftover, null));
        }
        return Result.ok(id + " alocado em [" + chosen.start + "-" + chosen.end() + "] (" + size + ")");
    }

    /**
     * Worst-Fit: o MAIOR bloco livre que comporte o processo.
     * Empate: fica o de menor endereço (o primeiro encontrado, por isso o ">" estrito).
     * @return índice do bloco na lista, ou -1 se nenhum couber
     */
    private int findWorstFit(int size) {
        int best = -1;
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            if (b.isFree() && b.size >= size
                    && (best == -1 || b.size > blocks.get(best).size)) {
                best = i;
            }
        }
        return best;
    }

    @Override
    public Result out(String id) {
        int index = indexOf(id);
        if (index < 0) {
            return Result.error(Result.PROCESS_NOT_FOUND); // o Simulator já evita isso
        }

        Block block = blocks.get(index);
        String message = id + " liberado de [" + block.start + "-" + block.end() + "] (" + block.size + ")";
        block.id = null;

        // Funde com o vizinho da direita, se estiver livre
        if (index + 1 < blocks.size() && blocks.get(index + 1).isFree()) {
            block.size += blocks.get(index + 1).size;
            blocks.remove(index + 1);
        }
        // Funde com o vizinho da esquerda, se estiver livre
        if (index > 0 && blocks.get(index - 1).isFree()) {
            blocks.get(index - 1).size += block.size;
            blocks.remove(index);
        }
        return Result.ok(message);
    }

    /** Posição na lista do bloco ocupado pelo processo, ou -1. */
    private int indexOf(String id) {
        for (int i = 0; i < blocks.size(); i++) {
            if (id.equals(blocks.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String state() {
        List<String> regions = new ArrayList<>();
        List<String> freeSizes = new ArrayList<>();
        int totalFree = 0;
        int largestFree = 0;

        for (Block b : blocks) {
            String range = "[" + b.start + "-" + b.end() + "] ";
            if (b.isFree()) {
                regions.add(range + "livre(" + b.size + ")");
                freeSizes.add(String.valueOf(b.size));
                totalFree += b.size;
                largestFree = Math.max(largestFree, b.size);
            } else {
                regions.add(range + b.id + "(" + b.size + ")");
            }
        }

        String freeList = freeSizes.isEmpty() ? "nenhum" : String.join(" | ", freeSizes);
        return "  Memória: " + String.join(" | ", regions) + "\n"
             + "  Livres:  " + freeList + "   (total " + totalFree + ", maior " + largestFree + ")";
    }
}