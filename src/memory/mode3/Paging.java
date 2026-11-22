package memory.mode3;

import memory.MemoryManager;
import memory.Result;

/**
 * Modo 3 — Paginação simples (sem memória virtual).
 * Esqueleto: só existe para o Main compilar. A lógica entra na Etapa 4 do plano.
 */
public class Paging implements MemoryManager {

    private final int memorySize;
    private final int pageSize;
    private final int frameCount;

    public Paging(int memorySize, int pageSize) {
        this.memorySize = memorySize;
        this.pageSize = pageSize;
        this.frameCount = memorySize / pageSize;
    }

    @Override
    public String name() {
        return "Paginação simples (página " + pageSize + ", " + frameCount + " frames)";
    }

    @Override
    public Result in(String id, int size) {
        return Result.error("NÃO IMPLEMENTADO"); // TODO Etapa 4
    }

    @Override
    public Result out(String id) {
        return Result.error("NÃO IMPLEMENTADO"); // TODO Etapa 4
    }

    @Override
    public boolean supportsAcc() {
        return true; // só o modo 3 aceita ACC
    }

    @Override
    public Result acc(String id, int address) {
        return Result.error("NÃO IMPLEMENTADO"); // TODO Etapa 4
    }

    @Override
    public String state() {
        return "  (modo 3 ainda não implementado - memória " + memorySize + ")";
    }
}