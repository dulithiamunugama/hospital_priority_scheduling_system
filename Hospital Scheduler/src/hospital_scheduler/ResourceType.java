package hospital_scheduler;

/**
 * Types of hospital resource. Each constant has a human-readable
 * display name used in the log and report.
 */
public enum ResourceType {
    OT("Operating Theatre"),
    SURGEON("Surgeon"),
    NURSE("Nurse"),
    AMBULANCE("Ambulance"),
    MRI("MRI Machine"),
    BED("Hospital Bed"),
    VENTILATOR("Ventilator"),
    TECHNICIAN("Technician");

    private final String display;
    ResourceType(String d) { this.display = d; }
    public String display() { return display; }

    @Override public String toString() { return display; }
}