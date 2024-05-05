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

public class SlaveSocket {
    
    private final int PORT = 9999; // We can't choose a port less than 1023 if we are not privileged users (root)

    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private BufferedWriter os = null;
    private BufferedReader is = null;

    public SlaveSocket(String hostname) {
        init(hostname);
    }

    private void init(String hostname) {
       try {
            serverSocket = new ServerSocket(PORT);
       } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
       }

       try {
            System.out.println("Server is waiting to accept user...");

            // Accept client connection request
            // Get new Socket at Server.    
            socket = serverSocket.accept();
            System.out.println("Accept a client!");

            // Open input and output streams
            is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            os = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

       } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
       }
       System.out.println("Sever stopped!");
    }

    private void read() {
        try {
            String line;
            while (true) {
                // Read data to the server (sent from client).
                line = is.readLine();
                
                // Send to client
                os.write(">> " + line);
                // End of line
                os.newLine();
                // Flush data
                os.flush(); 
    
                if (line.equals("QUIT")) {
                    os.write(">> OK");
                    os.newLine();
                    os.flush();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
