/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Department {
    
    public String name = null;
    public static long timestamp = 0;
    public static SocketAddress regServerAddress = null;
    public static int regServerPort;
    private static DatagramSocket serverSocket = null;
    
    public static ConcurrentHashMap<String, SocketAddress> nodeList = null;
    public static ConcurrentHashMap<String, Long> lastHeardTime = null;
    public static long lastCriticalSectionTime;
    public static CriticalSectionManager csm = null;
    public static ElectionManager em = null;
    public static ConcurrentHashMap<String, NodeStatusChecker> suspectedNodes = null;
    public static ConcurrentHashMap<String, NodeStatusChecker> suspectedNodesDuringElection = null;
    private static boolean silent = false;
    public static boolean RSIsSuspected = false;
    
    public Department(InetAddress ip, int port, String dname) {
        name = dname;
        regServerAddress = new InetSocketAddress(ip, port);
        regServerPort = port;
        
        try {
            serverSocket = new DatagramSocket();
            System.out.println("Department " + name + " is running at IP: " 
                + InetAddress.getLocalHost().toString() + " port: " + serverSocket.getLocalPort());
        } catch (Exception excp) {
            System.out.println("Communication system error.");
            System.exit(1);
        }        
        
        lastHeardTime = new ConcurrentHashMap<String, Long>();
        suspectedNodes = new ConcurrentHashMap<String, NodeStatusChecker>();
        suspectedNodesDuringElection = new ConcurrentHashMap<String, NodeStatusChecker>();
        csm = new CriticalSectionManager(name);
        em = new ElectionManager(name);
        
        //Thread t = new Thread(new DepartmentOps());
        //t.start();
        try {
            RmiServer Dojo = new RmiServer(this);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Registering to RS....");              
        Object[] message = new Object[4];
        message[0] = "Join";
        message[1] = name;
        message[2] = serverSocket.getLocalAddress();
        message[3] = new Integer(serverSocket.getLocalPort());
        Message request = new Message(message, ++timestamp, name);
        
        sendUDPData(request, regServerAddress);
        
        Thread hbpm = new Thread(new HeartBeatPulseManager());
        hbpm.start();
        
        while(true) {
            Message response = receiveUDPData();
            if(response == null || silent) {
                continue;
            }
            //// Bit of authentication
            if(!response.getSender().equals("RS") && !nodeList.containsKey(response.getSender())) {
                continue;
            }
            
            setLogicalClock(response.getTimestamp());
            
            lastHeardTime.put(response.getSender(), new Long(System.currentTimeMillis()));
                        
            if(((String)response.getMessage()[0]).equals("Join Successful")) {
                nodeList = (ConcurrentHashMap<String, SocketAddress>) response.getMessage()[1];                
                System.out.println("Successfully registered to the Distributed System.");
                printOnlineNodes();
            }
            else if(((String)response.getMessage()[0]).equals("Join Unsuccessful")) {                
                System.out.println("Join Unsuccessful because " + (String)response.getMessage()[1]);                
            }
            else if(((String)response.getMessage()[0]).equals("New Node Joined")) {
                nodeList = (ConcurrentHashMap<String, SocketAddress>) response.getMessage()[1];
                System.out.println("A new node joined.");
                lastHeardTime.put((String)response.getMessage()[2], new Long(System.currentTimeMillis()));
                printOnlineNodes();
            }
            else if(((String)response.getMessage()[0]).equals("Node Is Dead")) {                
                nodeList = (ConcurrentHashMap<String, SocketAddress>) response.getMessage()[2];
                System.out.println((String)response.getMessage()[1] + " just passed away.");
                lastHeardTime.remove((String)response.getMessage()[1]);
                csm.adjustOnNodeDeath((String)response.getMessage()[1]);
                em.adjustOnNodeDeath((String)response.getMessage()[1]);
            }
            else if(((String)response.getMessage()[0]).equals("New RS Created")) {
                RSIsSuspected = false;
                regServerAddress = (SocketAddress)response.getMessage()[1];
                regServerPort = (Integer)response.getMessage()[2];
                em.newRSCreated();
                System.out.println("The new RS just sent its address and port to be " + regServerAddress.toString());
            }
            else if(((String)response.getMessage()[0]).contains("Critical Section")) {
                csm.processMessage(response);
            }
            else if(((String)response.getMessage()[0]).contains("Election")) {
                em.processMessage(response);
            }
            else if(((String)response.getMessage()[0]).equals("Are you alive?")) {
                Object[] data = new Object[1];
                data[0] = "Yes I am alive";
                Message m = new Message(data, ++timestamp, name);
                if(response.getMessage()[1].equals("HEARTBEAT")) { 
                    multicastToAllNodes(m);
                    System.out.println(response.getSender() + " asked me if I am alive in **HEARTBEAT MODE**. I sent reply to everyone.");
                }
                else {
                    sendUDPData(m, nodeList.get(response.getSender()));
                    System.out.println(response.getSender() + " asked me if I am alive.");
                }
            }
            else if(((String)response.getMessage()[0]).equals("Yes I am alive")) {                
                System.out.println(response.getSender() + " just said that they are alive.");
                NodeStatusChecker nsc = suspectedNodes.remove(response.getSender());
                if(nsc != null) {
                    nsc.nodeHasResponded();
                }
                nsc = null;
                nsc = suspectedNodesDuringElection.remove(response.getSender());
                if(nsc != null) {
                    nsc.nodeHasResponded();
                }
            }
        }
    }
    
    private Message receiveUDPData() {
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
                System.out.println("Class not found. " + ex.getMessage());
                return null;
            }
        } catch (IOException ex) {
            System.out.println("Could not receive data. " + ex.getMessage());
                return null;
        }
        
        return m;
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
            System.out.println("Could not create output stream. " + ex.getMessage());
            return;
        }
        
        try {
            out.writeObject(m);
            out.flush();
            out.close();            
        } catch (IOException ex) {
            System.out.println("Could not write to output stream. " + ex.getMessage());
            return;
        }
        
        byte[] buf = baos.toByteArray();
        try {
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address);
            serverSocket.send(sendPacket);
            //return true;
        } catch (IOException excp) {
            System.err.println("Could not send message to user.");            
            return;
        }
    }
    
    private void multicastToAllNodes(Message response) {

        Set<String> nodes = nodeList.keySet();
        Iterator<String> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if(key.equals(name)) {
                continue;
            }
            SocketAddress addr = nodeList.get(key);
            sendUDPData(response, addr);
        }
    }
    
    private void printOnlineNodes() {

        Set<String> nodes = nodeList.keySet();
        System.out.println("Timestamp: " + timestamp);
        System.out.println("List of other connected nodes:");
        if (nodes.size() == 1) {
            System.out.println("<<empty>>");
            return;
        }

        Iterator<String> iterator = nodes.iterator();        

        while (iterator.hasNext()) {
            String key = iterator.next();
            if(!key.equals(name)) {
            SocketAddress deptAddr = nodeList.get(key);
                System.out.println("Dept. name: " + key + " Address: " + deptAddr.toString());
            }
        }

    }
    
    public synchronized static void setLogicalClock(long recvTimestamp) {
            if(recvTimestamp > timestamp) {
                timestamp = recvTimestamp;
            }
            timestamp++;
        }
    
    public static void main(String[] args) {
        if(args.length < 3) {
            System.out.println("Invalid number of arguments!");
            System.out.println("java Department host port department/RS");
            return;
        }
        InetAddress IP = null;
        try {
            IP = InetAddress.getByName(args[0]);
        } catch (Exception excp) {
            System.out.println("Invalid IP address.");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (Exception excp) {
            System.out.println("Invalid port number.");
            return;
        }
        
        String dname = "";
        for (int i = 2; i < args.length; i++) {
            dname = dname + args[i];
            if(i != args.length - 1) {
                dname = dname + " ";
            }
        }
        
        if(dname.equals("RS")) {
            new RegistrationServer(port);
        }
        else {
            new Department(IP, port, dname);
        }
    }
    
    
    final class HeartBeatPulseManager implements Runnable {
    
        
        public void run() {
            
            while (true) {
                
                try {
                    Thread.sleep(OperationalParameters.HEARTBEAT_INTERVAL);
                } catch (Exception ex) {
                }
                long currentTime = System.currentTimeMillis();
                
                if (currentTime - lastCriticalSectionTime > OperationalParameters.HEARTBEAT_INTERVAL) {
                    Set<String> nodes = nodeList.keySet();
                    Iterator<String> iterator = nodes.iterator();

                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        if (key.equals(name)) {
                            continue;
                        }
                        if (lastHeardTime.get(key) != null && 
                                currentTime - lastHeardTime.get(key) > OperationalParameters.HEARTBEAT_INTERVAL) {
                            System.out.println("Haven't heard from Department " + key
                                    + " for at least " + (((currentTime - lastHeardTime.get(key))) / 1000)
                                    + " seconds. Checking its status");
                            NodeStatusChecker nsc = new NodeStatusChecker(key, name, true);
                            Department.suspectedNodes.put(key, nsc);
                            Thread t = new Thread(nsc);
                            t.start();                            
                        }                        
                    }
                    if(lastHeardTime.get("RS") == null) {
                        lastHeardTime.put("RS", currentTime);
                    }
                    if (currentTime - lastHeardTime.get("RS") > OperationalParameters.RS_CHECK_INTV_ON_REGULAR_OP) {
                        System.out.println("Haven't heard from RS "
                                + " for at least " + (((currentTime - lastHeardTime.get("RS"))) / 1000)
                                + " seconds. Checking its status");
                        NodeStatusChecker nsc = new NodeStatusChecker("RS", name, true);
                        Department.suspectedNodes.put("RS", nsc);
                        Thread t = new Thread(nsc);
                        t.start();
                    }
                }
            }
        }
    }
    
    final class DepartmentOps implements Runnable {
    
        public DepartmentOps() {
        
        }
        
        public void run() {
            Scanner in = new Scanner(System.in);
            while (true) {
                String cmd = in.nextLine();
                if(silent) {
                    continue;
                }
                if(cmd.equalsIgnoreCase("enter")) {
                    csm.RequestCriticalSection();
                    System.out.println("Entering Critical Section...");
                    try {
                        Thread.sleep(OperationalParameters.TIME_SPENT_IN_CS);
                    } catch (Exception excp) {
                    
                    }
                    System.out.println("Exiting Critical Section.");
                    csm.ExitCriticalSection();
                }
                else if(cmd.equalsIgnoreCase("clock++")) {
                    int x = in.nextInt();
                    timestamp += x;
                }
                else if(cmd.equalsIgnoreCase("die")) {
                    silent = true;
                }
                
                else if(cmd.equalsIgnoreCase("back")) {
                    silent = false;
                }
                else if(cmd.equalsIgnoreCase("show")) {
                    printOnlineNodes();
                }
            }
        }
    }
}




