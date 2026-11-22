package memory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import memory.mode1.VariablePartitions;

public class Main {

    public static void main(String[] args) throws IOException {
        // Sem argumento: mostra como usar e encerra
        if (args.length < 1) {
            System.err.println("Uso: java -cp out memory.Main <arquivo>");
            System.exit(1);
        }

        Path file = Path.of(args[0]);
        if (!Files.isReadable(file)) {
            System.err.println("Erro: arquivo não encontrado: " + file);
            System.exit(1);
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        // Gerenciador do modo escolhido (por enquanto fixo: modo 1, memória 32)
        int memorySize = 32;
        MemoryManager manager = new VariablePartitions(memorySize);

        // Trata os erros comuns e repassa a alocação ao gerenciador
        Simulator simulator = new Simulator(manager);

        // Cabeçalho e estado inicial da memória
        System.out.println("=== Modo: " + manager.name() + " - memória " + memorySize + " ===");
        System.out.println("[00] início");
        System.out.println(manager.state());

        int lineNumber = 0;
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue; // linha em branco: não conta na numeração
            }
            lineNumber++;
            System.out.println();
            System.out.printf("[%02d] %s%n", lineNumber, line);

            // Transforma o texto em comando (IN/OUT/ACC + id + valor)
            Command command = Parser.parse(line);
            if (command == null) {
                System.out.println("  -> LINHA INVÁLIDA (ignorada)");
                continue;
            }

            // Executa o comando, mostra o resultado e o estado da memória
            Result result = simulator.execute(command);
            System.out.println("  -> " + result.formatted());
            System.out.println(manager.state());
        }

        // Resumo parcial (ocupação e fragmentação entram depois)
        System.out.println();
        System.out.println("=== RESUMO ===");
        System.out.println("Processos admitidos:            " + simulator.admitted());
        System.out.println("Rejeitados por falta de espaço: " + simulator.rejected());
    }
}