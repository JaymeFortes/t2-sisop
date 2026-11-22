package memory;

import java.util.HashMap;
import java.util.Map;

public class Simulator {

    private final Map<String, Integer> processes = new HashMap<>();

    private int admitted; // contagem para o resumo final

    public Result execute(Command command) {
        return switch (command.type()) {
            case IN -> in(command.id(), command.value());
            case OUT -> out(command.id());
            case ACC -> acc(command.id(), command.value());
        };
    }

    private Result in(String id, int size) {
        if (processes.containsKey(id)) {
            return Result.error(Result.PROCESS_EXISTS);
        }
        //perguntar ao gerenciador de memória se cabe
        processes.put(id, size);
        admitted++;
        return Result.ok(id + " admitido (" + size + ")");
    }

    private Result out(String id) {
        Integer size = processes.remove(id); // remove e devolve o tamanho, ou null
        if (size == null) {
            return Result.error(Result.PROCESS_NOT_FOUND);
        }
        return Result.ok(id + " liberado (" + size + ")");
    }

    private Result acc(String id, int address) {
        Integer size = processes.get(id);
        if (size == null) {
            return Result.error(Result.PROCESS_NOT_FOUND);
        }
        // Endereços válidos vão de 0 até size-1
        if (address >= size) {
            return Result.error(Result.INVALID_ADDRESS);
        }
        //traduzir para endereço físico
        return Result.ok(id + " acesso ao endereço " + address);
    }

    public int admitted() {
        return admitted;
    }
}