package memory.mode1;

import java.util.ArrayList;
import java.util.List;

import memory.MemoryManager;
import memory.Result;


//Modo 1 — Partições variáveis.
 //A memória é uma lista de blocos contíguos, em ordem de endereço.
 //Cada IN corta um bloco livre no tamanho exato; cada OUT libera e funde vizinhos livres.
public class VariablePartitions implements MemoryManager {

    public enum Policy { WORST_FIT, CIRCULAR_FIT }

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

        int end() { return start + size - 1; }
    }

    private final int memorySize;
    private final Policy policy;
    private final List<Block> blocks = new ArrayList<>(); // sempre ordenada por endereço

    //Circular-Fit: endereço logo após a última partição alocada. Começa em 0.
    private int pointer = 0;

    public VariablePartitions(int memorySize, Policy policy) {
        this.memorySize = memorySize;
        this.policy = policy;
        blocks.add(new Block(0, memorySize, null)); // começa como um único bloco livre
    }

    @Override
    public String name() {
        return "Partições variáveis (" + (policy == Policy.WORST_FIT ? "Worst-Fit" : "Circular-Fit") + ")";
    }

    @Override
    public Result in(String id, int size) {
        int index = (policy == Policy.WORST_FIT) ? findWorstFit(size) : findCircularFit(size);
        if (index < 0) {
            return Result.error(Result.NO_SPACE);
        }

        Block chosen = blocks.get(index);
        int leftover = chosen.size - size;

        chosen.id = id;
        chosen.size = size;

        // A sobra vira um bloco livre logo depois
        if (leftover > 0) {
            blocks.add(index + 1, new Block(chosen.start + size, leftover, null));
        }

        // Circular-Fit: o ponteiro vai para logo depois da partição criada
        // (volta a 0 se chegou ao fim da memória)
        pointer = chosen.start + size;
        if (pointer == memorySize) {
            pointer = 0;
        }
        return Result.ok(id + " alocado em [" + chosen.start + "-" + chosen.end() + "] (" + size + ")");
    }

    // Worst-Fit: o MAIOR bloco livre que comporte o processo.
    // Empate: fica o de menor endereço (o primeiro encontrado, por isso o ">" estrito).
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

    // Circular-Fit: começa no primeiro bloco com início >= ponteiro e segue em ordem
    // de endereço; ao chegar no fim, volta ao começo da lista. Fica com o PRIMEIRO que couber.
    // A busca é circular, a memória não: blocos do fim e do início nunca se juntam.    
    private int findCircularFit(int size) {
        int n = blocks.size();

        // Onde a busca começa: primeiro bloco com início >= ponteiro
        int startIndex = 0;
        for (int i = 0; i < n; i++) {
            if (blocks.get(i).start >= pointer) {
                startIndex = i;
                break;
            }
        }

        for (int k = 0; k < n; k++) {
            int i = (startIndex + k) % n;
            Block b = blocks.get(i);
            if (b.isFree() && b.size >= size) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Result out(String id) {
        int index = indexOf(id);
        if (index < 0) {
            return Result.error(Result.PROCESS_NOT_FOUND); 
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

    private int indexOf(String id) {
        for (int i = 0; i < blocks.size(); i++) {
            if (id.equals(blocks.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    // Soma das partições ocupadas.
    @Override
    public int usedMemory() {
        int used = 0;
        for (Block b : blocks) {
            if (!b.isFree()) {
                used += b.size;
            }
        }
        return used;
    }

    // Fragmentação externa: memória livre que está fora do maior bloco livre
    // (total livre - maior bloco livre). 
    // sem servir para um processo do tamanho do total livre. Zero quando todo o
    // espaço livre está num bloco só.
    @Override
    public int fragmentation() {
        int totalFree = 0;
        int largestFree = 0;
        for (Block b : blocks) {
            if (b.isFree()) {
                totalFree += b.size;
                largestFree = Math.max(largestFree, b.size);
            }
        }
        return totalFree - largestFree;
    }

    @Override
    public String fragmentationType() {
        return "externa";
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
        String text = "  Memória: " + String.join(" | ", regions) + "\n"
                    + "  Livres:  " + freeList + "   (total " + totalFree + ", maior " + largestFree + ")";
        if (policy == Policy.CIRCULAR_FIT) {
            text += "\n  Ponteiro: " + pointer;
        }
        return text;
    }
}