/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NodeStatusChecker implements Runnable {
    
    private String suspectNodeName = null;
    private String thisNodeName = null;
    private volatile boolean responded = false;
    private boolean heartBeat = false;
    
    public NodeStatusChecker(String suspectNode, String thisNode) {
        suspectNodeName = suspectNode;
        thisNodeName = thisNode;    
    }
    
    public NodeStatusChecker(String suspectNode, String thisNode, boolean hb) {
        heartBeat = hb;
        suspectNodeName = suspectNode;
        thisNodeName = thisNode;
    }
    
    public void run() {
        startStatusCheck();
    }
    
    public boolean startStatusCheck() {        
        Object[] m = new Object[2];
        m[0] = "Are you alive?";
        if(heartBeat) {
            m[1] = "HEARTBEAT";
        }
        else {
            m[1] = "";
        }
        Message message = null;
        if(thisNodeName.equals("RS")) {
            message = new Message(m, ++RegistrationServer.timestamp, "RS");
            RegistrationServer.sendUDPData(message, RegistrationServer.nodeList.get(suspectNodeName));
        }
        else if(suspectNodeName.equals("RS")) {        
            Department.RSIsSuspected = true;
        
            message = new Message(m, ++Department.timestamp, thisNodeName);
            Department.sendUDPData(message, Department.regServerAddress);
        }
        else {
            message = new Message(m, ++Department.timestamp, thisNodeName);
            Department.sendUDPData(message, Department.nodeList.get(suspectNodeName));
        }
        
        System.out.println((heartBeat ? "**HEARTBEAT MODE** " : "") + "Asked Department " + suspectNodeName + " : Are you alive?");
        
        
        try {
            Thread.sleep(OperationalParameters.ESTIMATED_TIME_TO_GET_ARE_YOU_ALIVE_REPLY);
        } catch (Exception excp) {}
        
        if(!responded) {
            System.out.println("Node " + suspectNodeName + " has not responded.");
            if(suspectNodeName.equals("RS") && Department.RSIsSuspected) {
                makeANewRS();
            }
            else{                
                multicastThatANodeIsDead();
            }
            Department.suspectedNodes.remove(suspectNodeName);
            Department.suspectedNodesDuringElection.remove(suspectNodeName);
            return false;
        }
        else {
            if(suspectNodeName.equals("RS")) {
                Department.RSIsSuspected = false;
            }
            System.out.println("Node " + suspectNodeName + "'s status check completed. It is found to be operational.");
            return true;
        }
    }
    
    public void nodeHasResponded() {
        responded = true;
        //System.out.println("Node " + suspectNodeName + "'s status has set to operational.");
    }
    
    private void notifyRSThatANodeIsDead() {
        Object[] m = new Object[2];
        m[0] = "Node Is Dead";
        m[1] = suspectNodeName;
        Message message = new Message(m, ++Department.timestamp, thisNodeName);
        Department.sendUDPData(message, Department.regServerAddress);
    }
    
    private void multicastThatANodeIsDead() {       
        
        if (thisNodeName == "RS") {
            RegistrationServer.nodeList.remove(suspectNodeName);
            Object[] m = new Object[3];
            m[0] = "Node Is Dead";
            m[1] = suspectNodeName;
            m[2] = RegistrationServer.nodeList;
            Message message = new Message(m, ++RegistrationServer.timestamp, "RS");

            Set<String> nodes = RegistrationServer.nodeList.keySet();
            Iterator<String> iterator = nodes.iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                SocketAddress addr = RegistrationServer.nodeList.get(key);
                RegistrationServer.sendUDPData(message, addr);
            }            
        }
        else {
            Department.nodeList.remove(suspectNodeName);
            Object[] m = new Object[3];
            m[0] = "Node Is Dead";
            m[1] = suspectNodeName;
            m[2] = Department.nodeList;
            Message message = new Message(m, ++Department.timestamp, thisNodeName);

            Set<String> nodes = Department.nodeList.keySet();
            Iterator<String> iterator = nodes.iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if(key.equals(thisNodeName)) {
                    continue;
                }
                SocketAddress addr = Department.nodeList.get(key);
                Department.sendUDPData(message, addr);
            }
            Department.sendUDPData(message, Department.regServerAddress);
            Department.csm.adjustOnNodeDeath(suspectNodeName);
            Department.em.adjustOnNodeDeath(suspectNodeName);
        }
    }
    
    private void makeANewRS() {
        if(Department.em.StartElection()) {
            Thread t = new Thread(new NewRegistrationServer());
            t.start();
        }
    }
    
    final class NewRegistrationServer implements Runnable {
        public void run() {
            new RegistrationServer(Department.regServerPort + 1, Department.nodeList, Department.timestamp);
        }
    }
}
