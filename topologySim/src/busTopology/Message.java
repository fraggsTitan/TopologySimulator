package busTopology;

import java.nio.ByteBuffer;

public class Message {
    long from;//from and to is meant to be PID here
    long to;
    int type;
    /*
    1 - port saying ready and sends address to the controller from field contains the nodes address,
    2 - an array was sent from controller and you should sort it
    3- success message from controller, you can close
    //1-3 are controller side msgs
    4 - node sorted array and is sending it back
    5- tell the sender that you have joined the group also give your port no
    6- node is leaving the group
     */
    int[]data;
    Message(long from, long to, int type,int[]data){
        this.from=from;
        this.type=type;
        this.to=to;
        this.data=data;
    }
    public byte[] getBytes(){
        ByteBuffer buf=ByteBuffer.allocate((data.length+2)*4+2*8);
        buf.putLong(from);
        buf.putLong(to);
        buf.putInt(type);
        buf.putInt(data.length);
        for(int n:data)buf.putInt(n);
        return buf.array();
    }
    public static Message fromBytes(byte[]bytes){
        ByteBuffer  buf=ByteBuffer.wrap(bytes);
        long from=buf.getLong();
        long to=buf.getLong();
        int type=buf.getInt();
        int n=buf.getInt();
        int[]data=new int[n];
        for(int i=0;i<n;i++)data[i]=buf.getInt();
        return new Message(from,to,type,data);
    }
}
