package hospital_scheduler;

/** One link in a singly-linked list of tasks. */
public class TaskNode {
    public Task task;
    public TaskNode next;
    public TaskNode(Task t) { this.task = t; }
}