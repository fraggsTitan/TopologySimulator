package busTopology;


import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;

public class BusController{//bus controller must be run before the workers
    private final Node controllerNode;
    public BusController(Node controllerNode){
        this.controllerNode = controllerNode;
    }
    public static void main(String[]args) throws IOException {
        String address="230.0.0.1";
        int port=9000;
        int nItems=70;
        int currentConnections=0;
        int desiredConnections=7;
        long[]addresses=new long[desiredConnections];
        long pid=ProcessHandle.current().pid();
        Node controllerNode=new Node(address,port,nItems);
        while(currentConnections<desiredConnections){
            Message msg=controllerNode.receive();
            if(msg.type!=5) {
                broadcastEnd(controllerNode);
                System.exit(0);
            }
            addresses[currentConnections++]=msg.from;
            System.out.printf("Made connection with %d nodes",currentConnections);
        }//setup connection with required number of nodes
        System.out.printf("Connection with %d nodes done, continuing now... ",currentConnections);
        controllerNode.send(makeMessage(pid,pid,1,new int[]{}));//discover msg, ask everyone to tell their pids

        //make the array now
        int absMax=10000;
        int[]data=new int[nItems];
        for(int i=0;i<data.length;i++)data[i]= ThreadLocalRandom.current().nextInt(-absMax,absMax);

        //partition array and send
        int cur=0;
        int partitionSize=nItems/ addresses.length;
        for(int i=0;i<addresses.length;i++){
            if(i==addresses.length-1){
                //send all elements remaining
                controllerNode.send(makeMessage(pid,addresses[i],2,makePartition(data,cur,nItems-cur)));
            }else controllerNode.send(makeMessage(pid,addresses[i],2,makePartition(data,cur,partitionSize)));
            cur+=partitionSize;//increment by that much
        }
        List<Long> completedNodes=new ArrayList<>();
        int[][]valueMat=new int[addresses.length][];
        int replies=0;//which index to write to in matrix
        while(currentConnections>0){
            try {
                Message msg = controllerNode.receive();
                if(msg.from==pid)continue;
                switch(msg.type){
                    case 4:{
                        completedNodes.add(msg.from);
                        valueMat[replies++]=msg.data;
                        break;
                    }
                    case 6:{
                        currentConnections--;
                        System.out.printf("Node with id %d is leaving the group\n",msg.from);
                        if(!completedNodes.contains(msg.from)){
                            System.out.printf("Node with id %d never completed its task but left the group, aborting..\n",msg.from);
                            broadcastEnd(controllerNode);
                            System.exit(0);
                        }
                        break;
                    }
                    case 5:{
                        //pass, dont care about new nodes entering
                        break;
                    }
                    default:{
                        System.out.printf("Invalid message received from %d, discarding..",msg.from);
                    }
                }
            }catch(SocketTimeoutException e){
                System.out.println("No messages received. Terminating...");
                broadcastEnd(controllerNode);
                System.exit(0);
            }
        }
        broadcastEnd(controllerNode);
        System.out.println("Received all data from the sockets");
        if(cur< addresses.length){
            System.out.printf("Didn't receive data from all the workers only %d replies received,terminating...",cur);
            System.exit(0);
        }
        System.out.println("Final Output:");
        System.out.println(Arrays.toString(mergeSortedMatrix(valueMat)));
    }
    public static void broadcastEnd(Node controllerNode) throws IOException{
        controllerNode.send(makeMessage(0,0,3,new int[]{}));
        controllerNode.close();
    }
    public static Message makeMessage(long from, long to, int type,int[]data){
        if(type>4||type<0)throw new IllegalArgumentException("Invalid type, type must be less than 5 and non-negative");
        return new Message(from,to,type,data);
    }
    public static int[]makePartition(int[]data,int from,int partitionSize){
        return Arrays.copyOfRange(data,from,from+partitionSize);
    }
    public static int[] mergeSortedMatrix(int[][] matrix) {
        // Min-heap storing {value, rowIndex, colIndex}
        PriorityQueue<int[]> minHeap = new PriorityQueue<>(
                (a, b) -> Integer.compare(a[0], b[0])
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
}