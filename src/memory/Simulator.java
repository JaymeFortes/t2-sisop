package memory;

import java.util.HashMap;
import java.util.Map;

/**
 * Executa os comandos e trata os erros comuns aos três modos.
 * A alocação em si é repassada ao MemoryManager do modo escolhido.
 */
public class Simulator {

    private final MemoryManager manager;
    private final Map<String, Integer> processes = new HashMap<>(); // id: tamanho

    private int admitted;
    private int rejected;

    public Simulator(MemoryManager manager) {
        this.manager = manager;
    }

    public Result execute(Command command) {
        manager.setInvolved(command.id());

        return switch (command.type()) {
            case IN  -> in(command.id(), command.value());
            case OUT -> out(command.id());
            case ACC -> acc(command.id(), command.value());
        };
    }

    private Result in(String id, int size) {
        if (processes.containsKey(id)) {
            return Result.error(Result.PROCESS_EXISTS);
        }
        Result result = manager.in(id, size);
        if (result.ok()) {
            processes.put(id, size);
            admitted++;
        } else if (Result.NO_SPACE.equals(result.message())) {
            rejected++; 
        }
        return result;
    }

    private Result out(String id) {
        if (!processes.containsKey(id)) {
            return Result.error(Result.PROCESS_NOT_FOUND);
        }
        Result result = manager.out(id);
        if (result.ok()) {
            processes.remove(id);
        }
        return result;
    }

    private Result acc(String id, int address) {
        if (!manager.supportsAcc()) {
            return Result.ok("AVISO: ACC ignorado neste modo");
        }
        Integer size = processes.get(id);
        if (size == null) {
            return Result.error(Result.PROCESS_NOT_FOUND);
        }
        if (address < 0 || address >= size) {
            return Result.error(Result.INVALID_ADDRESS);
        }
        return manager.acc(id, address);
    }

    public MemoryManager manager() { return manager; }
    public int admitted() { return admitted; }
    public int rejected() { return rejected; }
}