# TopologySim

A Java distributed systems simulation that demonstrates parallel array sorting across multiple independent OS processes using two network topologies: **Bus Topology (UDP Multicast)** and **Star Topology (TCP Hub-and-Spoke)**.

In both implementations, a controller generates a random array of integers, partitions it across **7 worker processes**, waits for each worker to sort its assigned partition, and merges the sorted subarrays into a single globally sorted array using an $O(N \log k)$ Min-Heap ($k$-way merge) algorithm.

---

## Project Structure

```text
topologySim/
└── src/
    ├── busTopology/
    │   ├── BusController.java   # Multicast coordinator, array partitioner, and k-way merger
    │   ├── Worker.java          # Multicast worker that sorts its addressed partition
    │   ├── Node.java            # MulticastSocket wrapper (join, send, receive, timeout, close)
    │   ├── Message.java         # Custom binary message protocol using ByteBuffer
    │   └── Runner.java          # ProcessBuilder launcher for 1 BusController + 7 Workers
    └── starTopology/
        ├── Controller.java      # TCP ServerSocket hub managing 7 dedicated Socket connections
        ├── Node.java            # TCP worker spoke that receives, sorts, and returns an int[]
        └── Runner.java          # ProcessBuilder launcher for 1 Controller + 7 Nodes
```

---

## Topologies Overview

### 1. Bus Topology (`busTopology`)
Simulates a shared communication bus using **UDP Multicast** (`230.0.0.1:9000`). Every process opens a single `MulticastSocket` joined to the same multicast group. Because all transmissions are broadcast on the shared channel, messages include sender and recipient **PIDs** so workers can filter out self-echoes and messages intended for other nodes.

* **`BusController.java`**: Waits for 7 workers to announce themselves, generates 70 random integers, broadcasts partitioned subarrays targeted to each worker's PID, tracks completions and disconnects (with a 10-second `SocketTimeoutException` safeguard), and merges the returned sorted arrays.
* **`Worker.java`**: Joins the multicast group, announces its PID, waits for a partition addressed to its PID, sorts it with `Arrays.sort()`, sends the sorted array back, and broadcasts a leave notification before closing.
* **`Node.java`**: Encapsulates `MulticastSocket` setup, `DatagramPacket` transmission/reception, a 10-second socket read timeout, and clean group departure (`leaveGroup()` + `close()`).
* **`Message.java`**: Serializes and deserializes `(long from, long to, int type, int[] data)` into raw byte arrays via `ByteBuffer`.

#### Bus Message Protocol

| Type | Direction | Description |
| :---: | :--- | :--- |
| `1` | Controller $\rightarrow$ Bus | Discovery / ready broadcast |
| `2` | Controller $\rightarrow$ Worker | Unsorted array partition assigned to a specific worker PID |
| `3` | Controller $\rightarrow$ Bus | Shutdown / completion broadcast |
| `4` | Worker $\rightarrow$ Controller | Sorted array partition returned to controller |
| `5` | Worker $\rightarrow$ Bus | Worker joined the multicast group (announces PID) |
| `6` | Worker $\rightarrow$ Bus | Worker leaving the multicast group |

---

### 2. Star Topology (`starTopology`)
Simulates a centralized hub-and-spoke network using **TCP** (`127.0.0.1:9001`). Instead of broadcasting on a shared bus, the controller maintains dedicated point-to-point full-duplex TCP streams with each worker.

* **`Controller.java`**: Opens a `ServerSocket` on port `9001`, accepts 7 incoming worker connections into a `Socket[]` array, partitions a 70-element random `int[]`, sends each partition via `ObjectOutputStream`, reads back the sorted `int[]` objects via `ObjectInputStream`, merges them with the Min-Heap algorithm, and closes all sockets.
* **`Node.java`**: Connects to the controller's `ServerSocket` at `127.0.0.1:9001`, reads its assigned `int[]` partition from `ObjectInputStream`, sorts it using `Arrays.sort()`, sends the sorted array back over `ObjectOutputStream`, and closes its socket.
* **`Runner.java`**: Automates launching 1 `Controller` process and 7 `Node` processes concurrently.

---

## Merge Algorithm

Once all $k = 7$ sorted partitions are collected into a 2D array, the controller combines them using a **Min-Heap (`PriorityQueue`)** storing `{value, rowIndex, colIndex}` tuples:

1. Insert the first element of each non-empty row into the min-heap.
2. Repeatedly poll the smallest element, append it to the final output array, and push the next element from that same row into the heap.

* **Time Complexity:** $O(N \log k)$, where $N$ is the total number of elements (default: 70) and $k$ is the number of workers (default: 7).
* **Space Complexity:** $O(k)$ for the heap and $O(N)$ for the final merged array.

---

## Comparison: Bus vs. Star

| Feature | Bus Topology (`busTopology`) | Star Topology (`starTopology`) |
| :--- | :--- | :--- |
| **Transport Protocol** | UDP Multicast (`MulticastSocket`) | TCP (`ServerSocket` & `Socket`) |
| **Sockets per Process** | 1 per process (8 total) | 1 `ServerSocket` + 7 `Socket`s on controller; 1 per worker (15 total) |
| **Serialization** | Custom binary format (`ByteBuffer`) | Java Object Serialization (`ObjectInputStream` / `ObjectOutputStream`) |
| **Addressing** | Broadcast to group; filtered by PID | Dedicated point-to-point TCP connection per worker |
| **Reliability** | Connectionless; relies on timeouts | Built-in TCP delivery guarantees, ordering, and flow control |

---

## How to Run

### Prerequisites
* **Java 11 or higher** (required for `ProcessHandle.current().pid()`).
* For the **Bus Topology**, `-Djava.net.preferIPv4Stack=true` is passed by `Runner` to avoid IPv6 multicast issues on macOS/Windows/Linux.

### Option 1: Run in IntelliJ IDEA
1. Ensure `src` is marked as **Sources Root**.
2. To run the **Bus Topology**, open `src/busTopology/Runner.java` and click **Run**.
3. To run the **Star Topology**, open `src/starTopology/Runner.java` and click **Run**.

### Option 2: Run from the Terminal

From the `src` directory:

**Compile both packages:**
```bash
javac busTopology/*.java starTopology/*.java
```

**Run Bus Topology:**
```bash
java busTopology.Runner
```

**Run Star Topology:**
```bash
java starTopology.Runner
```

*(You can also run the controller and workers manually in separate terminal windows—just be sure to start `BusController` or `Controller` before launching the 7 worker instances.)*