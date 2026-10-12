package hospital_scheduler;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Node-based free-list per resource type + in-use tracking.
 * acquire is atomic (all-or-nothing).
 * Also accumulates utilisation over time so the report shows
 * average utilisation, not an end-of-run snapshot.
 */
public class ResourcePool {

    private final Map<ResourceType, ResourceNode> free = new EnumMap<>(ResourceType.class);
    private final Map<Integer, ResourceNode> inUse = new HashMap<>();
    private final Map<ResourceType, Integer> total = new EnumMap<>(ResourceType.class);
    private final Map<ResourceType, Integer> busy  = new EnumMap<>(ResourceType.class);

    // cumulative utilisation
    private final Map<ResourceType, Double> utilSum = new EnumMap<>(ResourceType.class);
    private int ticksObserved = 0;

    public void add(Resource r) {
        ResourceNode node = new ResourceNode(r);
        node.next = free.get(r.type());
        free.put(r.type(), node);
        total.merge(r.type(), 1, Integer::sum);
        busy.putIfAbsent(r.type(), 0);
        utilSum.putIfAbsent(r.type(), 0.0);
    }

    public boolean canAcquire(Map<ResourceType,Integer> need) {
        for (Map.Entry<ResourceType,Integer> e : need.entrySet()) {
            if (countFree(e.getKey()) < e.getValue()) return false;
        }
        return true;
    }

    private int countFree(ResourceType t) {
        int n = 0;
        ResourceNode cur = free.get(t);
        while (cur != null) { n++; cur = cur.next; }
        return n;
    }

    public boolean acquire(int taskId, Map<ResourceType,Integer> need) {
        if (!canAcquire(need)) return false;

        ResourceNode acquiredHead = null;
        for (Map.Entry<ResourceType,Integer> e : need.entrySet()) {
            ResourceType type = e.getKey();
            for (int i = 0; i < e.getValue(); i++) {
                ResourceNode freeHead = free.get(type);
                free.put(type, freeHead.next);
                ResourceNode taken = new ResourceNode(freeHead.resource);
                taken.next = acquiredHead;
                acquiredHead = taken;
                busy.merge(type, 1, Integer::sum);
            }
        }
        inUse.put(taskId, acquiredHead);
        return true;
    }

    public void release(int taskId) {
        ResourceNode cur = inUse.remove(taskId);
        while (cur != null) {
            Resource r = cur.resource;
            ResourceNode back = new ResourceNode(r);
            back.next = free.get(r.type());
            free.put(r.type(), back);
            busy.merge(r.type(), -1, Integer::sum);
            cur = cur.next;
        }
    }

    public double utilization(ResourceType t) {
        int tot = total.getOrDefault(t, 0);
        return tot == 0 ? 0 : (double) busy.getOrDefault(t, 0) / tot;
    }

    public void sampleUtilization() {
        ticksObserved++;
        for (ResourceType t : total.keySet())
            utilSum.merge(t, utilization(t), Double::sum);
    }

    public double averageUtilization(ResourceType t) {
        if (ticksObserved == 0) return 0;
        return utilSum.getOrDefault(t, 0.0) / ticksObserved;
    }

    public Map<ResourceType,Integer> totalCounts() { return total; }
}
