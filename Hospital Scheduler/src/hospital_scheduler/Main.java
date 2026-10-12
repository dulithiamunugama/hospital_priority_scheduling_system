package hospital_scheduler;

import java.util.List;
import java.util.Map;

/** Entry point. Runs the baseline + the strategy comparison. */
public class Main {

    static ResourcePool buildHospital() {
        ResourcePool p = new ResourcePool();
        for (int i=1;i<=3;i++)  p.add(new Resource("OT"+i,  ResourceType.OT));
        for (int i=1;i<=6;i++)  p.add(new Resource("SG"+i,  ResourceType.SURGEON));
        for (int i=1;i<=20;i++) p.add(new Resource("NR"+i,  ResourceType.NURSE));
        for (int i=1;i<=4;i++)  p.add(new Resource("AM"+i,  ResourceType.AMBULANCE));
        for (int i=1;i<=2;i++)  p.add(new Resource("MR"+i,  ResourceType.MRI));
        for (int i=1;i<=25;i++) p.add(new Resource("BD"+i,  ResourceType.BED));
        for (int i=1;i<=4;i++)  p.add(new Resource("VT"+i,  ResourceType.VENTILATOR));
        for (int i=1;i<=6;i++)  p.add(new Resource("TC"+i,  ResourceType.TECHNICIAN));
        return p;
    }

    /**
     * Run a dataset through a scheduler with the given strategy.
     * verbose = true  → prints the inline event log and the report
     * verbose = false → prints nothing at all (silent)
     */
    static Scheduler run(List<Task> data, SchedulingStrategy strategy,
                         int extraMinutes, boolean verbose) {

        Scheduler s = new Scheduler(buildHospital(), strategy, !verbose);

        int lastArrival = data.isEmpty() ? 0
                                         : data.get(data.size() - 1).arrival();
        int totalMinutes = lastArrival + extraMinutes;

        int cursor = 0;
        for (int t = 0; t < totalMinutes; t++) {
            while (cursor < data.size()
                    && data.get(cursor).arrival() <= t)
                s.submit(data.get(cursor++));
            s.tick();
        }
        if (verbose) s.report();
        return s;
    }

    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();

        System.out.println("========================================");
        System.out.println(" HOSPITAL PRIORITY SCHEDULER");
        System.out.println(" Tasks Used: 100");
        System.out.println("========================================");
        System.out.println();

        // Baseline dataset
        List<Task> data = new DatasetGenerator(12345L)
                .generate(100, 10, "dataset.csv");

        // Baseline: verbose — shows first 60 event lines + full report
        run(data, new PriorityCalculator(), 120, true);
        
     // ---- Demo: priority re-evaluation ----
        System.out.println("\n========================================");
        System.out.println(" PRIORITY RE-EVALUATION DEMO");
        System.out.println("========================================\n");

        Scheduler s = new Scheduler(buildHospital(), new PriorityCalculator());

        Task t7  = new Task(7,  "Urgent Surgery",   Urgency.HIGH, Map.of(ResourceType.OT,1), 60, 0, 240);
        Task t99 = new Task(99, "Routine Checkup",  Urgency.LOW,  Map.of(ResourceType.BED,1), 20, 0, 600);
        s.submit(t7);
        s.submit(t99);

        for (int i = 0; i < 5; i++) s.tick();    // let a few minutes pass

        // Simulate: patient's condition worsens
        s.changePriority(7, Urgency.CRITICAL, 30);
        
     // ---- Demo: starvation warning ----
        System.out.println("\n========================================");
        System.out.println(" STARVATION WARNING DEMO");
        System.out.println("========================================\n");

        ResourcePool smallPool = new ResourcePool();
        smallPool.add(new Resource("OT1", ResourceType.OT));   // only ONE theatre
        Scheduler starve = new Scheduler(smallPool, new PriorityCalculator());

        // One task occupies the OT for 120 minutes...
        starve.submit(new Task(1, "Long Surgery", Urgency.HIGH,
                Map.of(ResourceType.OT, 1), 120, 0, 500));

        // ...and another waits, hour after hour
        starve.submit(new Task(2, "Second Surgery", Urgency.CRITICAL,
                Map.of(ResourceType.OT, 1), 30, 0, 900));

        for (int i = 0; i < 200; i++) starve.tick();
        
     // ---- Demo: deadline miss ----
        System.out.println("\n========================================");
        System.out.println(" DEADLINE MISS DEMO");
        System.out.println("========================================\n");

        Scheduler late = new Scheduler(buildHospital(), new PriorityCalculator());

        // Task with impossible deadline: arrives at 0, deadline at 1, duration 60
        late.submit(new Task(1, "Impossible Case", Urgency.LOW,
                Map.of(ResourceType.BED, 1), 60, 0, 1));

        for (int i = 0; i < 70; i++) late.tick();
        

        // Comparison: silent — only the table prints
        System.out.println();
        System.out.println("========================================");
        System.out.println(" STRATEGY COMPARISON (100 tasks)");
        System.out.println("========================================");
        System.out.printf("%-32s | %-9s | %-7s | %-7s | %-9s%n",
                "Strategy", "Completed", "Missed", "Miss%", "AvgWait");
        System.out.println("----------------------------------------"
                + "----------------------------------------");

        for (SchedulingStrategy st : List.of(
                new FifoStrategy(),
                new StrictPriorityStrategy(),
                new PriorityCalculator())) {

        	Scheduler cmpScheduler = run(data, st, 120, false);

        	System.out.printf("%-32s | %-9d | %-7d | %-6.2f%% | %-9.2f%n",
        	        st.name(),
        	        cmpScheduler.getCompleted(),
        	        cmpScheduler.getMissed(),
        	        cmpScheduler.getCompleted() == 0 ? 0
        	                : 100.0 * cmpScheduler.getMissed() / cmpScheduler.getCompleted(),
        	        cmpScheduler.getAverageWait());
        }
        System.out.println("----------------------------------------"
                + "----------------------------------------");

        long elapsed = System.currentTimeMillis() - t0;
        System.out.println();
        System.out.printf("Total Execution Time : %d ms%n", elapsed);
        
     // ---- Scaling tests: same seed, four sizes ----
        System.out.println();
        System.out.println("========================================");
        System.out.println(" SCALING TESTS");
        System.out.println("========================================");
        System.out.printf("%-10s | %-9s | %-7s | %-7s | %-9s%n",
                "N Tasks", "Completed", "Missed", "Miss%", "AvgWait");
        System.out.println("--------------------------------------------------");

        long SEED = 12345L;
        for (int n : new int[]{25, 50, 100, 200}) {
            List<Task> d = new DatasetGenerator(SEED)
                    .generate(n, 10, "dataset_" + n + ".csv");
            
            Scheduler scaleScheduler = run(d, new PriorityCalculator(), 120, false);

            System.out.printf("%-10d | %-9d | %-7d | %-6.2f%% | %-9.2f%n",
                    n,
                    scaleScheduler.getCompleted(),
                    scaleScheduler.getMissed(),
                    scaleScheduler.getCompleted() == 0 ? 0
                            : 100.0 * scaleScheduler.getMissed() / scaleScheduler.getCompleted(),
                    scaleScheduler.getAverageWait());
        }
        System.out.println("--------------------------------------------------");
    }
    
    
}
