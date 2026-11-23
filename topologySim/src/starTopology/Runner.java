package starTopology;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Runner {
    public static void main(String[] args) throws IOException, InterruptedException {
        String javaBin = ProcessHandle.current().info().command().orElse("java");
        String classpath = System.getProperty("java.class.path");

        List<Process> processes = new ArrayList<>();

        try {
            // 1. Start BusController first
            Process controller = new ProcessBuilder(
                    javaBin, "-Djava.net.preferIPv4Stack=true", "-cp", classpath, "starTopology.Controller"
            ).inheritIO().start();
            processes.add(controller);

            // Give BusController a moment to create server socket
            Thread.sleep(1000);

            // 2. Start 7 Worker instances
            for (int i = 0; i < 7; i++) {
                Process worker = new ProcessBuilder(
                        javaBin, "-Djava.net.preferIPv4Stack=true", "-cp", classpath, "starTopology.Node"
                ).inheritIO().start();
                processes.add(worker);
            }

            // Wait for the controller to finish
            controller.waitFor();

        } finally {
            // Clean up any leftover processes if you hit Stop
            for (Process p : processes) {
                if (p.isAlive()) {
                    p.destroy();
                }
            }
        }
    }
}
