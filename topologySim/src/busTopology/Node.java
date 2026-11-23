package busTopology;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

public class Node {
    static  final int timeoutTimeMs=10000;//wait for 10s
    private final MulticastSocket socket;
    private final InetSocketAddress group;
    private final int maxSize;
    public Node(String addr,int port, int maxSize) throws IOException {
        this.socket=new MulticastSocket(port);
        this.maxSize=maxSize;
        this.group=new InetSocketAddress(addr,port);
        socket.joinGroup(group,null);
        System.out.printf("Connected to group on %s:%d\n",addr,port);
    }
    public void send(Message message) throws IOException {
        byte[]dataBytes=message.getBytes();
        DatagramPacket pkt=new DatagramPacket(dataBytes,dataBytes.length,group.getAddress(),group.getPort());
        socket.send(pkt);
    }
    public Message receive() throws IOException {
        socket.setSoTimeout(timeoutTimeMs);
        byte[]buffer=new  byte[(maxSize+2)*4+2*8];
        DatagramPacket packet=new DatagramPacket(buffer,buffer.length);
        socket.receive(packet);
        return Message.fromBytes(packet.getData());
    }

    public void close() throws IOException {
        socket.leaveGroup(group,null);
        socket.close();
    }
}
