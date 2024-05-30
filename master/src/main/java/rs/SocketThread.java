package rs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

public class SocketThread extends Thread {

    private ObjectInputStream is;
    private ObjectOutputStream os;
    private volatile int slaveID;

    private boolean running = true;

    public SocketThread(ObjectInputStream is, ObjectOutputStream os, int slaveID) {
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
                String line = is.readUTF();
                if (line == null) {
                    break;
                }
                line = line.trim();
                System.out.println("[SocketThread] Received: " + line);
    
                if (line.equals("MAP_DONE")) {
                    Master.updateMapStatus(slaveID, true);
                } else if (line.equals("SHUFFLE1_DONE")) {
                    Master.updateShuffle1Status(slaveID, true);
                } else if (line.equals("REDUCE1_DONE")) {
                    Integer reduce1Min = is.readInt();
                    Integer reduce1Max = is.readInt();
                    Master.updateReduce1Status(slaveID, true, reduce1Min, reduce1Max);
                } else if (line.equals("SHUFFLE2_DONE")) {
                    Master.updateShuffle2Status(slaveID, true);
                } else if (line.equals("REDUCE2_DONE")) {
                    Master.updateReduce2Status(slaveID, true);
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    public void write(String message) {
        try {
            os.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
