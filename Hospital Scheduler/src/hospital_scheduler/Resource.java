package hospital_scheduler;

/** One physical resource, e.g. "OT1" of type OT. */
public class Resource {
    private final String id;
    private final ResourceType type;

    public Resource(String id, ResourceType t) { this.id = id; this.type = t; }
    public String id()          { return id; }
    public ResourceType type()  { return type; }

    @Override public String toString() { return type.display() + ":" + id; }
}
