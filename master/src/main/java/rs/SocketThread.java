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
    
                if (line.equals(ProtocolMessage.MAP_RECEIVED.toString())) {
                    Master.updateMapMessageAcquittal(slaveID, true);

                } else if (line.equals(ProtocolMessage.MAP_DONE.toString())) {
                    Master.updateMapStatus(slaveID, true);
                
                } else if (line.equals(ProtocolMessage.SHUFFLE1_RECEIVED.toString())) {
                    Master.updateShuffle1MessageAcquittal(slaveID, true);

                } else if (line.equals(ProtocolMessage.SHUFFLE1_DONE.toString())) {
                    Master.updateShuffle1Status(slaveID, true);

                } else if (line.equals(ProtocolMessage.REDUCE1_RECEIVED.toString())) {
                    Master.updateReduce1MessageAcquittal(slaveID, true);

                } else if (line.equals(ProtocolMessage.REDUCE1_DONE.toString())) {
                    Integer reduce1Min = is.readInt();
                    Integer reduce1Max = is.readInt();
                    Master.updateReduce1Status(slaveID, true, reduce1Min, reduce1Max);

                } else if (line.equals(ProtocolMessage.SHUFFLE2_RECEIVED.toString())) {
                    Master.updateShuffle2MessageAcquittal(slaveID, true);

                } else if (line.equals(ProtocolMessage.SHUFFLE2_DONE.toString())) {
                    Master.updateShuffle2Status(slaveID, true);

                } else if (line.equals(ProtocolMessage.REDUCE2_RECEIVED.toString())) {
                    Master.updateReduce2MessageAcquittal(slaveID, true);

                } else if (line.equals(ProtocolMessage.REDUCE2_DONE.toString())) {
                    Master.updateReduce2Status(slaveID, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
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

    public void reset() {
        try {
            os.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
