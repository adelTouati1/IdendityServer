package Server;



import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class bullyLeaderElection {
    HashMap<Integer, InetAddress> servers = new HashMap<>();
    int currentCoordinator = 4; // the leader
    int myServerNumber = 2;
    LinkedHashMap<Integer, Boolean> heartbeatReplies = new LinkedHashMap<>();
    LinkedHashMap<Integer, Boolean> electionReplies = new LinkedHashMap<>();
   
    DatagramSocket datagramSocket;
    static int numberOfServers = 5;
 
    int port = 5141;

    public bullyLeaderElection(){
        try {
            servers.put(1,InetAddress.getByName("127.17.0.2")); 
            servers.put(2,InetAddress.getByName("127.17.0.3")); 
            servers.put(3,InetAddress.getByName("127.17.0.4")); 
            servers.put(4,InetAddress.getByName("127.17.0.5")); 
            servers.put(5,InetAddress.getByName("127.17.0.6")); 


            
            datagramSocket =  new DatagramSocket(port);
        } catch (UnknownHostException |SocketException e) {
            e.printStackTrace();
        }
    }


    void recevingMessage(){
        while (true) {
            try {
                byte[] buffer = new byte[100000];
                // receiving data from other processes
                DatagramPacket dpRec = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(dpRec);
                String recData = new String(dpRec.getData(), 0, dpRec.getLength());
                String[] dataArray = recData.split(",");
                String messageType = dataArray[0];
                int receivingServer = Integer.parseInt(dataArray[1].trim());
                System.out.println(messageType.toUpperCase() + " message received from " + receivingServer);
                System.out.println("myServerNumber: " + myServerNumber);

                // received election message reply with election_reply message
                if(messageType.compareToIgnoreCase("election") == 0){
                    // send a reply message with type as election_reply
                    String repdata = "election_reply" + "," + myServerNumber;
                    byte[] repBuff = repdata.getBytes();

                    DatagramPacket elecdp = new DatagramPacket(repBuff, repBuff.length,
                            servers.get(receivingServer), port);
                    datagramSocket.send(elecdp);

                    // convey the election messages to other servers
                    String relecdata = "election," + myServerNumber;
                    byte[] relecBuffer = relecdata.getBytes();
                    for (int server : servers.keySet()) {
                        if (server > myServerNumber && server != myServerNumber) {
                            DatagramPacket edp = new DatagramPacket(relecBuffer, relecBuffer.length,
                                    servers.get(server), port);

                            datagramSocket.send(edp);
                        }
                    }
                }
                // received heartbeat message reply with heartbeat_reply message
                else if(messageType.compareToIgnoreCase("heartbeat") == 0){
                    // send a reply message with type as heartbeat_reply
                    String localData = "heartbeat_reply" + "," + myServerNumber;
                    byte[] localBuff = localData.getBytes();
                    DatagramPacket hrdp = new DatagramPacket(localBuff, localBuff.length,
                            servers.get(receivingServer), port);
                    datagramSocket.send(hrdp);
                }
                // received heartbeat_reply message update the hash map heartbeatReplies
                else if(messageType.compareToIgnoreCase("heartbeat_reply") == 0){
                    // update the replies from server in heartbeatReplies hash map
                    heartbeatReplies.put(receivingServer, true);
                }
                // received election_reply message update the hash map electionReplies
                else if(messageType.compareToIgnoreCase("election_reply") == 0){
                    electionReplies.put(receivingServer, true);
                }
                // received coordinator message set the coordinator
                else if(messageType.compareToIgnoreCase("coordinator") == 0){
                    // update the coordinator/leader
                    currentCoordinator = receivingServer;
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    void sendingMessage() {
        while(true) {
            // send heart beat messages to the processes having id greater then the current server
            try {
                System.out.println("current coordinator is: " + currentCoordinator);
                String heartBData = "heartbeat" + "," + myServerNumber;
                byte[] heartBBuffer = heartBData.getBytes();
                resetHeartBeatAndElectionReplies(numberOfServers);

                if(myServerNumber != currentCoordinator) {
                    System.out.println("sending heartbeat message to " + currentCoordinator);
                    DatagramPacket hdp = new DatagramPacket(heartBBuffer, heartBBuffer.length,
                            servers.get(currentCoordinator), port);

                    datagramSocket.send(hdp);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // sleep for 10 secs
            try {
                System.out.println("Sleeping for 10 secs");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // after waking up check if the leader has replied
            try {
                // if leader did not reply and you are not the leader
                System.out.println("heart beat reply from coordinator " + currentCoordinator + " : "
                        + heartbeatReplies.get(currentCoordinator));
                if (!heartbeatReplies.get(currentCoordinator) &&
                        currentCoordinator != myServerNumber) {
                    System.out.println("LEADER FAILED STARTING ELECTION");
                    // send an election message to all servers whose ids are greater than my serve id
                    String etdata = "election," + myServerNumber;
                    byte[] etbuffer = etdata.getBytes();
                    for (int server : servers.keySet()) {
                        if (server > myServerNumber && server != myServerNumber) {
                            DatagramPacket etdp = new DatagramPacket(etbuffer, etbuffer.length,
                                    servers.get(server), port);

                            datagramSocket.send(etdp);
                        }
                    }
                    // sleep for the replies for elections to be received
                    Thread.sleep(10000);
                    boolean isCoordinator = true;

                    for (int server : electionReplies.keySet()) {
                        if (server > myServerNumber && server != myServerNumber
                                && electionReplies.get(server))
                            isCoordinator = false;
                    }

                    if (isCoordinator) { // send coordination message
                        System.out.println("I am the COORDINATOR");
                        currentCoordinator = myServerNumber;
                        String coData = "coordinator" + "," + myServerNumber;
                        byte[] coBuffer = coData.getBytes();
                        System.out.println("sending coordinator messages to all servers");
                        for (int server : servers.keySet()) {
                            if (server != myServerNumber) {
                                DatagramPacket cdp = new DatagramPacket(coBuffer, coBuffer.length,
                                        servers.get(server), port);
                                datagramSocket.send(cdp);
                            }
                        }
                    } else {
                        System.out.println("election done, I am not the coordinator");
                    }
                } else {
                    System.out.println("current coordinator is working");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * populate the heartbeatReplies and electionReplies hash map entries
     * @param numOfServers: count of servers involved in the network
     */
    void resetHeartBeatAndElectionReplies(int numOfServers){
        for(int i=1; i<=numOfServers;i++) {
            heartbeatReplies.put(i, false);
            electionReplies.put(i, false);
        }
    }

    public static void main(String[] args) {
        try {
            bullyLeaderElection ble = new bullyLeaderElection();
           
            System.out.println("Coordinator set to " + ble.currentCoordinator);

            Thread receiving = new Thread() {
                public void run() {
                    ble.recevingMessage();
                }
            };

            Thread sending = new Thread() {
                public void run() {
                    ble.sendingMessage();
                }
            };

            receiving.start();
            Thread.sleep(2000);
            sending.start();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}