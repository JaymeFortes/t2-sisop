package memory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Parser {

    private static final Pattern IN_PATTERN = Pattern.compile(
            "^IN\\(\\s*(\\w+)\\s*,\\s*(\\d+)\\s*\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern OUT_PATTERN = Pattern.compile(
            "^OUT\\(\\s*(\\w+)\\s*\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACC_PATTERN = Pattern.compile(
            "^ACC\\(\\s*(\\w+)\\s*,\\s*(-?\\d+)\\s*\\)$", Pattern.CASE_INSENSITIVE);

    private Parser() {}

    public static Command parse(String line) {
        try {
            Matcher m = IN_PATTERN.matcher(line);
            if (m.matches()) {
                int size = Integer.parseInt(m.group(2));
                return size > 0 ? new Command(Command.Type.IN, m.group(1), size) : null;
            }

            m = OUT_PATTERN.matcher(line);
            if (m.matches()) {
                return new Command(Command.Type.OUT, m.group(1), 0);
            }

            m = ACC_PATTERN.matcher(line);
            if (m.matches()) {
                int address = Integer.parseInt(m.group(2));
                return new Command(Command.Type.ACC, m.group(1), address);
            }
        } catch (NumberFormatException e) {
            // Número grande demais para int
        }
        return null;
    }
}