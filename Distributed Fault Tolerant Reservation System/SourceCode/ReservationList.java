/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/

import java.util.*;
import java.io.*;

public class ReservationList {
    HashMap<Date, String> reservations = null;
    
    public ReservationList()
    {
        reservations = new HashMap<Date, String>();        
    }
    
    public void addReservation(String dname, Date date) {        
        reservations.put(date, dname);
    }
}

/*
final class Reservation implements Serializable {
    String department = null;
    java.util.Date date = null;
    
    public Reservation (String dname, Date dt) {
        department = dname;
        date = dt;
    }
}
*/
