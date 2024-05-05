package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MasterSocket {

    private static final int PORT = 9999;

    private static BufferedWriter os = null;
    private static BufferedReader is = null;
    private static Socket socket = null;

    public MasterSocket(String hostname) {
        init(hostname);
    }

    public static void init(String hostname) {

       try {
            socket = new Socket(hostname, PORT);
            os = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
       } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostname);
            return;
       } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostname);
            return;
       }
    }

    public void close() {
        try {
            os.close();
            is.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void read() {
        String responseLine;
        try {
            while ((responseLine = is.readLine()) != null) {
                System.out.println("Server: " + responseLine);
                if (responseLine.indexOf("OK") != -1) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    public void write(String message) {
        try {
            os.write(message);
            os.newLine(); // End of line
            os.flush();  
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
