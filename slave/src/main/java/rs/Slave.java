package rs;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Slave {

    private static CommunicationHandler communicationManager;
    private static int slaveID;
    private static int slaveCount;
    private static ArrayList<String> mapResult = new ArrayList<String>();
    private static HashMap<String, Integer> reduceResult = new HashMap<String, Integer>();

    public static void main(String[] args) {
        communicationManager = new CommunicationHandler();
    }

    public static void updateSlaveCount(int id, int count) {
        System.out.println("[Slave] Received slave count: " + count + " and id: " + id);
        slaveID = id;
        slaveCount = count;
    }

    /**
     * Read the map file located in the FTP repository.
     * Fill the mapResult ArrayList with the words of the file (split at each space).
     */
    public static void map() {
        String chunkFilePath = communicationManager.getFTPDirectory() + "/chunk" + slaveID + ".txt";
        String mapFilePath = communicationManager.getFTPDirectory() + "/map" + slaveID + ".txt";

        // Read the file located at chunkFilePath 
        // and write in a new txt file, in the same FTP directory, each word of the file on a new line
        // (split at each space)
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mapFilePath))) {
            try (BufferedReader br = new BufferedReader(new FileReader(chunkFilePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] words = line.split(" ");
                    for (String word : words) {
                        bw.write(word + "\n");
                    }
                }
                System.out.println("[Slave] Map: done");
            }
        } catch (IOException e) {
            System.err.println("[Slave] Map: Error while reading the file");
            e.printStackTrace();
        }
    }

    public static void chooseReducer() {

    }

    public static void shuffle1() {

    }

    public static void reduce1() {

    }

    public static void shuffle2() {

    }

    public static void reduce2() {

    }

}