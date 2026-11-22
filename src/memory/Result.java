package memory;

public record Result(boolean ok, String message) {

    public static final String NO_SPACE = "ESPAÇO INSUFICIENTE DE MEMÓRIA";
    public static final String PROCESS_EXISTS = "ERRO: PROCESSO JÁ EXISTE";
    public static final String PROCESS_NOT_FOUND = "ERRO: PROCESSO NÃO ENCONTRADO";
    public static final String INVALID_ADDRESS = "ERRO: ENDEREÇO INVÁLIDO";

    public static Result ok(String message) {
        return new Result(true, message);
    }

    public static Result error(String message) {
        return new Result(false, message);
    }

    public String formatted() {
        return ok ? "OK: " + message : message;
    }
}