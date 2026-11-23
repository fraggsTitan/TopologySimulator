package starTopology;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;

public class Controller {
    private final int timeoutMs=10000;//10s timeout
    private final int desiredConnections;
    private final int arraySize;
    private final Socket[]workerSockets;
    private final ServerSocket serverSocket;
    private final InetSocketAddress address;
    private int currentConnections;
    public Controller(String ip,int port,int desiredConnections,int arraySize) throws IOException {
        this.currentConnections=0;
        this.desiredConnections=desiredConnections;
        this.address=new InetSocketAddress(ip,port);
        this.serverSocket=new ServerSocket(address.getPort());
        this.workerSockets=new Socket[desiredConnections];
        this.arraySize=arraySize;
    }
    public void getConnections() throws  IOException {//connect to number of processed defined by desiredConnections
        while(currentConnections<desiredConnections){
            workerSockets[currentConnections]=this.serverSocket.accept();
            currentConnections++;
        }
    }
    public int[]makeArray(){//makes a random value filled array of size arraySize
        int[]res=new int[arraySize];
        for(int i=0;i<arraySize;i++)res[i]= ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE,Integer.MAX_VALUE);
        return res;
    }
    public int[][]partitionArray(int[]arr){//partitions the given array into desiredConnections number of arrays in a 2d array
        int[][]partitions=new int[this.desiredConnections][];
        int partitionSize=arr.length/desiredConnections;
        int from=0;
        for(int i=0;i<desiredConnections;i++){
            if(i==desiredConnections-1)partitions[i]=Arrays.copyOfRange(arr,from,arr.length);
            else partitions[i]= Arrays.copyOfRange(arr,from,from+partitionSize);
            from+=partitionSize;
        }
        return partitions;
    }
    public void close() throws IOException {
        this.serverSocket.close();
        for(Socket socket:workerSockets)socket.close();
    }
    public void sendArray(int[]arr,Socket socket) throws IOException {//sends the array along the given socket, may throw timeout exception in case message not received in time
        socket.setSoTimeout(this.timeoutMs);
       ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(arr);
        oos.flush();
        System.out.printf("Sent array of size %d to process %s:%d from %s:%d\n",arr.length,socket.getInetAddress(),socket.getPort(),socket.getLocalAddress(),socket.getLocalPort());
    }
    public void makeAndSendArrays() throws IOException{//orchestrates whole process of creating random array, partitioning and then sending to each of the sockets
        int[]array=makeArray();
        int[][]arrayPartitions=partitionArray(array);
        for(int i=0;i<desiredConnections;i++){
            this.sendArray(arrayPartitions[i],this.workerSockets[i]);
        }
        System.out.printf("Successfully sent %d arrays to %d processes\n",arrayPartitions.length,this.workerSockets.length);
    }
    public int[]receiveAndSortArrays() throws IOException, ClassNotFoundException {
        //receives arrays from all the sockets and calls the merge to sort them, each array arrives in sorted order
        int[][]arrays=new int[this.desiredConnections][];
        for(int i=0;i<desiredConnections;i++){
            ObjectInputStream ois= new ObjectInputStream(workerSockets[i].getInputStream());
            arrays[i]=(int[])ois.readObject();
        }
        return mergeSortedMatrix(arrays);
    }
    public static int[] mergeSortedMatrix(int[][] matrix) {
        // Min-heap storing {value, rowIndex, colIndex}
        PriorityQueue<int[]> minHeap = new PriorityQueue<>(
                Comparator.comparingInt(a -> a[0])
        );

        int totalElements = 0;

        // 1. Add the first element of every non-empty row to the heap
        for (int row = 0; row < matrix.length; row++) {
            if (matrix[row] != null && matrix[row].length > 0) {
                minHeap.offer(new int[]{matrix[row][0], row, 0});
                totalElements += matrix[row].length;
            }
        }

        int[] result = new int[totalElements];
        int idx = 0;

        // 2. Repeatedly pop the smallest element and push the next element from that same row
        while (!minHeap.isEmpty()) {
            int[] current = minHeap.poll();
            int value = current[0];
            int row = current[1];
            int col = current[2];

            result[idx++] = value;

            // If that row has more elements, push the next one into the heap
            if (col + 1 < matrix[row].length) {
                minHeap.offer(new int[]{matrix[row][col + 1], row, col + 1});
            }
        }
        return result;
    }
    public static void main(String[]args) throws IOException, ClassNotFoundException, InterruptedException {
        String ip="127.0.0.1";
        int port=9001;
        int desiredConnections=7;
        int arraySize=72;
        Controller controller=new Controller(ip,port,desiredConnections,arraySize);
        controller.getConnections();
        controller.makeAndSendArrays();
        int[]res=controller.receiveAndSortArrays();
        Thread.sleep(100);//sleep so all others can flush output
        System.out.printf("Received sorted array: %s\n",Arrays.toString(res));
        controller.close();
    }
}
