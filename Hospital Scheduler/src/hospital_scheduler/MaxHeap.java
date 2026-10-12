package hospital_scheduler;

import java.util.HashMap;
import java.util.Map;

/**
 * Pointer-based indexed binary max-heap.
 * No array — every node has parent / left / right pointers.
 * Insert / extract-max / update-priority all O(log n).
 */
public class MaxHeap {

    private HeapNode root;
    private int size;
    private final Map<Integer, HeapNode> index = new HashMap<>();
    private long seqCounter;

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    // ---------------- public ops ----------------

    public void insert(Task t) {
        t.setSeq(seqCounter++);
        HeapNode node = new HeapNode(t);

        if (root == null) {
            root = node;
        } else {
            HeapNode parent = findParent(size + 1);
            node.parent = parent;
            if (parent.left == null) parent.left = node;
            else                     parent.right = node;
        }
        index.put(t.id(), node);
        size++;
        siftUp(node);
    }

    public Task extractMax() {
        if (root == null) return null;
        Task top = root.task;
        index.remove(top.id());

        if (size == 1) {
            root = null;
            size = 0;
            return top;
        }

        HeapNode last = findNode(size);
        root.task = last.task;
        index.put(last.task.id(), root);

        if (last.parent != null) {
            if (last.parent.left == last)  last.parent.left  = null;
            else                            last.parent.right = null;
        }

        size--;
        siftDown(root);
        return top;
    }

    public void updateKey(Task t) {
        HeapNode n = index.get(t.id());
        if (n == null) return;
        siftUp(n);
        siftDown(n);
    }

    // ---------------- tree navigation ----------------

    /** Parent of position pos (1-based). Used by insert. */
    private HeapNode findParent(int pos) {
        if (pos == 1) return root;
        int bits = Integer.SIZE - Integer.numberOfLeadingZeros(pos) - 1;
        HeapNode cur = root;
        for (int i = bits - 1; i > 0; i--) {
            int bit = (pos >> i) & 1;
            cur = (bit == 0) ? cur.left : cur.right;
        }
        return cur;
    }

    /** Node at position pos (1-based). Used by extractMax. */
    private HeapNode findNode(int pos) {
        if (pos == 1) return root;
        int bits = Integer.SIZE - Integer.numberOfLeadingZeros(pos) - 1;
        HeapNode cur = root;
        for (int i = bits - 1; i >= 0; i--) {
            int bit = (pos >> i) & 1;
            cur = (bit == 0) ? cur.left : cur.right;
        }
        return cur;
    }

    // ---------------- heap invariants ----------------

    private boolean higher(HeapNode a, HeapNode b) {
        if (a.task.score() != b.task.score())
            return a.task.score() > b.task.score();
        return a.task.seq() < b.task.seq();
    }

    private void swapTasks(HeapNode a, HeapNode b) {
        Task tmp = a.task;
        a.task = b.task;
        b.task = tmp;
        index.put(a.task.id(), a);
        index.put(b.task.id(), b);
    }

    private void siftUp(HeapNode n) {
        while (n.parent != null && higher(n, n.parent)) {
            swapTasks(n, n.parent);
            n = n.parent;
        }
    }

    private void siftDown(HeapNode n) {
        while (true) {
            HeapNode best = n;
            if (n.left  != null && higher(n.left,  best)) best = n.left;
            if (n.right != null && higher(n.right, best)) best = n.right;
            if (best == n) break;
            swapTasks(n, best);
            n = best;
        }
    }
}
