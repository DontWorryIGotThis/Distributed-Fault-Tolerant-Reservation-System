/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/

import java.util.Date;
import java.io.File;
import java.io.BufferedOutputStream;
import java.lang.Exception;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.util.Set;
import java.util.Iterator;

public class Book {

    String SimpleDate;

    Book() throws FileNotFoundException {
    }

    public boolean BookDate(String DptName, Date x) // Return value has been set to Boolean incase you people want to verify the Confirmation. 
    {
        Date p = new Date(x.getTime());

        SimpleDate = p.toString().substring(4, 7) + " " + p.toString().substring(8, 10) + " " + p.toString().substring(24, 28);
        HashMap Content;
        try {

            if (FileExists("Schedule.dat")) {
                File Schedule = new File("Schedule.dat");
                //System.out.println("The length of Schedule.dat is "+Schedule.length());
                if ((Schedule.length() == 0)) {
                    Content = new HashMap<String, String>();
                    Content.put(SimpleDate, DptName);
                    ObjectOutputStream pub = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("Schedule.dat", false)));
                    pub.writeObject(Content);
                    pub.close();
                    System.out.println("The System has sucessfully booked a date on " + SimpleDate + " for the " + DptName + " Department");
                    return true;
                } else {

                    if (IsAvailable(SimpleDate)) {
                        ObjectInputStream getMap = new ObjectInputStream(new FileInputStream("Schedule.dat"));
                        Content = (HashMap<String, String>) getMap.readObject();
                        Content.put(SimpleDate, DptName);
                        ObjectOutputStream pub = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("Schedule.dat")));
                        pub.writeObject(Content);
                        pub.close();
                        System.out.println("The System has sucessfully booked a date on " + SimpleDate + " for the " + DptName + " Department");
                        return true;
                    } else {
                        System.out.println("The System was not successful in booking a date on" + SimpleDate + "for the" + DptName + "Department");
                        return false;
                    }

                }

            } else {
                File Schedule = new File("Schedule.dat");
                Schedule.createNewFile();
                Content = new HashMap<String, String>();
                Content.put(SimpleDate, DptName);
                ObjectOutputStream pub = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(Schedule, false)));
                pub.writeObject(Content);
                pub.close();
                System.out.println("The System has sucessfully booked a date on " + SimpleDate + " for the " + DptName + " Department");
                return true;
            }


        } catch (Exception r) {
            r.printStackTrace();
            return false; 
        }
    }

    public boolean FileExists(String r) {
        File ReqFile = new File(r);
        return (ReqFile.exists());
    }

    public boolean IsAvailable(String ChkDate) // the date consists of the time of the day which is making the key different everytime. 
    {
        try {
            HashMap<String, String> Content;
            ObjectInputStream getMap = new ObjectInputStream(new FileInputStream("Schedule.dat"));
            Content = (HashMap<String, String>) getMap.readObject();
            if (Content.get(ChkDate) == null) {
                return true;
            } else {
                return false;
            }

        } catch (Exception l) {
            l.printStackTrace();
            return false;
        }

    }

    public String Display() {
        String retVal = "";
        try {
            HashMap BookedNodes;
            if (FileExists("Schedule.dat")) {
                File Schedule = new File("Schedule.dat");
                if ((Schedule.length() == 0)) {
                    //System.out.println("NO Department has booked a date till now");  
                    retVal = "NO Department has booked a date till now.";
                } else {
                    ObjectInputStream getMap = new ObjectInputStream(new FileInputStream("Schedule.dat"));
                    BookedNodes = (HashMap<String, String>) getMap.readObject();
                    Set<String> BookedNodesKeySet = BookedNodes.keySet();
                    Iterator<String> KeyIterator = BookedNodesKeySet.iterator();
                    //System.out.println("The Schedule is as follows:");
                    retVal += "The Schedule is as follows:\r\n";
                    while (KeyIterator.hasNext()) {
                        //System.out.println(BookedNodes.get(KeyIterator.next()));
                        String key = (String) KeyIterator.next();
                        retVal += BookedNodes.get(key) + " " + key + "\r\n";
                    }
                }
            } else {
                //System.out.println("NO Department has booked a date till now");
                retVal = "NO Department has booked a date till now.";
            }
        } catch (Exception e) {
        }
        return retVal;
    }

    public boolean cancel(Date x) {
        try {

            if (FileExists("Schedule.dat")) {
                ObjectInputStream getMap = new ObjectInputStream(new FileInputStream("Schedule.dat"));
                HashMap BookedNodes = (HashMap<String, String>) getMap.readObject();
                String SimDate = x.toString().substring(4, 7) + " " + x.toString().substring(8, 10) + " " + x.toString().substring(24, 28);
                BookedNodes.remove(SimDate);
                ObjectOutputStream pub = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("Schedule.dat", false)));
                pub.writeObject(BookedNodes);
                pub.close();
                return true;
            } else {
                System.out.println("No Department has booked a date till now");
                return false;
            }

        } catch (Exception e) {
            return false;
        }       
    }

    /*public static void main(String[] args) throws FileNotFoundException {
        Book x = new Book();
        Date d = new Date();
        x.BookDate("WooHoo", d);
        x.Display();
        x.cancel(d);
    }*/
}
