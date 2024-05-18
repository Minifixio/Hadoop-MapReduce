package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
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

public class CommunicationHandler {

    private static boolean DEBUG = true;
    
    /**
     * Socket variables
     */
    private final int SOCKET_PORT = 9999; // We can't choose a port less than 1023 if we are not privileged users (root)

    private ServerSocket serverSocket = null;
    private SocketThread socketThread = null;
    private Socket masterSocket = null;
    private PrintWriter osSocket = null;
    private BufferedReader isSocket = null;


    /**
     * FTP variables
     */
    private static final int FTP_PORT = 3456;
    private static final String FTP_USERNAME = "elegallic-22";
    private static final String FTP_PASSWORD = "slr207wew";

    private FtpServer FTPserver;
    private String FTPDirectory;

    public CommunicationHandler() {
        initFTP();
        initSocket();
    }

    private void initFTP() {
        PropertyConfigurator.configure(Slave.class.getResource("/log4J.properties"));
        FtpServerFactory serverFactory = new FtpServerFactory();

        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(FTP_PORT);
        serverFactory.addListener("default", listenerFactory.createListener());

        // Create a UserManager instance
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        File userFile = new File("users.properties");
        if (!userFile.exists()) {
            try {
                if (userFile.createNewFile()) {
                    System.out.println("[FTP] User propreties file created: " + userFile.getName());
                } else {
                    System.out.println("[FTP] User propreties file already exists.");
                }
            } catch (IOException e) {
                System.out.println("[FTP] An error occurred.");
                e.printStackTrace();
            }
        }
        
        userManagerFactory.setFile(userFile); // Specify the file to store user details
        userManagerFactory.setPasswordEncryptor(new ClearTextPasswordEncryptor()); // Store plain text passwords
        UserManager userManager = userManagerFactory.createUserManager();
        
        // Create a user
        BaseUser user = new BaseUser();
        user.setName(FTP_USERNAME);
        user.setPassword(FTP_PASSWORD); 
        String username = user.getName();
        
        if (DEBUG) {
            FTPDirectory = System.getProperty("user.dir");
        } else {
            FTPDirectory = System.getProperty("java.io.tmpdir") + "/"+ FTP_USERNAME +"/" + username;
        }

        File directory = new File(FTPDirectory); // Convert the string to a File object
        if (!directory.exists()) { // Check if the directory exists
            if (directory.mkdirs()) {
                System.out.println("[FTP] FTP directory created: " + directory.getAbsolutePath());
            } else {
                System.out.println("[FTP] Failed to create FTP directory.");
            }
        }
        user.setHomeDirectory(FTPDirectory);

        // Set write permissions for the user
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        user.setHomeDirectory(FTPDirectory);

        // Add the user to the user manager
        try {
            userManager.save(user);
        } catch (FtpException e) {
            e.printStackTrace();
        }
        // Set the user manager on the server context
        serverFactory.setUserManager(userManager);
        
        FTPserver = serverFactory.createServer();
        // start the FTP server
        try {
            FTPserver.start();
            System.out.println("[FTP] Server started on port " + FTP_PORT);
        } catch (FtpException e) {
            e.printStackTrace();
        }
    }

    public void sendFTPFile(String hostname, String sentFileName, String filePath) {

        FTPClient FTPClient = new FTPClient();

        try {
            FTPClient.connect(hostname, FTP_PORT);
            FTPClient.login(FTP_USERNAME, FTP_PASSWORD);
            FTPClient.enterLocalPassiveMode();
            FTPClient.setFileType(FTP.BINARY_FILE_TYPE);
            System.out.println("[FTP] Connected to " + hostname + " on port " + FTP_PORT);

            System.out.println("[FTP] Uploading file: " + filePath);
            try (FileInputStream inputStream = new FileInputStream(filePath)) {
                FTPClient.storeFile(sentFileName, inputStream);
                System.out.println("[FTP] File uploaded successfully.");
            } catch (IOException e) {
                System.err.println("[FTP] Error uploading file: " + e.getMessage());
            }
            
            FTPClient.logout();
            FTPClient.disconnect();
        } catch (Exception e) {
            System.err.println("[FTP] Error connecting to " + hostname + " on port " + FTP_PORT);
            e.printStackTrace();
        }
    }

    public void listFTPFiles() {
        System.out.println("[FTP] Listing files in in the directory: " + FTPDirectory);
        File directory = new File(FTPDirectory);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                System.out.println(file.getName());
            }
        }
    }

    public String getFTPDirectory() {
        return FTPDirectory;
    }

    private void initSocket() {
       try {
            serverSocket = new ServerSocket(SOCKET_PORT);
       } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
       }

       try {
            System.out.println("[Socket] Server is waiting to accept user...");

            // Accept client connection request
            // Get new Socket at Server.
            masterSocket = serverSocket.accept();
            System.out.println("[Socket] Master reached the server.");

            // Open input and output streams
            osSocket = new PrintWriter(masterSocket.getOutputStream(), true);
            isSocket = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()));

            // Start thread of SocketThread to parse commands
            socketThread = new SocketThread(isSocket, osSocket);
            socketThread.start();

       } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
       }
    }

    public void sendProtocolMessage(ProtocolMessage message) {
        osSocket.println(message.toString());
    }

    public void disconnectSocket() {
        try {
            socketThread.terminate();
            osSocket.close();
            isSocket.close();
            masterSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
