package memory;

import java.util.Locale;

/**
 * Acumula os números do resumo final que dependem da execução inteira:
 * a ocupação máxima da memória e a fragmentação média.
 *
 * O Main chama record(...) depois de cada comando executado (inclusive os que
 * deram erro, porque a memória continua lá ocupando espaço naquela linha).
 * Linhas inválidas e linhas em branco não entram na conta.
 */
public class Statistics {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final int memorySize;

    private int maxUsed;          // maior ocupação vista até agora
    private long fragmentationSum; // soma das fragmentações (long: evita estouro em arquivos longos)
    private int samples;          // quantas linhas foram registradas

    public Statistics(int memorySize) {
        this.memorySize = memorySize;
    }

    /** Registra o estado da memória depois de uma linha. */
    public void record(int usedMemory, int fragmentation) {
        maxUsed = Math.max(maxUsed, usedMemory);
        fragmentationSum += fragmentation;
        samples++;
    }

    public int maxUsed() { return maxUsed; }

    /** Média das fragmentações registradas (0 se nenhuma linha foi executada). */
    public double averageFragmentation() {
        return samples == 0 ? 0.0 : (double) fragmentationSum / samples;
    }

    /** Ex.: "28/32 (87,5%)". */
    public String formattedMaxOccupation() {
        double percent = 100.0 * maxUsed / memorySize;
        return String.format(PT_BR, "%d/%d (%.1f%%)", maxUsed, memorySize, percent);
    }

    /** Ex.: "4,30 posições (externa)". */
    public String formattedAverageFragmentation(String type) {
        return String.format(PT_BR, "%.2f posições (%s)", averageFragmentation(), type);
    }
}