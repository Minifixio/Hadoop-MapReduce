package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;

public class SocketThread extends Thread {

    private BufferedReader is;
    private BufferedWriter os;

    private volatile boolean running = true;

    public SocketThread(BufferedReader is, BufferedWriter os) {
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
        while(running) {
            try {
                String line = is.readLine();
                if (line == null) {
                    break;
                }
                System.out.println("[SocketThread] Received: " + line);
                
                if (line.equals("INIT")) {
                    int slaveID = Integer.parseInt(is.readLine());
                    int slaveCount = Integer.parseInt(is.readLine());
                    Slave.updateSlaveCount(slaveID, slaveCount);
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
                    os.write("OK");
                    os.newLine();
                    os.flush();
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
}
