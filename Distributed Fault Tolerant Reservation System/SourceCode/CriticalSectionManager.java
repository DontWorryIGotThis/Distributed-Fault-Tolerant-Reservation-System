/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.*;

public class CriticalSectionManager {
    
    private String deptName = null;
    private ArrayList<String> repliesNotReceived = null;
    private boolean interested = false;
    private long requestTimestamp;
    private ConcurrentLinkedQueue<String> deferredQueue = null;
    
    public CriticalSectionManager(String deptName) {
        this.deptName = deptName;
        deferredQueue = new ConcurrentLinkedQueue<String>();
        repliesNotReceived = new ArrayList<String>();
    }
    
    public synchronized void RequestCriticalSection() {
        
        if (Department.lastHeardTime.get("RS") != null && 
                System.currentTimeMillis() - Department.lastHeardTime.get("RS") > OperationalParameters.RS_CHECK_INTV_ON_REGULAR_OP) {
            System.out.println("Haven't heard from RS "
                    + " for at least " + (OperationalParameters.RS_CHECK_INTV_ON_REGULAR_OP / 1000) + " seconds. Checking its status");
            NodeStatusChecker nsc = new NodeStatusChecker("RS", deptName);
            Department.suspectedNodes.put("RS", nsc);
            Thread t = new Thread(nsc);
            t.start();

        }
        
        Department.lastCriticalSectionTime = System.currentTimeMillis();
        interested = true;
        Object[] message = new Object[1];
        message[0] = "Request Critical Section";
        requestTimestamp = ++Department.timestamp;
        System.out.println("Critical Section Request initiated. Timestamp: " + requestTimestamp);
        Message request = new Message(message, requestTimestamp, deptName);
        
        Set<String> nodes = Department.nodeList.keySet();
        if (nodes.size() == 1) {
            return;
        }

        Iterator<String> iterator = nodes.iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!key.equals(deptName)) {
                SocketAddress deptAddr = Department.nodeList.get(key);
                Department.sendUDPData(request, deptAddr);
                repliesNotReceived.add(key);
                System.out.println("Request for critical section sent to the department: " + key);
            }
        }
        
        
        
        while(!repliesNotReceived.isEmpty()) {
            try {
                wait();
            } catch (Exception excp) {
                System.out.println(excp.getMessage());
            }
            
            if(repliesNotReceived.size() == 2) {
                Thread t1 = new Thread(new TimeoutChecker(repliesNotReceived.get(0)));
                t1.start();
                Thread t2 = new Thread(new TimeoutChecker(repliesNotReceived.get(1)));
                t2.start();
            }
            else if(repliesNotReceived.size() == 1) {
                Thread t1 = new Thread(new TimeoutChecker(repliesNotReceived.get(0)));
                t1.start();
            }
        }
    }
    
    public synchronized void processMessage(Message m) {
        
        if(m.getMessage()[0].equals("Reply Critical Section")) {
            System.out.println("Permission to enter Critical Section received from department: " + m.getSender());            
            repliesNotReceived.remove(m.getSender());
            if(repliesNotReceived.isEmpty() || repliesNotReceived.size() == 1
                    || repliesNotReceived.size() == 2) {
                notify();
            }
        }
        
        else if(m.getMessage()[0].equals("Request Critical Section")) {
            Department.lastCriticalSectionTime = System.currentTimeMillis();
            System.out.println("Department: " + m.getSender() + " is requesting Critical Section with Timestamp: " + m.getTimestamp());
            
            if(interested) {
                //if(hasHigherPriority(m) && !repliesNotReceived.isEmpty() && repliesNotReceived.contains(m.getSender())) {
                if(hasHigherPriority(m) && !repliesNotReceived.isEmpty() && repliesNotReceived.contains(m.getSender())) {
                    sendPermissionToEnter(m.getSender());
                    System.out.println("Reply sent to department: " + m.getSender() + " who's timestamp was: " + m.getTimestamp());
                }
                else {
                    deferRequest(m.getSender());
                    System.out.println(m.getSender() + "'s request deferred.");
                }
            }
            else {
                sendPermissionToEnter(m.getSender());
                System.out.println("Reply sent to department: " + m.getSender() + " who's timestamp was: " + m.getTimestamp());
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
    
    private void sendPermissionToEnter(String requester) {
        Object[] message = new Object[1];
        message[0] = "Reply Critical Section";
        Message reply = new Message(message, ++Department.timestamp, deptName);
        SocketAddress deptAddr = Department.nodeList.get(requester);
        Department.sendUDPData(reply, deptAddr);
    }
    
    private void deferRequest(String requester) {
        deferredQueue.add(requester);
        System.out.println("Reply to department: " + requester + " is deferred.");
    }
    
    public void ExitCriticalSection() {
        interested = false;
        Object[] message = new Object[1];
        message[0] = "Reply Critical Section";
        Message reply = new Message(message, ++Department.timestamp, deptName);
        
        while(!deferredQueue.isEmpty()) {
            String key = deferredQueue.remove();
            SocketAddress deptAddr = Department.nodeList.get(key);
            Department.sendUDPData(reply, deptAddr);
            System.out.println("Reply to department: " + key + " is sent (which was deferred).");
        }
    }
    
    public synchronized void adjustOnNodeDeath(String deadNodeName) {
        deferredQueue.remove(deadNodeName);
        repliesNotReceived.remove(deadNodeName);
        if(repliesNotReceived.isEmpty() || repliesNotReceived.size() == 1) {
            notify();
        }
    }
    
    final class TimeoutChecker implements Runnable {
        String suspectNode = "";
        public TimeoutChecker(String suspectNodeName) {
            suspectNode = suspectNodeName;
        }
        
        public void run() {
            try {
                Thread.sleep(OperationalParameters.CHECK_INTV_ON_WAITING_FOR_CS_REPLY);
            } catch (Exception ex) {
            }
            while (repliesNotReceived.contains(suspectNode)) {

                System.out.println("Department " + suspectNode
                        + " is not replying to my Critical Section message for at least "
                        + (OperationalParameters.CHECK_INTV_ON_WAITING_FOR_CS_REPLY / 1000)
                        + " seconds.");

                NodeStatusChecker nsc = new NodeStatusChecker(suspectNode, deptName);
                Department.suspectedNodes.put(suspectNode, nsc);
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
