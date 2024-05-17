package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class CommunicationHandler {


    private static final int FTP_PORT = 3456;
    private final String FTP_USERNAME = "elegallic-22";
    private final String FTP_PASSWORD = "slr207wew";
   
    private FTPClient FTPClient = null;

    private static final int SOCKET_PORT = 9999;
    private Socket socket = null;
    private SocketThread socketThread = null;
    
    private BufferedWriter os = null;
    private BufferedReader is = null;

    private int slaveID;

    public CommunicationHandler(String hostname, int slaveID) {
        this.slaveID = slaveID;
        connectFTP(hostname);
        connectSocket(hostname);
        System.out.println("[CommunicationHandler] Connected to " + hostname);
    }

    private void connectSocket(String hostname) {
       try {
            socket = new Socket(hostname, SOCKET_PORT);
            System.out.println("[Socket] Connected to " + hostname + " on port " + SOCKET_PORT);
            os = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socketThread = new SocketThread(is, os, slaveID).start();
       } catch (UnknownHostException e) {
            System.err.println("[Socket] Don't know about host " + hostname);
            return;
       } catch (IOException e) {
            System.err.println("[Socket] Couldn't get I/O for the connection to " + hostname);
            return;
       }
    }

    public void disconnectSocket() {
        try {
            socketThread.close();
            os.close();
            is.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isSocketConnected() {
        return (socket != null) && socket.isConnected();
    }

    private void connectFTP(String hostname) {

        FTPClient = new FTPClient();

        try {
            FTPClient.connect(hostname, FTP_PORT);
            FTPClient.login(FTP_USERNAME, FTP_PASSWORD);
            FTPClient.enterLocalPassiveMode();
            FTPClient.setFileType(FTP.BINARY_FILE_TYPE);
            System.out.println("[FTP] Connected to " + hostname + " on port " + FTP_PORT);
        } catch (Exception e) {
            System.err.println("[FTP] Error connecting to " + hostname + " on port " + FTP_PORT);
            e.printStackTrace();
        }
    }

    public boolean isFTPConnected() {
        return (FTPClient != null) && FTPClient.isConnected();
    }

    public void disconnectFTP() {
        try {
            FTPClient.logout();
            FTPClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFileFTP(String filePath, String sentFileName) {
        System.out.println("[FTP] Uploading file: " + filePath);
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            FTPClient.storeFile(sentFileName, inputStream);
            System.out.println("[FTP] File uploaded successfully.");
        } catch (IOException e) {
            System.err.println("[FTP] Error uploading file: " + e.getMessage());
        }
    }

    public void writeFTP(String fileName, byte[] bytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            FTPClient.storeFile(fileName, inputStream);
            int errorCode = FTPClient.getReplyCode();
            if (errorCode != 226) {
                System.out.println("[FTP] File upload failed. FTP Error code: " + errorCode);
            } else {
                System.out.println("[FTP] File uploaded successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void listFilesFTP() {
        System.out.println("[FTP] Listing remote files");
        try {
            FTPFile[] files = FTPClient.listFiles();
            for (FTPFile file : files) {
                System.out.println(file.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendProtocolMessage(ProtocolMessage message) {
        try {
            os.write(message.toString());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
