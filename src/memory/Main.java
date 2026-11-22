package memory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

/**
 * Ponto de entrada do simulador.
 *
 * Uso:
 *   java -cp out memory.Main [arquivo] [--mode 1|2|3] [--mem N]
 *        [--policy worst|circular] [--page N] [--step | --continuous]
 *
 * Qualquer parâmetro que não vier por argumento é perguntado no terminal.
 */
public class Main {

    /** Um único leitor do teclado, usado nas perguntas e no "Enter para continuar". */
    private static final BufferedReader INPUT =
            new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseArgs(args);

        // 1) Modo
        int mode = Integer.parseInt(ask(options, "mode",
                "Modo (1 = partições variáveis, 2 = buddy, 3 = paginação): ",
                value -> value.matches("[123]"),
                "o modo deve ser 1, 2 ou 3"));

        // 2) Tamanho da memória (obrigatório, potência de 2)
        int memorySize = Integer.parseInt(ask(options, "mem",
                "Tamanho da memória (potência de 2): ",
                Main::isPowerOfTwo,
                "o tamanho da memória deve ser uma potência de 2"));

        // 3) Parâmetros específicos de cada modo e criação do gerenciador
        MemoryManager manager = switch (mode) {
            case 1 -> {
                String policy = ask(options, "policy",
                        "Política (worst | circular): ",
                        value -> value.equalsIgnoreCase("worst") || value.equalsIgnoreCase("circular"),
                        "a política deve ser worst ou circular");
                yield new VariablePartitions(memorySize, policy.equalsIgnoreCase("worst")
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
                "Arquivo de requisições: ",
                Main::isReadableFile,
                "arquivo não encontrado");

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

        run(manager, memorySize, Path.of(filePath), stepByStep);
    }

    /** Laço principal: lê cada linha, executa e imprime resultado + estado. */
    private static void run(MemoryManager manager, int memorySize, Path file, boolean stepByStep)
            throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
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
            } else {
                // Executa o comando, mostra o resultado e o estado da memória
                Result result = simulator.execute(command);
                System.out.println("  -> " + result.formatted());
                System.out.println(manager.state());
            }

            // Passo a passo: espera o Enter antes da próxima linha
            if (stepByStep) {
                System.out.print("  (Enter para continuar)");
                if (INPUT.readLine() == null) {
                    stepByStep = false; // entrada acabou (ex.: redirecionada): segue contínuo
                }
            }
        }

        // Resumo parcial (ocupação máxima e fragmentação média entram na Etapa 5)
        System.out.println();
        System.out.println("=== RESUMO ===");
        System.out.println("Processos admitidos:            " + simulator.admitted());
        System.out.println("Rejeitados por falta de espaço: " + simulator.rejected());
    }

    // ------------------------------------------------------------------
    // Leitura dos parâmetros
    // ------------------------------------------------------------------

    /**
     * Separa os argumentos em um mapa:
     *   "--mode 1"      -> mode = 1
     *   "--step"        -> step = "" (flag sem valor)
     *   "teste.txt"     -> file = teste.txt (argumento solto)
     */
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

    /**
     * Devolve o valor do parâmetro. Se veio por argumento, só valida
     * (inválido = encerra com erro). Se não veio, pergunta no terminal
     * até receber um valor válido.
     */
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

    /** Potência de 2: exatamente um bit ligado (8 = 1000, 8-1 = 0111, 8 & 7 = 0). */
    static boolean isPowerOfTwo(String value) {
        try {
            int n = Integer.parseInt(value.strip());
            return n > 0 && (n & (n - 1)) == 0;
        } catch (NumberFormatException e) {
            return false; // não é número, ou não cabe em int
        }
    }

    private static boolean isReadableFile(String value) {
        try {
            return Files.isReadable(Path.of(value));
        } catch (InvalidPathException e) {
            return false; // caminho com caracteres inválidos
        }
    }

    private static void fatal(String message) {
        System.err.println("Erro: " + message);
        System.exit(1);
    }
}