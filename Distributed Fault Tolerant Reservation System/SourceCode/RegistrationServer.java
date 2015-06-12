/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class RegistrationServer {
    
    public volatile static ConcurrentHashMap<String, SocketAddress> nodeList = null;
    public static long timestamp = 0;
    public static DatagramSocket serverSocket = null;
    
    public RegistrationServer(int port, ConcurrentHashMap<String, SocketAddress> nodeList, long tstamp) {
        timestamp = tstamp;
        initialize(port, nodeList);
        
        System.out.println("RS:- I am the new RS. Notifying everyone.");
        
        Object[] m = new Object[3];
        m[0] = "New RS Created";
        try {
            m[1] = new InetSocketAddress(InetAddress.getLocalHost(), serverSocket.getLocalPort());
        } catch (Exception excp) {
            System.out.println(excp.getMessage());
        }
        m[2] = new Integer(serverSocket.getLocalPort());
        Message message = new Message(m, ++timestamp, "RS");
        
        Set<String> nodes = nodeList.keySet();
        Iterator<String> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            SocketAddress addr = nodeList.get(key);
            sendUDPData(message, addr);
        }
        
        listen();
    }
    
    public RegistrationServer(int port) {
        ConcurrentHashMap<String, SocketAddress> nl = new ConcurrentHashMap<String, SocketAddress>();
        initialize(port, nl);
        listen();
    }
    
    private void initialize(int port, ConcurrentHashMap<String, SocketAddress> nodeList) {
        this.nodeList = nodeList;
        //this.threadList = new ConcurrentHashMap<String, WorkerThread>();        
        
        while (true) {
            try {
                serverSocket = new DatagramSocket(port);
                System.out.println("RS:- Registration Server running at IP: "
                        + InetAddress.getLocalHost().toString() + " port: " + port);
                System.out.println("*****************************************************************");
                System.out.println("******* The Registration Server is running in this window *******");
                System.out.println("*****************************************************************");
                break;
            } catch (Exception excp) {
                System.err.println(excp.getMessage());
                //System.exit(1);
                System.out.println("RS:- Trying to bind to another port.");
                port++;
            }
        }
    }
    
    private void listen() {
        while (true) {
            receiveUDPData();
        }
    }
    
    private void receiveUDPData() {
        byte[] receiveData = new byte[8192];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            serverSocket.receive(receivePacket);
        } catch (Exception excp) {
            System.err.println(excp.getMessage());
        }
        SocketAddress address = receivePacket.getSocketAddress();
        
        ObjectInputStream in = null;
        Message m = null;
        try {
            in = new ObjectInputStream(new ByteArrayInputStream(receiveData));
            in.close();
            try {
                m = (Message) in.readObject();
            } catch (ClassNotFoundException ex) {
                System.out.println("RS:- Could not receive data. " + ex.getMessage());
                return;
            }
        } catch (IOException ex) {
            System.out.println("RS:- Could not receive data. " + ex.getMessage());
            return;
        }
        
        Thread t = new Thread(new WorkerThread(address, m));
        t.start();
    }
    
    public static synchronized void sendUDPData(Message m, SocketAddress address) {
        if(address == null) {
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(baos);
        } catch (IOException ex) {
            System.out.println("RS:- Could not create output stream. " + ex.getMessage());
            return;
        }
        
        try {
            out.writeObject(m);
            out.flush();
            out.close();            
        } catch (IOException ex) {
            System.out.println("RS:- Could not write to output stream. " + ex.getMessage());
            return;
        }
        
        byte[] buf = baos.toByteArray();
        try {
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address);
            serverSocket.send(sendPacket);
            //return true;
        } catch (IOException excp) {
            System.err.println("RS:- Could not send message to user.");            
            return;
        }
    }

    ///// Inner class
    final class WorkerThread implements Runnable {
        
        private SocketAddress socket = null;
        private Message request = null;
        
        public WorkerThread(SocketAddress s, Message m) {
            socket = s;            
            request = m;
        }
        
        public void run() {
            
            setLogicalClock(request.getTimestamp());
            String requestType = (String) request.getMessage()[0];
            if (requestType.equals("Join")) {
                String department = (String) request.getMessage()[1];
                InetAddress address = (InetAddress) request.getMessage()[2];
                int port = ((Integer) request.getMessage()[3]).intValue();
                
                Object[] message = null;
                if (!nodeList.containsKey(department)) {
                    nodeList.put(department, socket);
                    message = new Object[2];
                    message[0] = "Join Successful";
                    message[1] = nodeList;                    
                } else {
                    message = new Object[2];
                    message[0] = "Join Unsuccessful";   //// checks for duplicate department nodes
                    message[1] = "This department already exists as an operational node.";
                    Message response = new Message(message, ++timestamp, "RS");
                    writeToDepartment(response);
                    return;
                }
                
                Message response = new Message(message, ++timestamp, "RS");
                writeToDepartment(response);
                
                
                message = null;
                message = new Object[3];
                message[0] = "New Node Joined";
                message[1] = nodeList;
                message[2] = department;
                response = null;
                response = new Message(message, ++timestamp, "RS");
                multicastNewNodeJoin(department, response);
                System.out.println("RS:- A new department joined named: " + department);
            } else if (requestType.equals("Are you alive?")) {
                Object[] data = new Object[1];
                data[0] = "Yes I am alive";
                Message m = new Message(data, ++timestamp, "RS");
                if(request.getMessage()[1].equals("HEARTBEAT")) {
                    multicastToAllNodes(m);
                    System.out.println("RS:- " + request.getSender() + " asked me if I am alive in **HEARTBEAT MODE**. I sent reply to everyone.");
                }
                else {
                    writeToDepartment(m);
                    System.out.println("RS:- " + request.getSender() + " asked me if I am alive.");
                }
            } 
            else if(requestType.equals("Yes I am alive")) {
                System.out.println("RS:- " + request.getSender() + " just said that they are alive.");
                NodeStatusChecker nsc = Department.suspectedNodes.remove(request.getSender());
                if(nsc != null) {
                    nsc.nodeHasResponded();
                }
            }
            else if (requestType.equals("Node Is Dead")) {
                nodeList.remove(request.getMessage()[1]);
                nodeList = (ConcurrentHashMap<String, SocketAddress>)request.getMessage()[2];
                System.out.println("RS:- Node " + request.getMessage()[1] + " just passed away.");
            }
            
        }
        
        private void multicastNewNodeJoin(String except, Message response) {
            //// multicast to every department that a new node has joined
            //// 'except' the new node itself
            
            Set<String> nodes = nodeList.keySet();            
            Iterator<String> iterator = nodes.iterator();
            
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (!key.equals(except)) {
                    SocketAddress addr = nodeList.get(key);
                    writeToDepartment(response, addr);                    
                }
            }
        }
        
        private void multicastToAllNodes(Message response) {
            
            Set<String> nodes = nodeList.keySet();
            Iterator<String> iterator = nodes.iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                SocketAddress addr = nodeList.get(key);
                writeToDepartment(response, addr);
            }
        }
        
        public void writeToDepartment(Message message) {
            sendUDPData(message, socket);
        }

        public void writeToDepartment(Message message, SocketAddress addr) {
            sendUDPData(message, addr);
        }
        
        private void setLogicalClock(long recvTimestamp) {
            if (recvTimestamp > timestamp) {
                timestamp = recvTimestamp;
            }
            timestamp++;
        }
    }
}
