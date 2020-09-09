package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Server {
int port = 5451;
	
	public static void main(String[] args) {
		Server ser = new Server();
		// TODO Auto-generated method stub
		Thread rec1 =  ser.new MulticastReceiver("1");
		Thread rec2 =  ser.new MulticastReceiver("2");
		Thread rec3 =  ser.new MulticastReceiver("3");
		Thread rec4 =  ser.new MulticastReceiver("4");
		Thread rec5 =  ser.new MulticastReceiver("5");
		
		Thread sen = ser.new MulticastPublisher();
		
		System.out.println("Starting system");
		
		
		rec1.start();
		rec2.start();
		rec3.start();
		rec4.start();
		rec5.start();
		
		sen.start();
		
		

	}
	
	public class MulticastReceiver extends Thread {
	    protected MulticastSocket socket = null;
	    protected byte[] buf = new byte[256];
	    String id;
	    
	    public MulticastReceiver(String id){
	    	this.id = id;
	    }
	 
	    public void run() {
	        System.out.println("Starting reciever: " + id);
	        try {
	        	socket = new MulticastSocket(port);
	        	//("230.0.0.0")
		        InetAddress group = InetAddress.getByName("230.0.0.0");
		        socket.joinGroup(group);
		        while (true) {
		            DatagramPacket packet = new DatagramPacket(buf, buf.length);
		            socket.receive(packet);
		            String received = new String(
		              packet.getData(), 0, packet.getLength());
		            if ("end".equals(received)) {
		                break;
		            }
		            else{
		            	System.out.println(id + ": "+received);
		            }
		            		
		        }
				socket.leaveGroup(group);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        socket.close();
	        System.exit(0);
	    }
	}
	
	public class MulticastPublisher extends Thread{
	    private DatagramSocket socket;
	    private InetAddress group;
	    private byte[] buf;
	 
	    public void multicast(
	    	      String multicastMessage) throws IOException {
	        socket = new DatagramSocket();
	        group = InetAddress.getByName("230.0.0.0");
	        buf = multicastMessage.getBytes();
	 
	        DatagramPacket packet 
	          = new DatagramPacket(buf, buf.length, group, port);
	        socket.send(packet);
	        socket.close();
	    }
	    public void run(){
	    	System.out.println("sending message");
	    	try {
				multicast("hello");
				System.out.println("message multicast");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}

}
