package busTopology;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class Worker {
    private Node workerNode;
    public Worker(Node workerNode){
        this.workerNode = workerNode;
    }
    public static void main(String[] args) throws IOException {
        String address = "230.0.0.1"; // Must match BusController (see note below)
        int port = 9000;
        int nItems = 70;
        long pid = ProcessHandle.current().pid();

        Node workerNode = new Node(address, port, nItems);

        try {
            // 1. Tell the controller we joined the group (type 5)
            workerNode.send(new Message(pid, 0, 5, new int[]{}));
            System.out.printf("Worker %d joined the bus. Waiting for work...\n", pid);

            boolean running = true;
            while (running) {
                try {
                    Message msg = workerNode.receive();

                    // Ignore our own echoed broadcasts
                    if (msg.from == pid) continue;

                    switch (msg.type) {
                        case 1:
                            // Discover message from controller - ignore since we already sent type 5
                            break;

                        case 2:
                            // Only process if this partition is addressed to us
                            if (msg.to == pid) {
                                System.out.printf("Worker %d received %d items to sort.\n", pid, msg.data.length);
                                Arrays.sort(msg.data);

                                // 2. Send sorted array back to controller (type 4)
                                workerNode.send(new Message(pid, msg.from, 4, msg.data));

                                // 3. Notify controller we are leaving (type 6) and stop
                                workerNode.send(new Message(pid, msg.from, 6, new int[]{}));
                                System.out.printf("Worker %d finished and is leaving.\n", pid);
                                running = false;
                            }
                            break;

                        case 3:
                            // Controller broadcast end/abort
                            System.out.printf("Worker %d received shutdown signal.\n", pid);
                            running = false;
                            break;

                        default:
                            // Ignore messages meant for other workers or controller (types 4, 5, 6)
                            break;
                    }
                } catch (SocketTimeoutException e) {
                    System.out.printf("Worker %d timed out waiting for controller. Leaving...\n", pid);
                    workerNode.send(new Message(pid, 0, 6, new int[]{}));
                    running = false;
                }
            }
        } finally {
            workerNode.close();
        }
    }
}
