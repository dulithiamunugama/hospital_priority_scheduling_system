package hospital_scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * The engine. Owns the priority heap, running list, resource pool,
 * and a pluggable SchedulingStrategy.
 * Set quiet=true to suppress inline logs (used for comparison runs).
 */
public class Scheduler {

    private static final int START_HOUR = 8;
    private static final int START_MIN  = 0;
    private static final int STARVATION_THRESHOLD = 60;
    private static final int LOG_LIMIT = 60;

    private final MaxHeap queue = new MaxHeap();
    private final TaskLinkedList allTasks = new TaskLinkedList();
    private final RunningList running = new RunningList();
    private final ResourcePool pool;
    private final SchedulingStrategy strategy;
    private final boolean quiet;

    private int now = 0;
    private int completed = 0, missed = 0, dispatched = 0, starvationCount = 0;
    private long totalWait = 0;
    private int logLines = 0;

    // ---------------- constructors ----------------

    public Scheduler(ResourcePool pool, SchedulingStrategy strategy, boolean quiet) {
        this.pool = pool;
        this.strategy = strategy;
        this.quiet = quiet;
    }

    public Scheduler(ResourcePool pool, SchedulingStrategy strategy) {
        this(pool, strategy, false);
    }

    public Scheduler(ResourcePool pool) {
        this(pool, new PriorityCalculator(), false);
    }

    // ---------------- logging helpers ----------------

    private boolean shouldLog() {
        return !quiet && logLines < LOG_LIMIT;
    }

    private void log(String s) {
        if (!shouldLog()) return;
        System.out.println(s);
        logLines++;
        if (logLines == LOG_LIMIT)
            System.out.println("... (further event logs suppressed) ...");
    }

    public int now() { return now; }

    private String clock(int minutesSinceStart) {
        int totalMin = START_HOUR * 60 + START_MIN + minutesSinceStart;
        int h = (totalMin / 60) % 24;
        int m = totalMin % 60;
        return String.format("%02d:%02d", h, m);
    }

    private String primaryResourceName(Task t) {
        for (ResourceType rt : t.needs().keySet()) return rt.display();
        return "—";
    }

    // ---------------- ingestion ----------------

    public void submit(Task t) {
        t.setScore(strategy.score(t, now));
        allTasks.add(t);
        queue.insert(t);
        log(String.format("[ARRIVAL]  %s | TASK%-4d | %s",
                clock(now), t.id(), t.description()));
    }

    // ---------------- priority re-evaluation ----------------

    public void changePriority(int taskId, Urgency u, Integer newDeadline) {
        Task t = allTasks.find(taskId);
        if (t == null || t.status() != TaskStatus.WAITING) return;

        Urgency oldU = t.urgency();
        t.setUrgency(u);
        if (newDeadline != null) t.setDeadline(newDeadline);
        t.setScore(strategy.score(t, now));
        queue.updateKey(t);

        if (quiet) return;
        System.out.println();
        System.out.println("========================================");
        System.out.println(" PRIORITY RE-EVALUATION");
        System.out.println("========================================");
        System.out.printf ("Time        : %s%n", clock(now));
        System.out.printf ("Task        : TASK%d%n", taskId);
        System.out.printf ("Old Urgency : %d%n", oldU.weight());
        System.out.printf ("New Urgency : %d%n", u.weight());
        System.out.printf ("New Priority: %.2f%n", t.score());
        System.out.println("========================================");
        System.out.println();
    }

    // ---------------- tick ----------------

    public void tick() {
        now++;
        finishRunning();
        ageWaiting();
        dispatchOne();
        pool.sampleUtilization();
    }

    private void finishRunning() {
        TaskNode n = running.head();
        while (n != null) {
            TaskNode next = n.next;
            Task t = n.task;
            if (now - t.startTime() >= t.duration()) {
                boolean late = now > t.deadline();
                t.setStatus(late ? TaskStatus.MISSED : TaskStatus.COMPLETED);
                pool.release(t.id());
                completed++;
                totalWait += (t.startTime() - t.arrival());
                if (late) {
                    missed++;
                    log(String.format(
                            "[DEADLINE WARNING] TASK%d missed its deadline.", t.id()));
                }
                log(String.format("[COMPLETE] %s | TASK%-4d | Released: %s",
                        clock(now), t.id(), primaryResourceName(t)));
                running.remove(t.id());
            }
            n = next;
        }
    }

    private void ageWaiting() {
        allTasks.forEach(t -> {
            if (t.status() == TaskStatus.WAITING) {
                double before = t.score();
                t.setScore(strategy.score(t, now));
                if (before != t.score()) queue.updateKey(t);
            }
        });
    }

    private void dispatchOne() {
        List<Task> deferred = new ArrayList<>();
        while (!queue.isEmpty()) {
            Task top = queue.extractMax();
            if (top.status() != TaskStatus.WAITING) continue;

            if (pool.canAcquire(top.needs())) {
                pool.acquire(top.id(), top.needs());
                top.setStartTime(now);
                top.setStatus(TaskStatus.RUNNING);
                running.add(top);
                dispatched++;

                int waited = now - top.arrival();
                if (waited >= STARVATION_THRESHOLD) {
                    starvationCount++;
                    log(String.format(
                            "[STARVATION WARNING] TASK%d waited %d minutes.",
                            top.id(), waited));
                }

                log(String.format(
                        "[DISPATCH] %s | TASK%-4d | %-18s | Priority: %.2f | "
                      + "Wait: %d min | Completes: %s",
                        clock(now), top.id(), primaryResourceName(top),
                        top.score(), waited, clock(now + top.duration())));
                break;
            } else {
                deferred.add(top);
            }
        }
        for (Task d : deferred) queue.insert(d);
    }

    // ---------------- helpers for tests ----------------

    public double scoreOf(int id) {
        Task t = allTasks.find(id);
        return t == null ? -1 : t.score();
    }

    public boolean isRunning(int taskId) {
        TaskNode n = running.head();
        while (n != null) {
            if (n.task.id() == taskId) return true;
            n = n.next;
        }
        return false;
    }

    public int getCompleted() { return completed; }
    public int getMissed()    { return missed; }
    public double getAverageWait() {
        return completed == 0 ? 0 : (double) totalWait / completed;
    }

    // ---------------- report ----------------

    public void report() {
        double hours = now / 60.0;
        double throughput = hours == 0 ? 0 : completed / hours;

        System.out.println();
        System.out.println("========================================");
        System.out.println(" PERFORMANCE SUMMARY");
        System.out.println("========================================");
        System.out.printf ("Strategy                : %s%n", strategy.name());
        System.out.printf ("Task Volume             : %d%n", allTasks.size());
        System.out.printf ("Completed Tasks         : %d%n", completed);
        System.out.printf ("Dispatched Tasks        : %d%n", dispatched);
        System.out.printf ("Throughput              : %.2f tasks/hour%n", throughput);
        System.out.printf ("Average Waiting Time    : %.2f minutes%n",
                completed == 0 ? 0 : (double) totalWait / completed);
        System.out.printf ("Missed Deadlines        : %d%n", missed);
        System.out.printf ("Deadline Miss Rate      : %.2f%%%n",
                completed == 0 ? 0 : 100.0 * missed / completed);
        System.out.printf ("Starvation Risk Tasks   : %d%n", starvationCount);
        System.out.printf ("Waiting in Queue        : %d%n", queue.size());
        System.out.printf ("Currently Running       : %d%n", running.size());
        System.out.println();

        System.out.println("========================================");
        System.out.println(" AVERAGE RESOURCE UTILIZATION");
        System.out.println("========================================");
        for (ResourceType rt : ResourceType.values()) {
            if (pool.totalCounts().containsKey(rt))
                System.out.printf("%-18s : %.2f%%%n",
                        rt.display(), pool.averageUtilization(rt) * 100);
        }
        System.out.println("========================================");
    }
}
