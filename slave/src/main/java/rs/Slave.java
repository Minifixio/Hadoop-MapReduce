package rs;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Slave {

    private static CommunicationHandler communicationManager;
    private static MapReduceState state = MapReduceState.STARTING;
    private static int slaveID;
    private static int slaveCount;
    private static ArrayList<String> slavesHostnames;
    private static HashMap<String, Integer> reduce1Result = new HashMap<String, Integer>();
    private static HashMap<String, Integer> reduce2Result = new HashMap<String, Integer>();

    public static void main(String[] args) {
        System.out.println("\n\n[Slave] Starting");
        communicationManager = new CommunicationHandler();
        init(); 
    }

    public static void init() {
        state = MapReduceState.STARTING;
        reduce1Result.clear();
        reduce2Result.clear();    
    }

    public static void reset() {   
        state = MapReduceState.STARTING;
        communicationManager.reset();   
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
        if (state != MapReduceState.STARTING) {
            System.err.println("[Slave] Map: wrong state");
            return;
        } else {
            state = MapReduceState.MAP;
        }

        System.out.println("[Slave] Map: starting");
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
                        if (!word.isEmpty() && word.length() > 1) {
                            bw.write(word.toLowerCase() + "\n");
                        }
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
        int hash = Math.abs(word.hashCode());
        int reducer = hash % numberOfSlaves;
        return reducer;
    }

    public static void shuffle1() {
        if (state != MapReduceState.MAP) {
            System.err.println("[Slave] Shuffle1: wrong state");
            return;
        } else {
            state = MapReduceState.SHUFFLE1;
        }

        System.out.println("[Slave] Shuffle1: starting");

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
                if (reducerID >= 0) {
                    String shuffleFilePath = communicationManager.getFTPDirectory() + "/shuffle1/shuffle1_" + reducerID + ".txt";
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(shuffleFilePath, true))) {
                        bw.write(word + "\n");
                    }
                } else {
                    System.out.println("[Slave] word : " + word + " reducerID : " + reducerID);
                }
            }
            System.out.println("[Slave] Shuffle1: created shuffle1 files");

            for (int i = 0; i < slaveCount; i++) {
                
                String shuffleFilePath = communicationManager.getFTPDirectory() + "/shuffle1/shuffle1_" + i + ".txt";
                String sentFileName = "shuffle1_result_slave" + i + "_sender" + slaveID + ".txt";

                // if i == slaveID, move the file to the slave's own FTP directory
                if (i == slaveID) {
                    File file = new File(shuffleFilePath);
                    if (file.exists()) {
                        String sentFilePath = communicationManager.getFTPDirectory() + "/" + sentFileName;
                        file.renameTo(new File(sentFilePath));
                    }

                // else send the file to the right slave via FTP
                } else {
                    communicationManager.sendFTPFile(slavesHostnames.get(i), sentFileName, shuffleFilePath);
                }
            }
            System.out.println("[Slave] Shuffle1: done");
            communicationManager.sendProtocolMessage(ProtocolMessage.SHUFFLE1_DONE);

        } catch (IOException e) {
            System.err.println("[Slave] Shuffle1: Error while reading the file");
            e.printStackTrace();
        }

    }

    /**
     * Reduce1 part
     */
    public static void reduce1() {
        if (state != MapReduceState.SHUFFLE1) {
            System.err.println("[Slave] Reduce1: wrong state");
            return;
        } else {
            state = MapReduceState.REDUCE1;
        }

        System.out.println("[Slave] Reduce1: starting");

        // read all the files with names containing shuffle1_result in the ftp folder
        // for each word in each file, increment the count of the word in the reduce1Result map
        // send back the counts to the master
        File[] files = new File(communicationManager.getFTPDirectory()).listFiles();
        for (File file : files) {
            if (file.getName().contains("shuffle1_result")) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String word = line.trim();
                        if (word.isEmpty()) {
                            continue;
                        }
                        reduce1Result.put(word, reduce1Result.getOrDefault(word, 0) + 1);
                    }
                } catch (IOException e) {
                    System.err.println("[Slave] Reduce1: Error while reading the file");
                    e.printStackTrace();
                }
            }
        }
        communicationManager.sendProtocolMessage(ProtocolMessage.REDUCE1_DONE);
        
        // send the min and max values of the counts
        communicationManager.sendInt(reduce1Result.values().stream().min(Integer::compare).get());
        communicationManager.sendInt(reduce1Result.values().stream().max(Integer::compare).get());
    }

    /**
     * Shuffle2 part
     * @param groups
     */
    public static void shuffle2(ArrayList<Integer> groups) {
        if (state != MapReduceState.REDUCE1) {
            System.err.println("[Slave] Shuffle2: wrong state");
            return;
        } else {
            state = MapReduceState.SHUFFLE2;
        }

        System.out.println("[Slave] Shuffle2: starting");

        // iterate over the words and the counts from reduce1Result
        // create groups.size() files in the shuffle2 folder
        // for each word, add the word to the right file according to the groups list
        // then, send the files to the right slaves
        File shuffle2Folder = new File(communicationManager.getFTPDirectory(), "shuffle2");
        if (!shuffle2Folder.exists()) {
            System.out.println("[Split&Send] Creating folder : " + shuffle2Folder.getAbsolutePath());
            shuffle2Folder.mkdir();
        } else {
            // make sure to delete all previous shuffle2 files to avoid to append above previous tests
            File[] allContents = shuffle2Folder.listFiles();
            if (allContents != null) {
                for (File file : allContents) {
                    file.delete();
                }
            }
        }

        for (Map.Entry<String, Integer> entry : reduce1Result.entrySet()) {
            int group = 0;
            for (int i = 0; i < groups.size(); i++) {
                if (entry.getValue() <= groups.get(i)) {
                    group = i;
                    break;
                }
            }

            String shuffleFilePath = communicationManager.getFTPDirectory() + "/shuffle2/shuffle2_" + group + "_slave_" + slaveID + ".txt";
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(shuffleFilePath, true))) {
                bw.write(entry.getKey() + " " + entry.getValue() + "\n");
            } catch (IOException e) {
                System.err.println("[Slave] Shuffle2: Error while writing the file");
                e.printStackTrace();
            }
        }

        for (int i = 0; i < groups.size(); i++) {
            String shuffleFilePath = communicationManager.getFTPDirectory() + "/shuffle2/shuffle2_" + i + "_slave_" + slaveID + ".txt";
            File f = new File(shuffleFilePath);
            if (f.exists()) {
                String sentFileName = "shuffle2_result_slave" + i + "_sender" + slaveID + ".txt";

                // if i == slaveID, move the file to the slave's own FTP directory
                if (i == slaveID) {
                    File file = new File(shuffleFilePath);
                    if (file.exists()) {
                        String sentFilePath = communicationManager.getFTPDirectory() + "/" + sentFileName;
                        file.renameTo(new File(sentFilePath));
                    }

                // else send the file to the right slave via FTP
                } else {
                    communicationManager.sendFTPFile(slavesHostnames.get(i), sentFileName, shuffleFilePath);
                }
            }
        }

        communicationManager.sendProtocolMessage(ProtocolMessage.SHUFFLE2_DONE);
    }

    public static void reduce2() {
        if (state != MapReduceState.SHUFFLE2) {
            System.err.println("[Slave] Reduce2: wrong state");
            return;
        } else {
            state = MapReduceState.REDUCE2;
        }
        
        // read all the files with names containing shuffle2_result in the ftp folder
        // for each word, happen the count of the word in the reduce2Result map
        // sort the map by values
        // write the map in a file and send it back to the master

        System.out.println("[Slave] Reduce2: starting");
        File[] files = new File(communicationManager.getFTPDirectory()).listFiles();
        for (File file : files) {
            if (file.getName().contains("shuffle2_result")) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String word = line.split(" ")[0];
                        Integer count = Integer.parseInt(line.split(" ")[1]);
        
                        reduce2Result.put(word, reduce2Result.getOrDefault(word, 0) + count);
                    }
                } catch (IOException e) {
                    System.err.println("[Slave] Reduce2: Error while reading the file");
                    e.printStackTrace();
                }
            }
        }

        // convert reduce2Result to stream sorted in ascending order
        Stream<Map.Entry<String,Integer>> sortedReduce2Result = reduce2Result.entrySet().stream().sorted(Map.Entry.comparingByValue());

        // write reduce2Result map in a file
        String reduce2FilePath = communicationManager.getFTPDirectory() + "/reduce2_result_" + slaveID + ".txt";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(reduce2FilePath))) {
            sortedReduce2Result.forEach(entry -> {
                try {
                    bw.write(entry.getKey() + " " + entry.getValue() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            System.err.println("[Slave] Reduce2: Error while writing the file");
            e.printStackTrace();
        }

        System.out.println("[Slave] Reduce2: done");
        communicationManager.sendProtocolMessage(ProtocolMessage.REDUCE2_DONE);
    }

}