package memory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

        // Guarda os processos e trata os erros comuns aos três modos
        Simulator simulator = new Simulator();

        int lineNumber = 0;
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue; // linha em branco: não conta na numeração
            }
            lineNumber++;
            System.out.printf("[%02d] %s%n", lineNumber, line);

            // Transforma o texto em comando (IN/OUT/ACC + id + valor)
            Command command = Parser.parse(line);
            if (command == null) {
                System.out.println("  -> LINHA INVÁLIDA (ignorada)");
                continue;
            }

            // Executa e mostra o resultado
            Result result = simulator.execute(command);
            System.out.println("  -> " + result.formatted());
        }
    }
}