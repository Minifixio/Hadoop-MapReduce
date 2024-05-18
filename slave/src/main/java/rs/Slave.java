package rs;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Slave {

    private static CommunicationHandler communicationManager;
    private static int slaveID;
    private static int slaveCount;
    private static ArrayList<String> slavesHostnames;
    private static ArrayList<String> mapResult = new ArrayList<String>();
    private static HashMap<String, Integer> reduceResult = new HashMap<String, Integer>();

    public static void main(String[] args) {
        communicationManager = new CommunicationHandler();
    }

    public static void setSlaveID(int id) {
        slaveID = id;
    }

    public static void setSlaveCount(int numberOfSlaves) {
        slaveCount = numberOfSlaves;
    }

    public static void setSlavesHostnames(ArrayList<String> hostnames) {
        slavesHostnames = hostnames;
    }

    /**
     * Read the map file located in the FTP repository.
     * Fill the mapResult ArrayList with the words of the file (split at each space).
     */
    public static void map() {
        String chunkFilePath = communicationManager.getFTPDirectory() + "/split_" + slaveID + ".txt";
        String mapFilePath = communicationManager.getFTPDirectory() + "/map_" + slaveID + ".txt";

        // Read the file located at chunkFilePath 
        // and write in a new txt file, in the same FTP directory, each word of the file on a new line
        // (split at each space)
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mapFilePath))) {
            try (BufferedReader br = new BufferedReader(new FileReader(chunkFilePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // removing punctuation
                    String[] words = line.replaceAll("[\\p{P}&&[^\u0027]]", "").split(" ");
                    for (String word : words) {
                        bw.write(word + "\n");
                    }
                }
                System.out.println("[Slave] Map: done");
                communicationManager.sendProtocolMessage(ProtocolMessage.MAP_DONE);
            }
        } catch (IOException e) {
            System.err.println("[Slave] Map: Error while reading the file");
            e.printStackTrace();
        }
    }

    public static int chooseReducer(String word, int numberOfSlaves) {
        int hash = word.hashCode();
        int reducer = hash % numberOfSlaves;
        return reducer;
    }

    public static void shuffle1() {
        // read all the words in the map_0.txt file
        // for each word, get the reducer ID with chooseReducer
        // if a shuffle1/shuffle1_{reducerID}.txt file exists, append the word to it
        // else create shuffle1/shuffle1_{reducerID}.txt file and append the word to it
        // at the end, send the file via FTP to the hostname of id reducerID

        String mapFilePath = communicationManager.getFTPDirectory() + "/map_" + slaveID + ".txt";
        // create shuffle1 folder
        File shuffle1Folder = new File(communicationManager.getFTPDirectory(), "shuffle1");
        if (!shuffle1Folder.exists()) {
            System.out.println("[Split&Send] Creating folder : " + shuffle1Folder.getAbsolutePath());
            shuffle1Folder.mkdir();
        } else {
            // make sure to delete all previous shuffle1 files to avoid to append above previous tests
            File[] allContents = shuffle1Folder.listFiles();
            if (allContents != null) {
                for (File file : allContents) {
                    file.delete();
                }
            }
        }

        try(BufferedReader br = new BufferedReader(new FileReader(mapFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String word = line.trim();
                int reducerID = chooseReducer(word, slaveCount);
                String shuffleFilePath = communicationManager.getFTPDirectory() + "/shuffle1/shuffle1_" + reducerID + ".txt";
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(shuffleFilePath, true))) {
                    bw.write(word + "\n");
                }
            }
            System.out.println("[Slave] Shuffle1: created shuffle1 files");

            for (int i = 0; i < slaveCount; i++) {
                String shuffleFilePath = communicationManager.getFTPDirectory() + "/shuffle1/shuffle1_" + i + ".txt";
                String sentFileName = "shuffle1_result_" + i + ".txt";
                communicationManager.sendFTPFile(slavesHostnames.get(i), sentFileName, shuffleFilePath);
            }
            communicationManager.sendProtocolMessage(ProtocolMessage.SHUFFLE1_DONE);
        } catch (IOException e) {
            System.err.println("[Slave] Shuffle1: Error while reading the file");
            e.printStackTrace();
        }

    }

    public static void reduce1() {

    }

    public static void shuffle2() {

    }

    public static void reduce2() {

    }

}