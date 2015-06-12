/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/

import java.util.*;
import java.net.*;

public class ElectionManager {
    
    
    private String deptName = null;
    private ArrayList<String> repliesNotReceived = null;
    boolean interested = false;
    //boolean RSFailed = false;
    private long requestTimestamp;
    
    
    public ElectionManager(String deptName) {
        this.deptName = deptName;        
        repliesNotReceived = new ArrayList<String>();
    }
    
    public synchronized boolean StartElection() {
        if(interested)
        {
            return false;
        }
        interested = true;
        
        Object[] message = new Object[1];
        message[0] = "Election Request";
        requestTimestamp = ++Department.timestamp;
        System.out.println("Election Request initiated. Timestamp: " + requestTimestamp);
        /*System.out.println("*********************************** Now ************************");
        try {
            Thread.sleep(3000);
        } catch (Exception ex) {}
        */
        Message request = new Message(message, requestTimestamp, deptName);
        
        Set<String> nodes = Department.nodeList.keySet();
        if (nodes.size() == 1) {
            return true;
        }

        Iterator<String> iterator = nodes.iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!key.equals(deptName)) {
                SocketAddress deptAddr = Department.nodeList.get(key);
                Department.sendUDPData(request, deptAddr);
                repliesNotReceived.add(key);
                System.out.println("Election request sent to the department: " + key);
            }
        }             
        
        while(!repliesNotReceived.isEmpty()) {
            try {
                wait();
            } catch (Exception excp) {
                System.out.println(excp.getMessage());
            }
            
            if(repliesNotReceived.size() == 1) {
                Thread t = new Thread(new TimeoutChecker());
                t.start();
            }
        }
        
        if(interested) {
            interested = false;
            return true;
        }
        else {
            return false;
        }
    }
    
    public synchronized void processMessage(Message m) {
        
        if(m.getMessage()[0].equals("Election Reply")) {
            System.out.println("Election Reply received from department: " + m.getSender());            
            repliesNotReceived.remove(m.getSender());
            if(repliesNotReceived.isEmpty() || repliesNotReceived.size() == 1) {
                notify();
            }
        }
        
        else if(m.getMessage()[0].equals("Election Request")) {
            System.out.println("Department: " + m.getSender() + " is requesting an Election with Timestamp: " + m.getTimestamp());
            
            if(interested) {
                if(hasHigherPriority(m)) {
                    System.out.println(m.getSender() + " has higher priority to be the RS.");
                    sendElectionReply(m.getSender());
                    System.out.println("Election Reply sent to department: " + m.getSender() + " who's timestamp was: " + m.getTimestamp());
                }                
            }
            else {
                sendElectionReply(m.getSender());
                System.out.println("Election Reply sent to department: " + m.getSender() + " who's timestamp was: " + m.getTimestamp());
            }
        }
    }
    
    private boolean hasHigherPriority(Message m) {
        if(m.getTimestamp() < requestTimestamp) {
            return true;
        }
        if(m.getTimestamp() == requestTimestamp) {
            if(m.getSender().compareTo(deptName) < 0) {
                return true;
            }            
        }        
        return false;
    }
    
    private void sendElectionReply(String requester) {
        Object[] message = new Object[1];
        message[0] = "Election Reply";
        Message reply = new Message(message, ++Department.timestamp, deptName);
        SocketAddress deptAddr = Department.nodeList.get(requester);
        Department.sendUDPData(reply, deptAddr);
    }
    
    
    
    public synchronized void adjustOnNodeDeath(String deadNodeName) {        
        repliesNotReceived.remove(deadNodeName);
        if(repliesNotReceived.isEmpty() || repliesNotReceived.size() == 1) {
            notify();
        }
    }
    
    public synchronized void newRSCreated() {
        interested = false;
        repliesNotReceived.clear();
        notify();
    }
    
    final class TimeoutChecker implements Runnable {
        
        public void run() {
            try {
                Thread.sleep(OperationalParameters.CHECK_INTV_ON_WAITING_FOR_CS_REPLY);
            } catch (Exception ex) {
            }
            while (repliesNotReceived.size() == 1) {

                System.out.println("Department " + repliesNotReceived.get(0)
                        + " is not replying to my Election Request message for at least " 
                        + (OperationalParameters.CHECK_INTV_ON_WAITING_FOR_CS_REPLY / 1000)
                        + " seconds.");
                NodeStatusChecker nsc = new NodeStatusChecker(repliesNotReceived.get(0), deptName);
                Department.suspectedNodesDuringElection.put(repliesNotReceived.get(0), nsc);
                if (nsc.startStatusCheck() == false) {
                    break;
                }
                try {
                    Thread.sleep(OperationalParameters.CHECK_INTV_ON_WAITING_FOR_CS_REPLY);
                } catch (Exception ex) {
                }
            }
        }
    }
}
