package memory;

import java.util.Locale;

/**
 Acumula os números do resumo final que dependem da execução inteira:
 a ocupação máxima da memória e a fragmentação média.
 
 */
public class Statistics {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final int memorySize;

    private int maxUsed;          // maior ocupação vista até agora
    private long fragmentationSum; // soma das fragmentações
    private int samples;         

    public Statistics(int memorySize) {
        this.memorySize = memorySize;
    }

    public void record(int usedMemory, int fragmentation) {
        maxUsed = Math.max(maxUsed, usedMemory);
        fragmentationSum += fragmentation;
        samples++;
    }

    public int maxUsed() { return maxUsed; }

    public double averageFragmentation() {
        return samples == 0 ? 0.0 : (double) fragmentationSum / samples;
    }

    public String formattedMaxOccupation() {
        double percent = 100.0 * maxUsed / memorySize;
        return String.format(PT_BR, "%d/%d (%.1f%%)", maxUsed, memorySize, percent);
    }

    public String formattedAverageFragmentation(String type) {
        return String.format(PT_BR, "%.2f posições (%s)", averageFragmentation(), type);
    }
}