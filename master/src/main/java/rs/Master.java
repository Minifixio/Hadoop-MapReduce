package rs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * TODO : 
 * - Try different file sizes
 */

public class Master {

    private static final String SPLIT_FOLDER_NAME = "splits";

    private static int slavesCount;
    private static ArrayList<String> slavesHostnames = new ArrayList<String>();

    private static ArrayList<CommunicationHandler> communicationHandlers;

    private static ArrayList<Boolean> slavesMapStatus = new ArrayList<Boolean>();

    private static ArrayList<Boolean> slavesShuffle1Status = new ArrayList<Boolean>();
    private static ArrayList<Boolean> slavesReduce1Status = new ArrayList<Boolean>();

    private static ArrayList<Boolean> slavesShuffle2Status = new ArrayList<Boolean>();
    private static ArrayList<Boolean> slavesReduce2Status = new ArrayList<Boolean>();

    private static HashMap<String, Integer> output = new HashMap<String, Integer>();

    public static void main(String[] args) {

        if(args.length != 2) {
			System.err.println("syntax: java -jar Master.jar <slaves hostnames file path> <source file path>");
			System.exit(1);
		}

        String slavesHostnamesFileName = args[0];
        retreiveSlavesHostnames(slavesHostnamesFileName);
        initCommunications(slavesCount);

        String sourceFilePath = args[1];
        splitAndSendChunks(sourceFilePath, slavesCount);
    }

    public static ArrayList<String> getSlavesHostnames() {
        return slavesHostnames;
    }

    private static void initCommunications(int numberOfSlaves) {
        int slaveID = 0;
        communicationHandlers = new ArrayList<CommunicationHandler>();
        for (String slaveHostname : slavesHostnames) {
            communicationHandlers.add(new CommunicationHandler(slaveHostname, slaveID, numberOfSlaves));
            slavesMapStatus.add(false);
            slavesShuffle1Status.add(false);
            slavesReduce1Status.add(false);
            slavesShuffle2Status.add(false);
            slavesReduce2Status.add(false);
            slaveID++;
        }
    }

    /**
     * Read the hostnames of the slaves from a file
     * and store them in the slavesHostnames array
     * and also initialize slavesCount
     * @param slavesHostnamesFileName
     */
    public static void retreiveSlavesHostnames(String slavesHostnamesFileName) {
        String userDir = System.getProperty("user.dir");
        File file = new File(userDir, slavesHostnamesFileName);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                slavesCount += 1;
                slavesHostnames.add(line.trim());
                System.out.println("[Retreive Hostnames] Added slave!  (hostname : " + line.trim() + ")");
            }
        } catch (IOException e) {
            System.err.println("[Retreive Hostnames] Erreur lors de la lecture du fichier : " + file.getAbsolutePath() + " (" + e.getMessage() + ")");
        }
    }

    /**
     * Split the file into multiple parts
     * and send each chunk to a slave
     * @param filePath
     * @param splitsFolderPath
     * @param numberOfSlaves
     */
    public static void splitAndSendChunks(String sourceFilePath, int numberOfSlaves) {
        System.out.println("[Split&Send] Splitting file into " + numberOfSlaves + " parts...");
        
        // Create a folder named SPLIT_FOLDER_NAME at workind_dir/SPLIT_FOLDER_NAME if it doesn't exist
        String userDir = System.getProperty("user.dir");
        File splitsFolder = new File(userDir, SPLIT_FOLDER_NAME);
        if (!splitsFolder.exists()) {
            System.out.println("[Split&Send] Creating folder : " + splitsFolder.getAbsolutePath());
            splitsFolder.mkdir();
        }

        // Read the source file using BufferedInputStream
        // Split the file in chunks of size = file_size / numberOfSlaves (make sure to stop at a space character not to cut a word)
        // Write the chunks to a file named SPLIT_FOLDER_NAME/split_i.txt
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFilePath))) {
            long fileSize = new File(sourceFilePath).length();
            long chunkSize = fileSize / numberOfSlaves; // taille de chaque morceau
            
            int defaultMaxBufferSize = 8192;

            for (int i = 0; i < numberOfSlaves; i++) {
                System.out.println("\n\n[Split&Send] Splitting file " + i + "...");
                String splitFilePath = splitsFolder.getAbsolutePath() + "/split_" + i + ".txt";
                int bytesRead = 0;
                int currentBytesRead = 0;

                while(currentBytesRead >= 0 && bytesRead < chunkSize) {
                    
                    byte[] buffer;
                    boolean lastRead = false;
                    if (bytesRead + defaultMaxBufferSize > chunkSize) {
                        buffer = new byte[(int) (chunkSize - bytesRead)];
                        lastRead = true;
                    } else {
                        buffer = new byte[defaultMaxBufferSize];
                    }
                    

                    currentBytesRead = bis.read(buffer);
                    bytesRead += currentBytesRead;

                    if (currentBytesRead < buffer.length) {
                        buffer = Arrays.copyOf(buffer, currentBytesRead);
                        currentBytesRead = -1;
                    } 
                    
                    if (currentBytesRead >= 0 && lastRead) {
                        if ((char) buffer[buffer.length - 1] != ' ') {
                            byte[] restOfWordBuffer = new byte[1];
                            int bytesRestOfWord = bis.read(restOfWordBuffer);

                            while (bytesRestOfWord > 0 && (char) restOfWordBuffer[0] != ' ') {
                                restOfWordBuffer = new byte[1];
                                bytesRestOfWord = bis.read(restOfWordBuffer);
                                if (bytesRestOfWord > 0) {
                                    bytesRead += bytesRestOfWord;
                                    buffer = Arrays.copyOf(buffer, buffer.length + 1);
                                    buffer[buffer.length - 1] = restOfWordBuffer[0];
                                }
                            }
                        }
                    }

                    for (byte b : buffer) {
                        System.out.print((char) b);
                    }
                    System.out.println();  

                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(splitFilePath))) {
                        bos.write(buffer);
                    }
                }

                // Send the split file to the slave
                // TODO: make sure Threads are working well
                final int index = i;
                new Thread(() -> {
                    communicationHandlers.get(index).sendFileFTP(splitFilePath, "split_" + index + ".txt");
                    communicationHandlers.get(index).sendProtocolMessage(ProtocolMessage.START_MAP);
                }).start();
            }
        } catch (IOException e) {
            System.err.println("[Split&Send] Erreur lors de la lecture ou de l'écriture de fichier : " + e.getMessage());
        }
        
    }

    /**
     * Send the order to start the map phase to all slaves
     */
    public static void sendMapOrder() {
        for (int i = 0; i < slavesCount; i++) {
            final int index = i;
            new Thread(() -> {
                communicationHandlers.get(index).sendProtocolMessage(ProtocolMessage.START_MAP);
            }).start();
        }
    }

    /**
     * Send the order to start the shuffle1 phase to all slaves
     */
    public static void sendShuffle1Order() {
        for (int i = 0; i < slavesCount; i++) {
            final int index = i;
            new Thread(() -> {
                communicationHandlers.get(index).sendProtocolMessage(ProtocolMessage.START_SHUFFLE1);
            }).start();
        }
    }

    /**
     * Send the order to start the reduce1 phase to all slaves
     */
    public static void sendReduce1Order() {
        for (int i = 0; i < slavesCount; i++) {
            final int index = i;
            new Thread(() -> {
                communicationHandlers.get(index).sendProtocolMessage(ProtocolMessage.START_REDUCE1);
            }).start();
        }
    }

    /**
    * Update the status of the map for a slave
    * @param slaveID
    * @param status
    */
    public static void updateMapStatus(int slaveID, boolean status) {
        slavesMapStatus.set(slaveID, status);
        if (slavesMapStatus.stream().allMatch(s -> s)) {
            sendShuffle1Order();
        }
    }

    /**
     * Update the status of the shuffle for a slave
     * @param slaveID
     * @param status
     */
    public static void updateShuffle1Status(int slaveID, boolean status) {
        slavesShuffle1Status.set(slaveID, status);
    }

    /**
     * Update the status of the reduce for a slave
     * @param slaveID
     * @param status
     */
    public static void updateReduce1Status(int slaveID, boolean status) {
        slavesReduce1Status.set(slaveID, status);
    }

    /**
     * Update the status of the shuffle for a slave
     * @param slaveID
     * @param status
     */
    public static void updateShuffle2Status(int slaveID, boolean status) {
        slavesShuffle2Status.set(slaveID, status);
    }

    /**
     * Update the status of the reduce for a slave
     * @param slaveID
     * @param status
     */

    public static void updateReduce2Status(int slaveID, boolean status) {
        slavesReduce2Status.set(slaveID, status);
    }
}
