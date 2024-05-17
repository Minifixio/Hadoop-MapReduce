package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketThread extends Thread {

    private BufferedWriter os = null;
    private BufferedReader is = null;
    private volatile int slaveID;

    private boolean running = true;

    public SocketThread(BufferedReader is, BufferedWriter os, int slaveID) {
        this.is = is;
        this.os = os;
        this.slaveID = slaveID;
    }

    @Override
    public void run() {
        running = true;
        startCommunication();
    }

    public void terminate() {
        running = false;
    }

    private void startCommunication() {
        while(running) {
            try {
                String line = is.readLine();
                if (line == null) {
                    break;
                }
                System.out.println("[SocketThread] Received: " + line);
    
                if (line.equals("MAP_DONE")) {
                    Master.updateMapStatus(slaveID, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
