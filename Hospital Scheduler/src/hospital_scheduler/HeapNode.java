package hospital_scheduler;

/**
 * A node in the max-heap. Holds a Task plus parent/left/right
 * pointers so the heap is a true linked tree, not an array.
 */
public class HeapNode {
    public Task task;
    public HeapNode parent;
    public HeapNode left;
    public HeapNode right;
    public HeapNode(Task t) { this.task = t; }
}
