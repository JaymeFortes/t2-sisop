package memory;

public record Command(Type type, String id, int value) {

    public enum Type { IN, OUT, ACC }
}