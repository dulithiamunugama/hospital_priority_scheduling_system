package hospital_scheduler;

/**
 * Severity of a case. Each level carries a numeric weight used
 * by PriorityCalculator. CRITICAL is heaviest.
 */
public enum Urgency {
    CRITICAL(4), HIGH(3), MEDIUM(2), LOW(1);

    private final int weight;
    Urgency(int w) { this.weight = w; }
    public int weight() { return weight; }
}