package rs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class SocketThread extends Thread {

    private PrintWriter os = null;
    private BufferedReader is = null;
    private volatile int slaveID;

    private boolean running = true;

    public SocketThread(BufferedReader is, PrintWriter os, int slaveID) {
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
        System.out.println("[SocketThread] Starting communication");
        while(running) {
            try {
                String line = is.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                System.out.println("[SocketThread] Received: " + line);
    
                if (line.equals("MAP_DONE")) {
                    Master.updateMapStatus(slaveID, true);
                } else if (line.equals("SHUFFLE1_DONE")) {
                    Master.updateShuffle1Status(slaveID, true);
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
        os.println(message);
    }
    
}
