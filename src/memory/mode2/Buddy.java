package memory.mode2;

import memory.MemoryManager;
import memory.Result;

/**
 * Modo 2 — Sistema buddy.
 * Esqueleto: só existe para o Main compilar. A lógica entra na Etapa 3 do plano.
 */
public class Buddy implements MemoryManager {

    private final int memorySize;

    public Buddy(int memorySize) {
        this.memorySize = memorySize;
    }

    @Override
    public String name() {
        return "Sistema buddy";
    }

    @Override
    public Result in(String id, int size) {
        return Result.error("NÃO IMPLEMENTADO"); // TODO Etapa 3
    }

    @Override
    public Result out(String id) {
        return Result.error("NÃO IMPLEMENTADO"); // TODO Etapa 3
    }

    @Override
    public String state() {
        return "  (modo 2 ainda não implementado - memória " + memorySize + ")";
    }
}