package hospital_scheduler;

/** One link in a singly-linked list of resources. */
public class ResourceNode {
    public Resource resource;
    public ResourceNode next;
    public ResourceNode(Resource r) { this.resource = r; }
}
