package rs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;

public class SocketThread extends Thread {

    private ObjectInputStream is;
    private ObjectOutputStream os;

    private volatile boolean running = true;

    public SocketThread(ObjectInputStream is, ObjectOutputStream os) {
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
                String line = is.readUTF();
                if (line == null) {
                    break;
                }
                line = line.trim();
                System.out.println("[SocketThread] Received: " + line);
                
                if (line.equals("INIT")) {
                    System.out.println("[SocketThread] Starting initialization");

                    int slaveCount = is.readInt();
                    Slave.setSlaveCount(slaveCount);

                    int slaveID = is.readInt();
                    Slave.setSlaveID(slaveID);

                    System.out.println("[SocketThread] Received slave count: " + slaveCount + " and id: " + slaveID);

                    ArrayList<String> slavesHostnames = new ArrayList<String>();
                    for (int i=0; i<slaveCount; i++) {
                        slavesHostnames.add(is.readUTF());
                    }

                    Slave.setSlavesHostnames(slavesHostnames);
                    os.writeUTF(ProtocolMessage.INIT_RECEIVED.toString());
                    os.flush();

                } else if (line.equals(ProtocolMessage.START_MAP.toString())) {
                    os.writeUTF(ProtocolMessage.MAP_RECEIVED.toString());
                    os.flush();
                    Slave.map();

                } else if (line.equals(ProtocolMessage.START_SHUFFLE1.toString())) {
                    os.writeUTF(ProtocolMessage.SHUFFLE1_RECEIVED.toString());
                    os.flush();
                    Slave.shuffle1();

                } else if (line.equals(ProtocolMessage.START_REDUCE1.toString())) {
                    os.writeUTF(ProtocolMessage.REDUCE1_RECEIVED.toString());
                    os.flush();
                    Slave.reduce1();

                } else if (line.equals(ProtocolMessage.START_SHUFFLE2.toString())) {
                    ArrayList<Integer> shuffle2Groups = (ArrayList<Integer>) is.readObject();
                    os.writeUTF(ProtocolMessage.SHUFFLE2_RECEIVED.toString());
                    os.flush();
                    Slave.shuffle2(shuffle2Groups);

                } else if (line.equals(ProtocolMessage.START_REDUCE2.toString())) {
                    os.writeUTF(ProtocolMessage.REDUCE2_RECEIVED.toString());
                    os.flush();
                    Slave.reduce2();

                } else if (line.equals(ProtocolMessage.QUIT.toString())) {
                    os.writeUTF("OK");
                    os.flush();
                    Slave.reset();
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
