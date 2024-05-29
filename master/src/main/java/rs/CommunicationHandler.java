package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

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
    private ObjectOutputStream osSocket = null;
    private ObjectInputStream isSocket = null;

    private int slaveID;
    private int slavesCount;

    public CommunicationHandler(String hostname, int slaveID, int slavesCount) {
        this.slaveID = slaveID;
        this.slavesCount = slavesCount;
        connectSocket(hostname);
        connectFTP(hostname);
        System.out.println("[CommunicationHandler] Connected to " + hostname);
    }

    private void connectSocket(String hostname) {
       try {
            socket = new Socket(hostname, SOCKET_PORT);
            System.out.println("[Socket] Connected to " + hostname + " on port " + SOCKET_PORT);
            osSocket = new ObjectOutputStream(socket.getOutputStream());
            isSocket = new ObjectInputStream(socket.getInputStream());

            // Sending the INIT message to the slave
            System.out.println("[Socket] Sending INIT message to the slave");
            osSocket.writeUTF("INIT");
            osSocket.writeInt(slavesCount);
            osSocket.writeInt(slaveID);
            ArrayList<String> hostnames = Master.getSlavesHostnames();

            for (int i=0; i<slavesCount; i++) {
                osSocket.writeUTF(hostnames.get(i));
            }

            socketThread = new SocketThread(isSocket, osSocket, slaveID);
            socketThread.start();

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
            socketThread.terminate();
            osSocket.close();
            isSocket.close();
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
        System.out.println("[Socket] Sending to slaveID " + slaveID + ": " + message.toString());
        try {
            osSocket.writeUTF(message.toString());
            osSocket.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendObject(Object object) {
        try {
            osSocket.writeObject(object);
            osSocket.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
