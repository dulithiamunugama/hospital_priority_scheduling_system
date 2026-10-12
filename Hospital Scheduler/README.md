# Hospital Priority Scheduler

**Scenario 2 — Adaptive Resource Planning and Priority Scheduling System**

**Module:** COMP50065 - Data Structures and Algorithms

**Name:** Dulithi Amunugama

**CB No:** CB014644

---

## 1. What This Program Does

A simulation of a hospital triage and resource allocation system. The program:

- Generates a reproducible synthetic dataset of patient tasks arriving at unpredictable times.
- Continuously dispatches the highest-priority task using a composite priority model
  (urgency + deadline proximity + waiting time + overdue override).
- Manages a finite pool of hospital resources (operating theatres, surgeons, nurses,
  ambulances, MRI scanners, beds, ventilators, technicians) with atomic all-or-nothing
  acquisition.
- Detects and reports missed deadlines and starvation risks.
- Produces a performance summary showing throughput, average wait time, deadline
  miss rate, and cumulative resource utilisation.
- Runs three scheduling strategies (FIFO, Strict priority, Composite ageing-based)
  for comparison.

---

## 2. Requirements

- **Java version:** JDK 17 or newer
  (the code uses `switch` expressions, introduced in Java 14).
- **No external libraries** — pure Java standard library only.
- **IDE:** any (tested in NetBeans and IntelliJ IDEA).

To check your Java version: 
	java -version
	

If it reports 11 or older, install a newer JDK.

---

## 3. How to Run

### From an IDE (recommended)

1. Open the project folder in NetBeans / IntelliJ / VS Code.
2. Ensure all `.java` files under `src/hospital_scheduler/` are present.
3. Run the class `hospital_scheduler.Main`.
4. The console will display:
   - a dataset preview,
   - inline arrival / dispatch / complete event logs,
   - a performance summary,
   - a strategy comparison table,
   - total execution time in milliseconds,
   - a scaling tests table for 25, 50, 100, and 200 tasks.

### From the command line

From the folder containing `src/`:

```bash
# Compile
javac -d build/classes src/hospital_scheduler/*.java

# Run
java -cp build/classes hospital_scheduler.Main
