Abhishek - G00775054
Faridi - G00526645
Kirankumar - G00817535
Assignment 3
5/5/2013


Java Compiler version
java version "1.7.0_07"
Java(TM) SE Runtime Environment (build 1.7.0_07-b11)
Java HotSpot(TM) 64-Bit Server VM (build 23.3-b01, mixed mode)

Operating System: 
Microsoft Windows 8 Pro
Version: 6.2.9200 Build 9200



To compile, while in the source directory, issue

javac *.java


To run the Registration Server issue

java Department hostname port RS

Here, hostname can be localhost and port is the port number where the registration server will be listening.


To run any Department, issue

java Department hostname port dept-name

Here, hostname and port is where the Registration server is running. dept-name is the name of the Department, for example, CS


To run a RmiClient, issue

java RmiClient IP port

Here, IP is the address where the Department is running. Port is the port number where the Department listens to its client.
[It is different than the port that is used for inter-departmental communications.]


To inject faults, you need to press CTRL+C on that Department or RS window.


Known problems:

We have assumed that UDP packets will not get lost. Generally, it does not get lost for a single machine implementation. But,
in some cases I found that UDP packets are lost even within a single machine "localhost". When packets get lost, our system
becomes unstable. Apart from that, this system can recover from any kind of fault in at most 90 seconds.
