/*
Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013
*/

import java.util.*;
import java.io.*;

public class Message implements Serializable {
    private long timestamp;     //// logical clock
    private String sender;
    private Object[] message = null;
    
    public Message(Object[] m, long tstamp, String sender) {
        message = m.clone();
        timestamp = tstamp;
        this.sender = sender;
    }
    
    public Object[] getMessage() {
        return message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getSender() {
        return sender;
    }
}
