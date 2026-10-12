package hospital_scheduler;

/**
 * Composite strategy: urgency + deadline pressure + ageing + overdue override.
 * Small numbers so the log reads nicely.
 */
public final class PriorityCalculator implements SchedulingStrategy {

    public static final double URGENCY_W   = 1.0;
    public static final double DEADLINE_W  = 0.5;
    public static final double AGEING_RATE = 0.03;
    public static final double OVERDUE     = 100.0;

    @Override public String name() { return "Composite (urgency + ageing)"; }

    public static double compute(Task t, int now) {
        double urgency = t.urgency().weight() * URGENCY_W;
        int remain = t.deadline() - now;
        double dlPressure = (remain <= 0) ? 1.0 : 1.0 / (remain + 1.0);
        double ageing = t.waitTime(now) * AGEING_RATE;
        double s = urgency + dlPressure * DEADLINE_W + ageing;
        if (t.overdue(now)) s += OVERDUE;
        return s;
    }

    @Override public double score(Task t, int now) { return compute(t, now); }
}
