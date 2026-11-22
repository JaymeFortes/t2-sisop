package memory;

/**
 * Um comando já interpretado.
 * valor = tamanho (IN) ou endereço (ACC); 0 no OUT.
 */
public record Command(Type type, String id, int value) {

    public enum Type { IN, OUT, ACC }
}