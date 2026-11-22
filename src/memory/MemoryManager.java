package memory;

// interface que faz a ponte entre o simulador e os três modos de gerenciamento de memória
public interface MemoryManager {

    //Nome exibido no cabeçalho, ex.: "Partições variáveis (Worst-Fit)".
    String name();

    //tenta alocar. Devolve ok ou erro
    Result in(String id, int size);

    // Libera toda a memória do processo
    Result out(String id);

    /** Só o modo 3 aceita ACC; nos outros o comando é ignorado com aviso. */
    default boolean supportsAcc() { return false; }

    /** Traduz endereço lógico em físico (só modo 3). */
    default Result acc(String id, int address) {
        throw new UnsupportedOperationException("ACC só existe no modo 3");
    }

    /** Estado da memória já formatado; cada linha começa com dois espaços. */
    String state();
}