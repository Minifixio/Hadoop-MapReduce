package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class SocketThread extends Thread {

    private BufferedReader is;
    private PrintWriter os;

    private volatile boolean running = true;

    public SocketThread(BufferedReader is, PrintWriter os) {
        this.is = is;
        this.os = os;
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
                
                if (line.equals("INIT")) {
                    System.out.println("[SocketThread] Starting initialization");
                    
                    int slaveCount = Integer.parseInt(is.readLine());
                    Slave.setSlaveCount(slaveCount);

                    int slaveID = Integer.parseInt(is.readLine());
                    Slave.setSlaveID(slaveID);

                    System.out.println("[SocketThread] Received slave count: " + slaveCount + " and id: " + slaveID);

                    ArrayList<String> slavesHostnames = new ArrayList<String>();
                    for (int i=0; i<slaveCount; i++) {
                        slavesHostnames.add(is.readLine().trim());
                    }

                    Slave.setSlavesHostnames(slavesHostnames);
                    os.println("INIT_OK");
                } else if (line.equals("START_MAP")) {
                    Slave.map();
                } else if (line.equals("START_SHUFFLE1")) {
                    Slave.shuffle1();
                } else if (line.equals("START_REDUCE1")) {
                    Slave.reduce1();
                } else if (line.equals("START_SHUFFLE2")) {
                    Slave.shuffle2();
                } else if (line.equals("START_REDUCE2")) {
                    Slave.reduce2();
                } else if (line.equals("QUIT")) {
                    os.println("OK");
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
}
