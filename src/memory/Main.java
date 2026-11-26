package memory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import memory.mode1.VariablePartitions;
import memory.mode2.Buddy;
import memory.mode3.Paging;

public class Main {

    private static final BufferedReader INPUT =
            new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseArgs(args);

        // 1) Modo
        int mode = Integer.parseInt(ask(options, "mode",
                "Modo (1 = partições variáveis, 2 = buddy, 3 = paginação): ",
                value -> value.matches("[123]"),
                "o modo deve ser 1, 2 ou 3"));

        // 2) Tamanho da memória
        int memorySize = Integer.parseInt(ask(options, "mem",
                "Tamanho da memória (potência de 2): ",
                Main::isPowerOfTwo,
                "o tamanho da memória deve ser uma potência de 2"));

        // 3) Parâmetros específicos de cada modo e criação do gerenciador
        MemoryManager manager = switch (mode) {
            case 1 -> {
                boolean policyFromArgument = options.containsKey("policy");
                String policy = ask(options, "policy",
                        "Política (worst (1) | circular (2)): ",
                        value -> policyFromArgument
                                ? value.equalsIgnoreCase("worst") || value.equalsIgnoreCase("circular")
                                : value.matches("[12]"),
                        policyFromArgument ? "a política deve ser worst ou circular" : "responda 1 ou 2");
                yield new VariablePartitions(memorySize, policy.equals("1") || policy.equalsIgnoreCase("worst")
                        ? VariablePartitions.Policy.WORST_FIT
                        : VariablePartitions.Policy.CIRCULAR_FIT);
            }
            case 2 -> new Buddy(memorySize);
            default -> {
                int pageSize = Integer.parseInt(ask(options, "page",
                        "Tamanho da página (potência de 2, até " + memorySize + "): ",
                        value -> isPowerOfTwo(value) && Integer.parseInt(value.strip()) <= memorySize,
                        "a página deve ser potência de 2 e no máximo o tamanho da memória"));
                yield new Paging(memorySize, pageSize);
            }
        };

        // 4) Arquivo de requisições
        String filePath = ask(options, "file",
                "Arquivo de requisições (ex.: example-1): ",
                Main::isReadableFile,
                "arquivo não encontrado ou não é um arquivo comum");

        // 5) Execução passo a passo ou contínua
        boolean stepByStep;
        if (options.containsKey("step")) {
            stepByStep = true;
        } else if (options.containsKey("continuous")) {
            stepByStep = false;
        } else {
            stepByStep = ask(options, "execution",
                    "Passo a passo? (s/n): ",
                    value -> value.matches("[sSnN]"),
                    "responda s ou n").equalsIgnoreCase("s");
        }

        run(manager, memorySize, resolveRequestFile(filePath), stepByStep);
    }

    /** Laço principal: lê cada linha, executa e imprime resultado + estado. */
    private static void run(MemoryManager manager, int memorySize, Path file, boolean stepByStep)
            throws IOException {
        List<String> lines = readLines(file);
        Simulator simulator = new Simulator(manager);
        Statistics statistics = new Statistics(memorySize); // ocupação máxima e fragmentação média

        System.out.println("=== Modo: " + manager.name() + " - memória " + memorySize + " ===");
        System.out.println("[00] início");
        System.out.println(manager.state());

        int lineNumber = 0;
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            lineNumber++;
            System.out.println();
            System.out.printf("[%02d] %s%n", lineNumber, line);

            Command command = Parser.parse(line);
            if (command == null) {
                System.out.println("  -> LINHA INVÁLIDA (ignorada)");
            } else {
                Result result = simulator.execute(command);
                System.out.println("  -> " + result.formatted());
            }

            System.out.println(manager.state());

            if (command != null) {
                statistics.record(manager.usedMemory(), manager.fragmentation());
            }

            // Passo a passo: espera o Enter antes da próxima linha
            if (stepByStep) {
                System.out.print("  (Enter para continuar)");
                if (INPUT.readLine() == null) {
                    stepByStep = false; // entrada acabou
                }
            }
        }

        // Resumo final
        System.out.println();
        System.out.println("=== RESUMO ===");
        System.out.println("Processos admitidos:            " + simulator.admitted());
        System.out.println("Rejeitados por falta de espaço: " + simulator.rejected());
        System.out.println("Ocupação máxima:                " + statistics.formattedMaxOccupation());
        System.out.println("Fragmentação média:             "
                + statistics.formattedAverageFragmentation(manager.fragmentationType()));
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            try {
                return Files.readAllLines(file, StandardCharsets.ISO_8859_1);
            } catch (IOException e2) {
                fatal("não foi possível ler o arquivo: " + file);
            }
        } catch (IOException e) {
            fatal("não foi possível ler o arquivo: " + file);
        }
        return List.of(); // não chega aqui: fatal() encerra o programa
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--step") || arg.equals("--continuous")) {
                options.put(arg.substring(2), "");
            } else if (arg.startsWith("--")) {
                if (i + 1 >= args.length) {
                    fatal("o parâmetro " + arg + " precisa de um valor");
                }
                options.put(arg.substring(2), args[++i]);
            } else {
                options.put("file", arg);
            }
        }
        return options;
    }

    private static String ask(Map<String, String> options, String key, String question,
                              Predicate<String> isValid, String errorMessage) throws IOException {
        String value = options.get(key);
        if (value != null) {
            value = value.strip();
            if (!isValid.test(value)) {
                fatal(errorMessage + " (recebido: " + value + ")");
            }
            return value;
        }
        while (true) {
            System.out.print(question);
            String typed = INPUT.readLine();
            if (typed == null) {
                fatal("entrada encerrada antes de informar todos os parâmetros");
            }
            typed = typed.strip();
            if (isValid.test(typed)) {
                return typed;
            }
            System.out.println("  Valor inválido: " + errorMessage + ".");
        }
    }

    // verifica se é potencia de 2
    static boolean isPowerOfTwo(String value) {
        try {
            int n = Integer.parseInt(value.strip());
            return n > 0 && (n & (n - 1)) == 0;
        } catch (NumberFormatException e) {
            return false; // não é número, ou não cabe em int
        }
    }

    private static Path resolveRequestFile(String value) {
        Path path = Path.of(value);
        if (path.getRoot() != null || path.getParent() != null || Files.exists(path)) {
            return path;
        }

        Path testFile = Path.of("tests", value);
        if (!value.contains(".") && !Files.exists(testFile)) {
            return testFile.resolveSibling(value + ".txt");
        }
        return testFile;
    }

    // verifica se é um arquivo legível
    private static boolean isReadableFile(String value) {
        try {
            Path path = resolveRequestFile(value);
            return Files.isRegularFile(path) && Files.isReadable(path);
        } catch (InvalidPathException e) {
            return false; // caminho com caracteres inválidos
        }
    }

    private static void fatal(String message) {
        System.err.println("Erro: " + message);
        System.exit(1);
    }
}