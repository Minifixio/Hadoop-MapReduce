package rs;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.log4j.PropertyConfigurator;

public class Slave {

    public static void main(String[] args) {
        startFTPServer();
        startSocketServer();
    }

    public static void startSocketServer() {
       ServerSocket listener = null;
       String line;
       BufferedReader is;
       BufferedWriter os;
       Socket socketOfServer = null;

       // Try to open a server socket on port 9999
       // Note that we can't choose a port less than 1023 if we are not
       // privileged users (root)
 
       try {
           listener = new ServerSocket(9999);
       } catch (IOException e) {
           System.out.println(e);
           System.exit(1);
       }

       try {
           System.out.println("Server is waiting to accept user...");

           // Accept client connection request
           // Get new Socket at Server.    
           socketOfServer = listener.accept();
           System.out.println("Accept a client!");

           // Open input and output streams
           is = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
           os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));

           while (true) {
               // Read data to the server (sent from client).
               line = is.readLine();
               
               // Write to socket of Server
               // (Send to client)
               os.write(">> " + line);
               // End of line
               os.newLine();
               // Flush data.
               os.flush();  

               // If users send QUIT (To end conversation).
               if (line.equals("QUIT")) {
                   os.write(">> OK");
                   os.newLine();
                   os.flush();
                   break;
               }
           }

       } catch (IOException e) {
           System.out.println(e);
           e.printStackTrace();
       }
       System.out.println("Sever stopped!");
    }

    public static void startFTPServer() {
        PropertyConfigurator.configure(Slave.class.getResource("/log4J.properties"));
        FtpServerFactory serverFactory = new FtpServerFactory();
        int port = 3456; // FTP Port

        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(port);
        serverFactory.addListener("default", listenerFactory.createListener());

        // Create a UserManager instance
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        File userFile = new File("users.properties");
        if (!userFile.exists()) {
            try {
                if (userFile.createNewFile()) {
                    System.out.println("File created: " + userFile.getName());
                } else {
                    System.out.println("File already exists.");
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }
        
        userManagerFactory.setFile(userFile); // Specify the file to store user details
        userManagerFactory.setPasswordEncryptor(new ClearTextPasswordEncryptor()); // Store plain text passwords
        UserManager userManager = userManagerFactory.createUserManager();
        
        // Create a user
        BaseUser user = new BaseUser();
        user.setName("elegallic-22");
        user.setPassword("slr207wew");
        String username = user.getName();
        String homeDirectory = System.getProperty("java.io.tmpdir") + "/elegallic-22/" + username;
        File directory = new File(homeDirectory); // Convert the string to a File object
        if (!directory.exists()) { // Check if the directory exists
            if (directory.mkdirs()) {
                System.out.println("Directory created: " + directory.getAbsolutePath());
            } else {
                System.out.println("Failed to create directory.");
            }
        }
        user.setHomeDirectory(homeDirectory);

        // Set write permissions for the user
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        user.setHomeDirectory(homeDirectory);

        // Add the user to the user manager
        try {
            userManager.save(user);
        } catch (FtpException e) {
            e.printStackTrace();
        }
        // Set the user manager on the server context
        serverFactory.setUserManager(userManager);
        FtpServer server = serverFactory.createServer();

        // start the FTP server
        try {
            server.start();
            System.out.println("FTP Server started on port " + port);
            
        } catch (FtpException e) {
            e.printStackTrace();
        }
    }
}