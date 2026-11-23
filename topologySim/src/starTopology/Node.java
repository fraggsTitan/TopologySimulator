package starTopology;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class Node {
    private final InetSocketAddress address;//stores the sockets addr,port
    private Socket socket;//socket to be initialized in getSocketConnection
    public Node(String addr,int port){
        this.address=new InetSocketAddress(addr,port);
    }
    public void getSocketConnection() throws IOException {//assign socket to the specified address port combination
        socket=new Socket(address.getAddress(),address.getPort());
    }
    public int[]receiveArray() throws IOException, ClassNotFoundException {
        //read array transmitted on the given socket
        int[]arr;
        ObjectInputStream ois=new ObjectInputStream(socket.getInputStream()) ;
        arr=(int[])ois.readObject();
        return arr;
    }
    public int[]sortArray(int[]arr){//method to sort array and return it
        Arrays.sort(arr);
        return arr;
    }
    public void sendArray(int[]arr) throws IOException {// send array over the socket
        ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(arr);
        oos.flush();
        System.out.printf("Sent array of size %d to %s:%d from %s:%d\n",arr.length,socket.getInetAddress(),socket.getPort(),socket.getLocalAddress(),socket.getLocalPort());
    }
    public void close() throws IOException {
        this.socket.close();
    }
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String addr="127.0.0.1";
        int port=9001;
        Node node=new Node(addr,port);
        node.getSocketConnection();
        int[]arr=node.receiveArray();
        node.sendArray(node.sortArray(arr));
        node.close();
    }
}
