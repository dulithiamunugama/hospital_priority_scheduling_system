package hospital_scheduler;

/**
 * Contract for a scheduling strategy. Every strategy answers one
 * question: given a waiting task and the current time, what is its
 * priority score? Higher score = dispatch sooner.
 */
public interface SchedulingStrategy {
    double score(Task t, int now);
    String name();
}