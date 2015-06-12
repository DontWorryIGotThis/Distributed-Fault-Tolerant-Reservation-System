/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/

import java.rmi.*;
import java.util.Date;

public interface RmiServerInterface extends Remote
{
	public String ProcessMessage(String msg) throws RemoteException;
        public String EnterCriticalSection() throws RemoteException;
        public long GetLogicalClockValue() throws RemoteException;
        public long IncrementLogicalClockValue(long x) throws RemoteException;
        public String ShowReservations() throws RemoteException;
        public boolean MakeAReservation(Date date) throws RemoteException;
        public boolean CancelAReservation(Date date) throws RemoteException;
}
