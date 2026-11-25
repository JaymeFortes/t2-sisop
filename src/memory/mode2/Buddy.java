package memory.mode2;

import java.util.ArrayList;
import java.util.List;

import memory.MemoryManager;
import memory.Result;

/**
 * Modo 2 — Sistema buddy.
 *
 * A memória é uma árvore binária: a raiz é a memória inteira; um nó dividido
 * tem dois filhos com metade do tamanho cada (os "buddies"). Só as folhas são
 * blocos de verdade, e cada folha está livre ou alocada para um processo.
 *
 * IN:  arredonda o pedido para potência de 2, pega o menor bloco livre que caiba
 *      e divide ao meio até chegar no tamanho; aloca sempre a metade da esquerda.
 * OUT: libera o bloco e funde com o buddy enquanto o buddy também estiver livre.
 */
public class Buddy implements MemoryManager {

    /** Um nó da árvore. Dividido = tem filhos; folha = livre (id null) ou alocada. */
    private static class Node {
        final int start;
        final int size;
        Node left;       // filho da esquerda (metade de menor endereço)
        Node right;      // filho da direita
        String id;       // processo dono (só em folha alocada)
        int requested;   // tamanho que o processo pediu (para a fragmentação interna)

        Node(int start, int size) {
            this.start = start;
            this.size = size;
        }

        boolean isSplit() { return left != null; }

        boolean isFreeLeaf() { return !isSplit() && id == null; }

        /** Último endereço do bloco (inclusive). */
        int end() { return start + size - 1; }
    }

    private final int memorySize;
    private final Node root;

    public Buddy(int memorySize) {
        this.memorySize = memorySize;
        this.root = new Node(0, memorySize); // começa como um único bloco livre
    }

    @Override
    public String name() {
        return "Sistema buddy";
    }

    // ------------------------------------------------------------------
    // IN
    // ------------------------------------------------------------------

    @Override
    public Result in(String id, int size) {
        if (size > memorySize) {
            return Result.error(Result.NO_SPACE); // nem a memória inteira comporta
        }
        int blockSize = nextPowerOfTwo(size);

        Node node = findSmallestFree(root, blockSize, null);
        if (node == null) {
            return Result.error(Result.NO_SPACE);
        }

        // Divide ao meio até chegar no tamanho; segue sempre pela esquerda
        while (node.size > blockSize) {
            int half = node.size / 2;
            node.left = new Node(node.start, half);
            node.right = new Node(node.start + half, half);
            node = node.left;
        }

        node.id = id;
        node.requested = size;
        return Result.ok(id + " alocado em [" + node.start + "-" + node.end()
                + "] (bloco " + node.size + ", pedido " + size + ")");
    }

    /** Menor potência de 2 maior ou igual a n (5 -> 8, 16 -> 16, 20 -> 32). */
    private static int nextPowerOfTwo(int n) {
        int p = 1;
        while (p < n) {
            p *= 2;
        }
        return p;
    }

    /**
     * Procura, entre as folhas livres, a MENOR que comporte blockSize.
     * Percorre da esquerda para a direita (ordem de endereço); em empate
     * fica a primeira encontrada, ou seja, a de menor endereço ("<" estrito).
     */
    private Node findSmallestFree(Node node, int blockSize, Node best) {
        if (node.isSplit()) {
            best = findSmallestFree(node.left, blockSize, best);
            return findSmallestFree(node.right, blockSize, best);
        }
        if (node.isFreeLeaf() && node.size >= blockSize
                && (best == null || node.size < best.size)) {
            return node;
        }
        return best;
    }

    // ------------------------------------------------------------------
    // OUT
    // ------------------------------------------------------------------

    @Override
    public Result out(String id) {
        Node leaf = findAllocated(root, id);
        if (leaf == null) {
            return Result.error(Result.PROCESS_NOT_FOUND); // o Simulator já evita isso
        }
        String message = id + " liberado de [" + leaf.start + "-" + leaf.end()
                + "] (bloco " + leaf.size + ")";

        leaf.id = null;
        leaf.requested = 0;
        coalesce(root);

        // Mostra em qual bloco livre o espaço terminou depois das fusões
        Node merged = leafContaining(root, leaf.start);
        if (merged.size > leaf.size) {
            message += "; fundido em [" + merged.start + "-" + merged.end() + "] (" + merged.size + ")";
        }
        return Result.ok(message);
    }

    /** Folha alocada para o processo, ou null. */
    private Node findAllocated(Node node, String id) {
        if (node.isSplit()) {
            Node found = findAllocated(node.left, id);
            return (found != null) ? found : findAllocated(node.right, id);
        }
        return id.equals(node.id) ? node : null;
    }

    /**
     * Funde buddies livres, de baixo para cima: primeiro resolve os filhos;
     * se os dois viraram folhas livres, o nó deixa de ser dividido.
     * Como a recursão volta subindo, a fusão se repete enquanto o buddy estiver livre.
     */
    private void coalesce(Node node) {
        if (!node.isSplit()) {
            return;
        }
        coalesce(node.left);
        coalesce(node.right);
        if (node.left.isFreeLeaf() && node.right.isFreeLeaf()) {
            node.left = null;
            node.right = null;
        }
    }

    /** Folha que contém o endereço. */
    private Node leafContaining(Node node, int address) {
        while (node.isSplit()) {
            node = (address < node.right.start) ? node.left : node.right;
        }
        return node;
    }

    // ------------------------------------------------------------------
    // Estado
    // ------------------------------------------------------------------

    @Override
    public String state() {
        List<Node> leaves = new ArrayList<>();
        collectLeaves(root, leaves);

        List<String> regions = new ArrayList<>();
        List<String> freeSizes = new ArrayList<>();
        int totalFree = 0;
        int internalFragmentation = 0;

        for (Node leaf : leaves) {
            String range = "[" + leaf.start + "-" + leaf.end() + "] ";
            if (leaf.id == null) {
                regions.add(range + "livre(" + leaf.size + ")");
                freeSizes.add(String.valueOf(leaf.size));
                totalFree += leaf.size;
            } else {
                // Ocupado: ID(pedido/bloco)
                regions.add(range + leaf.id + "(" + leaf.requested + "/" + leaf.size + ")");
                internalFragmentation += leaf.size - leaf.requested;
            }
        }

        String freeList = freeSizes.isEmpty() ? "nenhum" : String.join(" | ", freeSizes);
        return "  Memória: " + String.join(" | ", regions) + "\n"
             + "  Livres:  " + freeList + "   (total " + totalFree + ")\n"
             + "  Frag. interna total: " + internalFragmentation;
    }

    /** Folhas em ordem de endereço (esquerda antes da direita). */
    private void collectLeaves(Node node, List<Node> leaves) {
        if (node.isSplit()) {
            collectLeaves(node.left, leaves);
            collectLeaves(node.right, leaves);
        } else {
            leaves.add(node);
        }
    }
}