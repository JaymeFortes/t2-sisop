package memory;

/**
 * Interface comum dos três modos. O Simulator já filtrou os erros comuns
 */
public interface MemoryManager {

    String name();

    Result in(String id, int size);

    Result out(String id);

    default boolean supportsAcc() { return false; }

    default Result acc(String id, int address) {
        throw new UnsupportedOperationException("ACC só existe no modo 3");
    }

    String state();

    int usedMemory();

    int fragmentation();

    default String fragmentationType() { return "interna"; }

    default void setInvolved(String id) { }
}