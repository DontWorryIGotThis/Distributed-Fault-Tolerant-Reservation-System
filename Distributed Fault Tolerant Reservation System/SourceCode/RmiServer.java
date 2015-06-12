/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.net.*;
import java.util.Date;

public class RmiServer extends UnicastRemoteObject implements RmiServerInterface {

    int Dport;
    String Addr;
    Registry RegForLookUp;
    Department department = null;

    public String ProcessMessage(String msg) throws RemoteException {
        System.out.println(msg);
        return msg.toLowerCase();
    }

    public String EnterCriticalSection() throws RemoteException {
        System.out.println("REQUEST FROM CLIENT: Enter Critical Section.");
        
        department.csm.RequestCriticalSection();
        System.out.println("Entering Critical Section...");
        try {
            Thread.sleep(OperationalParameters.TIME_SPENT_IN_CS);
        } catch (Exception excp) {
        }
        System.out.println("Exiting Critical Section.");
        department.csm.ExitCriticalSection();
        return "Entered and exited critical section.";
    }
    
    public long GetLogicalClockValue() throws RemoteException {
        System.out.println("REQUEST FROM CLIENT: Get Logical Clock.");
        return department.timestamp;
    }
    
    public long IncrementLogicalClockValue(long x) throws RemoteException {
        System.out.println("REQUEST FROM CLIENT: Increment Logical Clock.");
        department.timestamp += x;
        return department.timestamp;
    }
    
    public String ShowReservations() throws RemoteException {
        String retVal;
        System.out.println("REQUEST FROM CLIENT: Show The Reservations.");
        department.csm.RequestCriticalSection();
        System.out.println("Entered Critical Section.");
        try {
            Thread.sleep(OperationalParameters.TIME_SPENT_IN_CS);
        } catch (Exception excp) {
        }
        
        //// Call the Book methods here
        try {
            Book bookings = new Book();
            retVal = bookings.Display();
        }catch (Exception ex) {
            retVal = "List of reservations cannot be displayed now.";
        }
        System.out.println("Exiting Critical Section.");
        department.csm.ExitCriticalSection();
        return retVal;
    }
    
    public boolean MakeAReservation(Date date) throws RemoteException {
        System.out.println("REQUEST FROM CLIENT: Make A Reservations.");
        department.csm.RequestCriticalSection();
        System.out.println("Entered Critical Section.");
        try {
            Thread.sleep(OperationalParameters.TIME_SPENT_IN_CS);
        } catch (Exception excp) {
        }
        
        //// Call the Book methods here
        boolean success;
        try {
            Book bookings = new Book();
            success = bookings.BookDate(department.name, date);
            
        }catch (Exception ex) {
            success = false;
        }        
        System.out.println("Exiting Critical Section.");
        department.csm.ExitCriticalSection();
        return success;    
    }
    
    public boolean CancelAReservation(Date date) throws RemoteException {
        System.out.println("REQUEST FROM CLIENT: Cancel A Reservations.");
        department.csm.RequestCriticalSection();
        System.out.println("Entered Critical Section.");
        try {
            Thread.sleep(OperationalParameters.TIME_SPENT_IN_CS);
        } catch (Exception excp) {
        }
        
        //// Call the Book methods here
        boolean success;
        try {
            Book bookings = new Book();
            success = bookings.cancel(date);
            
        }catch (Exception ex) {
            success = false;
        }        
        
        System.out.println("Exiting Critical Section.");
        department.csm.ExitCriticalSection();
        return success;
    }

    public RmiServer(Department dept) throws RemoteException {
        department = dept;
        try {
            Addr = (InetAddress.getLocalHost()).toString();
            System.out.println(Addr);
        } catch (Exception e) {
            throw new RemoteException("Uable to extract Inet Address");
        }

        //// To find an arbitrary available port
        try {
            ServerSocket s = new ServerSocket(0);
            Dport = s.getLocalPort();
            s.close();
            s = null;
        } catch (Exception ex) {
        }

        try {
            RegForLookUp = LocateRegistry.createRegistry(Dport);
            RegForLookUp.rebind("Dep", this);
        } catch (RemoteException e) {
            throw e;
        }
        System.out.println("The interface for client is running on port: " + Dport);
    }
    /*public static void main(String args[])
     {
     try
     {
     RmiServer Dojo = new RmiServer();
     }
     catch(Exception e)
     {
     e.printStackTrace();
     System.exit(1);
     }
     }*/
}
