package hospital_scheduler;

/** FIFO: earliest arrival wins. No urgency, no ageing. */
class FifoStrategy implements SchedulingStrategy {
    public String name() { return "FIFO (first-come-first-served)"; }
    public double score(Task t, int now) {
        return 1_000_000.0 - t.arrival();
    }
}

/** Strict priority: urgency only, no ageing, no deadline. */
class StrictPriorityStrategy implements SchedulingStrategy {
    public String name() { return "Strict priority (urgency only)"; }
    public double score(Task t, int now) {
        return t.urgency().weight() * 1000.0;
    }
}
