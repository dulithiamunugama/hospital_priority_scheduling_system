package hospital_scheduler;

/** Singly-linked list of currently running tasks. */
public class RunningList {
    private TaskNode head;
    private int size;

    public void add(Task t) {
        TaskNode n = new TaskNode(t);
        n.next = head;
        head = n;
        size++;
    }

    public void remove(int taskId) {
        TaskNode prev = null, cur = head;
        while (cur != null) {
            if (cur.task.id() == taskId) {
                if (prev == null) head = cur.next;
                else prev.next = cur.next;
                size--;
                return;
            }
            prev = cur; cur = cur.next;
        }
    }

    public TaskNode head() { return head; }
    public int size()      { return size; }
}
