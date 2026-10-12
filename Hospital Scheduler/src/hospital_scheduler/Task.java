package hospital_scheduler;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * One schedulable unit. Urgency and deadline are mutable so that
 * priority re-evaluation can update the SAME object — critical
 * because the MaxHeap's HeapNode points to this instance.
 */
public class Task {
    private final int id;
    private final String description;
    private Urgency urgency;                          // mutable
    private final Map<ResourceType, Integer> needs;
    private final int duration;
    private final int arrival;
    private int deadline;                             // mutable

    private double score;
    private long seq;
    private TaskStatus status = TaskStatus.WAITING;
    private int startTime = -1;

    public Task(int id, String d, Urgency u, Map<ResourceType,Integer> n,
                int dur, int arr, int dl) {
        this.id = id;
        this.description = d;
        this.urgency = u;
        this.needs = new EnumMap<>(n);
        this.duration = dur;
        this.arrival = arr;
        this.deadline = dl;
    }

    public int id()               { return id; }
    public String description()   { return description; }
    public Urgency urgency()      { return urgency; }
    public void setUrgency(Urgency u) { this.urgency = u; }
    public Map<ResourceType,Integer> needs() { return Collections.unmodifiableMap(needs); }
    public int duration()         { return duration; }
    public int arrival()          { return arrival; }
    public int deadline()         { return deadline; }
    public void setDeadline(int d) { this.deadline = d; }

    public double score()         { return score; }
    public void setScore(double s){ this.score = s; }
    public long seq()             { return seq; }
    public void setSeq(long s)    { this.seq = s; }

    public TaskStatus status()    { return status; }
    public void setStatus(TaskStatus s) { this.status = s; }
    public int startTime()        { return startTime; }
    public void setStartTime(int t) { this.startTime = t; }

    public int waitTime(int now) {
        return (status == TaskStatus.COMPLETED || status == TaskStatus.MISSED)
                ? startTime - arrival
                : now - arrival;
    }
    public boolean overdue(int now) { return now > deadline; }

    @Override public String toString() {
        return String.format("TASK%d[%s,%s,arr=%d,dl=%d,dur=%d]",
                id, description, urgency, arrival, deadline, duration);
    }
}
