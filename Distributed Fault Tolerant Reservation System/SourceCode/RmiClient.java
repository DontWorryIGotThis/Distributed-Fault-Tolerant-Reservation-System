/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/

import java.rmi.*;
import java.rmi.registry.*;
import java.net.*;
import java.util.*;
import java.util.Scanner;
import java.util.StringTokenizer;

public class RmiClient {

    public static void main(String args[]) {
        RmiServerInterface DServer;
        Registry RegFromServer;
        Scanner in = new Scanner(System.in);
        //String Daddr;
        //Integer Dport = Integer.parseInt(args[1]);

        if (args.length < 2) {
            System.out.println("Invalid number of arguments!");
            System.out.println("java RmiClient host port");
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

        String cmd;
        String response = "";
        while (true) {
            System.out.print("Please Enter command: ");
            cmd = in.nextLine();
            try {
                //System.out.println("Tranmitting the command " + cmd + " to " + IP + ":" + port);
                RegFromServer = LocateRegistry.getRegistry(IP.getHostAddress(), port);
                DServer = (RmiServerInterface) (RegFromServer.lookup("Dep"));

                StringTokenizer st = new StringTokenizer(cmd, " ", false);
                String cmdStr = null;
                if (st.hasMoreTokens()) {
                    cmdStr = st.nextToken();
                } else {
                    cmdStr = cmd;
                }

                if (cmdStr == null || cmdStr.isEmpty()) {
                    System.out.println("Unrecognized command.");
                    continue;
                }

                if (cmdStr.compareToIgnoreCase("clock+") == 0) {                    
                        if (st.countTokens() == 1) {
                            long newValue = DServer.IncrementLogicalClockValue(new Long(st.nextToken()));
                            System.out.println("The new logical clock value is now " + newValue);
                        } else {
                            System.out.println("A valid clock value expected.");
                            continue;
                        }
                    
                }
                else if (cmdStr.compareToIgnoreCase("clock") == 0) {                    
                        long value = DServer.GetLogicalClockValue();
                        System.out.println("The logical clock value is now " + value);
                    
                }
                else if (cmdStr.compareToIgnoreCase("show") == 0) {                    //// show the list of reservation
                        response = DServer.ShowReservations();
                        System.out.println(response);
                }
                else if (cmdStr.compareToIgnoreCase("reserve") == 0) {
                    if (st.countTokens() == 1) {
                        String d = st.nextToken();
                        try {                            
                            int year = Integer.parseInt(d.substring(0, 4));
                            int month = Integer.parseInt(d.substring(4, 6));
                            int day = Integer.parseInt(d.substring(6, 8));
                            
                            Calendar cal = Calendar.getInstance();
                            cal.set(year, month, day);
                            Date date = cal.getTime();

                            boolean success = DServer.MakeAReservation(date);
                            if (success) {
                                System.out.println("Reservation successful.");
                            } else {
                                System.out.println("Date is not available.");
                            }
                        } catch (Exception ex) {
                            System.out.println("Invalid parameters.");
                        }

                    } else {
                        System.out.println("A valid value expected.");
                    }

                } else if (cmd.equalsIgnoreCase("enter")) {
                    response = DServer.EnterCriticalSection();
                } else if (cmdStr.compareToIgnoreCase("cancel") == 0) {
                    if (st.countTokens() == 1) {
                        String d = st.nextToken();
                        try {
                            int year = Integer.parseInt(d.substring(0, 4));
                            int month = Integer.parseInt(d.substring(4, 6));
                            int day = Integer.parseInt(d.substring(6, 8));

                            Calendar cal = Calendar.getInstance();
                            cal.set(year, month, day);
                            Date date = cal.getTime();

                            boolean success = DServer.CancelAReservation(date);
                            if (success) {
                                System.out.println("Cancelation successful.");
                            } else {
                                System.out.println("Reservation not found.");
                            }
                        } catch (Exception ex) {
                            System.out.println("Invalid parameters.");
                        }

                    } else {
                        System.out.println("A valid value expected.");
                    }

                }     

                //System.out.println(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
