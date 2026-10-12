package hospital_scheduler;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Reproducible task dataset. Each arrival generates TWO identical
 * tasks (id and id+n/2) so the resource pool sees contention.
 * Writes each task to a CSV file.
 */
public class DatasetGenerator {
    private final Random rng;
    private final String[] names = {
            "Emergency Surgery", "MRI Scan", "Ambulance Request",
            "Bed Allocation", "Patient Transfer", "Equipment Maintenance",
            "Urgent Surgery", "CT/MRI Diagnostic", "Ward Assistance",
            "Critical Bed Request", "Equipment Inspection", "Emergency Transfer"
    };

    public DatasetGenerator(long seed) { this.rng = new Random(seed); }

    public List<Task> generate(int n, int gapMinutes, String csvPath) {
        List<Task> data = new ArrayList<>(n);
        int time = 0;

        try (PrintWriter csv = new PrintWriter(new FileWriter(csvPath))) {
            csv.println("id,description,urgency,arrival,deadline,duration,resources");

            int pairs = n / 2;
            for (int i = 1; i <= pairs; i++) {
                Urgency u = pickUrgency();
                Map<ResourceType,Integer> need = new EnumMap<>(ResourceType.class);
                buildNeeds(u, need);

                int dur = switch (u) {
                    case CRITICAL -> 60 + rng.nextInt(60);
                    case HIGH     -> 45 + rng.nextInt(45);
                    case MEDIUM   -> 30 + rng.nextInt(30);
                    case LOW      -> 15 + rng.nextInt(20);
                };
                int dl = time + switch (u) {
                    case CRITICAL ->  30 + rng.nextInt(30);
                    case HIGH     ->  90 + rng.nextInt(60);
                    case MEDIUM   -> 180 + rng.nextInt(120);
                    case LOW      -> 360 + rng.nextInt(240);
                };
                String name = names[rng.nextInt(names.length)];

                Task a = new Task(i,         name, u, need, dur, time, dl);
                Task b = new Task(i + pairs, name, u, need, dur, time, dl);
                data.add(a);
                data.add(b);

                StringBuilder rs = new StringBuilder();
                for (Map.Entry<ResourceType,Integer> e : need.entrySet())
                    rs.append(rs.length() == 0 ? "" : "|")
                      .append(e.getKey()).append(':').append(e.getValue());

                csv.printf("%d,%s,%s,%d,%d,%d,%s%n",
                        a.id(), name, u, time, dl, dur,
                        rs.length() == 0 ? "-" : rs.toString());
                csv.printf("%d,%s,%s,%d,%d,%d,%s%n",
                        b.id(), name, u, time, dl, dur,
                        rs.length() == 0 ? "-" : rs.toString());

                time += gapMinutes;
            }
        } catch (IOException e) {
            System.err.println("CSV write failed: " + e.getMessage());
        }
        return data;
    }

    private void buildNeeds(Urgency u, Map<ResourceType,Integer> need) {
        switch (u) {
            case CRITICAL -> {
                need.put(ResourceType.OT, 1);
                need.put(ResourceType.SURGEON, 1);
                need.put(ResourceType.NURSE, 2);
                if (rng.nextDouble() < 0.6) need.put(ResourceType.VENTILATOR, 1);
            }
            case HIGH -> {
                need.put(ResourceType.NURSE, 1);
                if (rng.nextDouble() < 0.5) need.put(ResourceType.MRI, 1);
                if (rng.nextDouble() < 0.4) need.put(ResourceType.BED, 1);
            }
            case MEDIUM -> {
                need.put(ResourceType.NURSE, 1);
                if (rng.nextDouble() < 0.5) need.put(ResourceType.BED, 1);
                if (rng.nextDouble() < 0.3) need.put(ResourceType.AMBULANCE, 1);
            }
            case LOW -> {
                if (rng.nextDouble() < 0.5) need.put(ResourceType.BED, 1);
                if (rng.nextDouble() < 0.3) need.put(ResourceType.AMBULANCE, 1);
            }
        }
    }

    private Urgency pickUrgency() {
        double r = rng.nextDouble();
        if (r < 0.10) return Urgency.CRITICAL;
        if (r < 0.30) return Urgency.HIGH;
        if (r < 0.65) return Urgency.MEDIUM;
        return Urgency.LOW;
    }
}
