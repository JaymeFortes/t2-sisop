package memory;

/**
 * Interface comum dos três modos. O Simulator já filtrou os erros comuns
 * (ID repetido, ID inexistente, endereço inválido); cada modo só cuida da alocação.
 */
public interface MemoryManager {

    /** Nome exibido no cabeçalho, ex.: "Partições variáveis (Worst-Fit)". */
    String name();

    /** Tenta alocar. Devolve ok ou erro(Result.NO_SPACE). O id ainda não existe. */
    Result in(String id, int size);

    /** Libera toda a memória do processo. O id com certeza existe. */
    Result out(String id);

    /** Só o modo 3 aceita ACC; nos outros o comando é ignorado com aviso. */
    default boolean supportsAcc() { return false; }

    /** Traduz endereço lógico em físico (só modo 3). */
    default Result acc(String id, int address) {
        throw new UnsupportedOperationException("ACC só existe no modo 3");
    }

    /** Estado da memória já formatado; cada linha começa com dois espaços. */
    String state();

    /**
     * Avisa qual processo está envolvido no comando da linha atual.
     * Chamado pelo Simulator antes de qualquer validação. Só o modo 3 usa
     * (para mostrar a tabela de páginas desse processo); os outros ignoram.
     */
    default void setInvolved(String id) { }
}