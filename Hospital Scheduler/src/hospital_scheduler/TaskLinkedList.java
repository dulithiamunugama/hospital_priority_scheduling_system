package hospital_scheduler;

/** Singly-linked list of every submitted task. */
public class TaskLinkedList {
    private TaskNode head;
    private int size;

    public void add(Task t) {
        TaskNode n = new TaskNode(t);
        n.next = head;
        head = n;
        size++;
    }

    public Task find(int id) {
        for (TaskNode n = head; n != null; n = n.next)
            if (n.task.id() == id) return n.task;
        return null;
    }

    public void replace(int id, Task t) {
        for (TaskNode n = head; n != null; n = n.next)
            if (n.task.id() == id) { n.task = t; return; }
    }

    public void forEach(TaskVisitor v) {
        for (TaskNode n = head; n != null; n = n.next) v.visit(n.task);
    }

    public int size() { return size; }

    public interface TaskVisitor { void visit(Task t); }
}
