package rs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

/**
 * TODO : 
 * - Try different file sizes
 */

public class Master {

    private static final String SPLIT_FOLDER_NAME = "splits";
    private static final String RESULT_FILE_NAME = "result.txt";

    private static long executionTime;
    private static ArrayList<Long> mapTimes = new ArrayList<Long>();
    private static ArrayList<Long> shuffle1Times = new ArrayList<Long>();
    private static ArrayList<Long> reduce1Times = new ArrayList<Long>();
    private static ArrayList<Long> shuffle2Times = new ArrayList<Long>();
    private static ArrayList<Long> reduce2Times = new ArrayList<Long>();

    private static ArrayList<Long> mapMessagesTimes = new ArrayList<Long>();
    private static ArrayList<Long> shuffle1MessagesTimes = new ArrayList<Long>();
    private static ArrayList<Long> reduce1MessagesTimes = new ArrayList<Long>();
    private static ArrayList<Long> shuffle2MessagesTimes = new ArrayList<Long>();
    private static ArrayList<Long> reduce2MessagesTimes = new ArrayList<Long>();

    private static long communicationTime;
    private static long synchronizationTime;
    private static long computationTime;

    private static int slavesCount;
    private static ArrayList<String> slavesHostnames = new ArrayList<String>();

    private static MapReduceState state = MapReduceState.STARTING;
    private static volatile boolean isFinished = false;

    private static ArrayList<CommunicationHandler> communicationHandlers;

    private static ArrayList<Boolean> slavesMapStatus = new ArrayList<Boolean>();

    private static ArrayList<Boolean> slavesShuffle1Status = new ArrayList<Boolean>();
    private static ArrayList<Boolean> slavesReduce1Status = new ArrayList<Boolean>();
    private static Integer reduce1Min = null;
    private static Integer reduce1Max = null;

    private static ArrayList<Integer> shuffle2Groups = new ArrayList<Integer>();
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
        // Start of the MapReduce process
        executionTime = System.currentTimeMillis();
        splitAndSendChunks(sourceFilePath, slavesCount);
    }

    public static void initTimes() {
        executionTime = System.currentTimeMillis();
        communicationTime = 0;
        synchronizationTime = 0;
        computationTime = 0;
    }

    public static ArrayList<String> getSlavesHostnames() {
        return slavesHostnames;
    }

    /**
     * Initialize the communication handlers for each slave
     * @param numberOfSlaves
     */
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
            mapTimes.add(0L);
            shuffle1Times.add(0L);
            reduce1Times.add(0L);
            shuffle2Times.add(0L);
            reduce2Times.add(0L);
            mapMessagesTimes.add(0L);
            shuffle1MessagesTimes.add(0L);
            reduce1MessagesTimes.add(0L);
            shuffle2MessagesTimes.add(0L);
            reduce2MessagesTimes.add(0L);
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

        // Remove all the existing files in the split folder
        File[] files = splitsFolder.listFiles();
        for (File f : files) {
            f.delete();
        }

        // Read the source file using BufferedInputStream
        // Split the file in chunks of size = file_size / numberOfSlaves (make sure to stop at a space character not to cut a word)
        // Write the chunks to a file named SPLIT_FOLDER_NAME/split_i.txt
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFilePath))) {
            long fileSize = new File(sourceFilePath).length();
            long chunkSize = fileSize / numberOfSlaves; // taille de chaque morceau
            System.out.println("[Split&Send] File size : " + fileSize + " | Chunk size : " + chunkSize);
            
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
                    
                    // If the last character of the buffer is not a space, read the next byte until a space is found
                    if (currentBytesRead >= 0 && lastRead) {
                        if ((char) buffer[buffer.length - 1] != ' ') {
                            byte[] restOfWordBuffer = new byte[1];
                            int bytesRestOfWord = bis.read(restOfWordBuffer);

                            while (bytesRestOfWord > 0 && (char) restOfWordBuffer[0] != ' ') {
                                if (bytesRestOfWord > 0) {
                                    bytesRead += bytesRestOfWord;
                                    buffer = Arrays.copyOf(buffer, buffer.length + 1);
                                    buffer[buffer.length - 1] = restOfWordBuffer[0];
                                }

                                restOfWordBuffer = new byte[1];
                                bytesRestOfWord = bis.read(restOfWordBuffer);
                            }
                        }
                    }

                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(splitFilePath, true))) {
                        bos.write(buffer);
                    }
                }

                // Send the split file to the slave
                System.out.println("[Master] Sending order to start map phase to all slaves...");
                
                state = MapReduceState.MAP;
                mapMessagesTimes.set(i, System.currentTimeMillis());

                final int index = i;
                communicationHandlers.get(index).sendFileFTP(splitFilePath, "split_" + index + ".txt");
                communicationHandlers.get(index).sendProtocolMessage(ProtocolMessage.START_MAP);
            }
        } catch (IOException e) {
            System.err.println("[Split&Send] Erreur lors de la lecture ou de l'écriture de fichier : " + e.getMessage());
        }
    }

    /**
     * Send the order to start the shuffle1 phase to all slaves
     */
    public static void sendShuffle1Order() {

        if (state != MapReduceState.MAP) {
            System.err.println("[Master] Error: Can't start shuffle1 phase, the current state is " + state);
            return;
        } else {
            System.out.println("[Master] Sending order to start shuffle1 phase to all slaves...");
            state = MapReduceState.SHUFFLE1;
        }

        for (int i = 0; i < slavesCount; i++) {
            final int index = i;
            shuffle1MessagesTimes.set(index, System.currentTimeMillis());
            communicationHandlers.get(index).sendProtocolMessage(ProtocolMessage.START_SHUFFLE1);
        }
    }

    /**
     * Send the order to start the reduce1 phase to all slaves
     */
    public static void sendReduce1Order() {

        if (state != MapReduceState.SHUFFLE1) {
            System.err.println("[Master] Error: Can't start reduce1 phase, the current state is " + state);
            return;
        } else {
            System.out.println("[Master] Sending order to start reduce1 phase to all slaves...");
            state = MapReduceState.REDUCE1;
        }

        for (int i = 0; i < slavesCount; i++) {
            final int index = i;
            reduce1MessagesTimes.set(index, System.currentTimeMillis());
            if (reduce1Times.get(index) == 0L) {
                reduce1Times.set(index, System.currentTimeMillis());
            }
            communicationHandlers.get(index).sendProtocolMessage(ProtocolMessage.START_REDUCE1);
        }
    }

    /**
    * Update the status of the map for a slave
    * @param slaveID
    * @param status
    */
    public static void updateMapStatus(int slaveID, boolean status) {
        System.out.println("[Master] Map: Slave " + slaveID + " has finished the map phase");
        slavesMapStatus.set(slaveID, status);
        mapTimes.set(slaveID, System.currentTimeMillis() - mapTimes.get(slaveID));

        if (slavesMapStatus.stream().allMatch(s -> s) && state == MapReduceState.MAP) {
            sendShuffle1Order();
        }
    }

    /**
     * Update the status of the map for a slave
     * @param slaveID
     * @param status
     */
    public static void updateMapMessageAcquittal(int slaveID, boolean status) {
        mapMessagesTimes.set(slaveID, System.currentTimeMillis() - mapMessagesTimes.get(slaveID));
        mapTimes.set(slaveID, System.currentTimeMillis());
    }

    /**
     * Update the status of the shuffle for a slave
     * @param slaveID
     * @param status
     */
    public static void updateShuffle1Status(int slaveID, boolean status) {
        System.out.println("[Master] Shuffle1: Slave " + slaveID + " has finished the shuffle phase");
        slavesShuffle1Status.set(slaveID, status);
        shuffle1Times.set(slaveID, System.currentTimeMillis() - shuffle1Times.get(slaveID));
        if (slavesShuffle1Status.stream().allMatch(s -> s) && state == MapReduceState.SHUFFLE1) {
            sendReduce1Order();
        }
    }

    /**
     * Update the status of the shuffle for a slave
     * @param slaveID
     * @param status
     */
    public static void updateShuffle1MessageAcquittal(int slaveID, boolean status) {
        shuffle1MessagesTimes.set(slaveID, System.currentTimeMillis() - shuffle1MessagesTimes.get(slaveID));
        if (shuffle1Times.get(slaveID) == 0L) {
            shuffle1Times.set(slaveID, System.currentTimeMillis());
        }
    }

    /**
     * Update the status of the reduce for a slave
     * @param slaveID
     * @param status
     * @param output
     */
    public static void updateReduce1Status(int slaveID, boolean status, Integer min, Integer max) {
        System.out.println("[Master] Reduce1: Slave " + slaveID + " has finished the reduce phase");
        
        slavesReduce1Status.set(slaveID, status);
        reduce1Times.set(slaveID, System.currentTimeMillis() - reduce1Times.get(slaveID));

        if (reduce1Min == null || min < reduce1Min) {
            reduce1Min = min;
        }

        if (reduce1Max == null || max > reduce1Max) {
            reduce1Max = max;
        }

        if (slavesReduce1Status.stream().allMatch(s -> s) && state == MapReduceState.REDUCE1) {
            System.out.println("[Master] Reduce1: All slaves have finished the reduce phase");
            System.out.println("[Master] Reduce1: Min = " + reduce1Min + " | Max = " + reduce1Max);
            sendShuffle2Order();
        }
    }

    /**
     * Update the status of the reduce for a slave
     * @param slaveID
     * @param status
     */
    public static void updateReduce1MessageAcquittal(int slaveID, boolean status) {
        reduce1MessagesTimes.set(slaveID, System.currentTimeMillis() - reduce1MessagesTimes.get(slaveID));
    }

    public static void makeShuffle2Groups() {
        System.out.println("[Master] Reduce1: Making groups for shuffle2 phase...");
        int groupSize = (reduce1Max - reduce1Min) / slavesCount;
        int currentMin = reduce1Min;
        int currentMax = reduce1Min + groupSize;
        for (int i = 0; i < slavesCount; i++) {
            int max = currentMax;
            shuffle2Groups.add(max);
            currentMin = currentMax + 1;
            currentMax = currentMin + groupSize;
        }
        System.out.println("[Master] Reduce1: Groups for shuffle2 phase : " + shuffle2Groups);
    }

    /**
     * Send the order to start the shuffle2 phase to all slaves
     */
    public static void sendShuffle2Order() {

        if (state != MapReduceState.REDUCE1) {
            System.err.println("[Master] Error: Can't start shuffle2 phase, the current state is " + state);
            return;
        } else {
            System.out.println("[Master] Sending order to start shuffle2 phase to all slaves...");
            state = MapReduceState.SHUFFLE2;
        }

        makeShuffle2Groups();

        for (int i = 0; i < slavesCount; i++) {
            final int index = i;
            shuffle2MessagesTimes.set(index, System.currentTimeMillis());
            communicationHandlers.get(index).sendProtocolMessage(ProtocolMessage.START_SHUFFLE2);
            communicationHandlers.get(index).sendObject(shuffle2Groups);
        }

    }

    /**
     * Update the status of the shuffle for a slave
     * @param slaveID
     * @param status
     */
    public static void updateShuffle2MessageAcquittal(int slaveID, boolean status) {
        shuffle2MessagesTimes.set(slaveID, System.currentTimeMillis() - shuffle2MessagesTimes.get(slaveID));
        if (shuffle2Times.get(slaveID) == 0L) {
            shuffle2Times.set(slaveID, System.currentTimeMillis());
        }
    }

    /**
     * Update the status of the shuffle for a slave
     * @param slaveID
     * @param status
     */
    public static void updateShuffle2Status(int slaveID, boolean status) {
        slavesShuffle2Status.set(slaveID, status);
        shuffle2Times.set(slaveID, System.currentTimeMillis() - shuffle2Times.get(slaveID));

        if (slavesShuffle2Status.stream().allMatch(s -> s) && state == MapReduceState.SHUFFLE2) {
            sendReduce2Order();
        }
    }

    public static void sendReduce2Order() {

        if (state != MapReduceState.SHUFFLE2) {
            System.err.println("[Master] Error: Can't start reduce2 phase, the current state is " + state);
            return;
        } else {
            System.out.println("[Master] Sending order to start reduce2 phase to all slaves...");
            state = MapReduceState.REDUCE2;
        }

        for (int i = 0; i < slavesCount; i++) {
            final int index = i;
            communicationHandlers.get(index).sendProtocolMessage(ProtocolMessage.START_REDUCE2);
            reduce2MessagesTimes.set(index, System.currentTimeMillis());
            if (reduce2Times.get(index) == 0L) {
                reduce2Times.set(index, System.currentTimeMillis());
            }
        }
    }

    /**
     * Update the status of the reduce for a slave
     * @param slaveID
     * @param status
     */
    public static void updateReduce2Status(int slaveID, boolean status) {
        slavesReduce2Status.set(slaveID, status);
        reduce2Times.set(slaveID, System.currentTimeMillis() - reduce2Times.get(slaveID));

        if (slavesReduce2Status.stream().allMatch(s -> s) && state == MapReduceState.REDUCE2) {
            
            
            if (state == MapReduceState.REDUCE2) {
                buildReduce2Result();
            }

            state = MapReduceState.FINISHED;
        }
    }

    /**
     * Update the status of the reduce for a slave
     * @param slaveID
     * @param status
     */
    public static void updateReduce2MessageAcquittal(int slaveID, boolean status) {
        reduce2MessagesTimes.set(slaveID, System.currentTimeMillis() - reduce2MessagesTimes.get(slaveID));
    }

    public synchronized static void buildReduce2Result() {

        if (isFinished) {
            return;
        } else {
            isFinished = true;
        }   

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("[Master] Building the final result...");

        long startTime = System.currentTimeMillis();

        // Create a result file at the location of user dir
        String userDir = System.getProperty("user.dir");
        File resultFile = new File(userDir, RESULT_FILE_NAME);

        // clear the file if it already exists
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(resultFile))) {
            bos.write("".getBytes());
        } catch (IOException e) {
            System.err.println("[Master] Error while clearing the result file : " + e.getMessage());
        }

        // retreive the reduce2_result_{slaveID}.txt file from each slave via FTP, in the right order
        // and append the content to the result file
        for (int i = 0; i < slavesCount; i++) {
            InputStream is = communicationHandlers.get(i).getFileFTP("reduce2_result_" + i + ".txt");
            try (BufferedInputStream bis = new BufferedInputStream(is);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(resultFile, true))) {
                byte[] buffer = new byte[8192];
                int bytesRead = 0;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                System.err.println("[Master] Error while reading or writing file : " + e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        long buildResultTime = endTime - startTime;

        executionTime = System.currentTimeMillis() - executionTime;
        System.out.println("[Master] Finished: Execution time : " + executionTime + " ms");



        // Print the timings
        System.out.println("[Master] Timings : ");
        
        int mapMessagesTime = mapMessagesTimes.stream().mapToInt(Long::intValue).sum();
        int mapTime = mapMessagesTime - mapTimes.stream().mapToInt(Long::intValue).sum();
        System.out.println("[Master] Map time : " + mapTime + " ms");

        int shuffle1Time = shuffle1Times.stream().mapToInt(Long::intValue).sum();
        System.out.println("[Master] Shuffle1 time : " + shuffle1Time + " ms");

        int shuffle1MessagesTime = shuffle1MessagesTimes.stream().mapToInt(Long::intValue).sum();
        System.out.println("[Master] Shuffle1 messages time : " + shuffle1MessagesTime + " ms");

        int reduce1Time = reduce1Times.stream().mapToInt(Long::intValue).sum();
        System.out.println("[Master] Reduce1 time : " + reduce1Time + " ms");

        int shuffle2Time = shuffle2Times.stream().mapToInt(Long::intValue).sum();
        System.out.println("[Master] Shuffle2 time : " + shuffle2Time + " ms");

        int groupsMessagesTime = shuffle2MessagesTimes.stream().mapToInt(Long::intValue).sum();;
        System.out.println("[Master] Groups messages time : " + groupsMessagesTime + " ms");

        int reduce2Time = reduce2Times.stream().mapToInt(Long::intValue).sum();
        System.out.println("[Master] Reduce2 time : " + reduce2Time + " ms");

        communicationTime += shuffle1Time + shuffle2Time;
        synchronizationTime += groupsMessagesTime;
        computationTime += mapTime + reduce1Time + reduce2Time + buildResultTime;

        double ratio = ((double) communicationTime + (double) synchronizationTime) / (double) computationTime;
        System.out.println("[Master] Communication time : " + communicationTime + " ms");
        System.out.println("[Master] Synchronization time : " + synchronizationTime + " ms");
        System.out.println("[Master] Computation time : " + computationTime + " ms");
        System.out.println("[Master] Ratio : " + ratio);

        reset();
    }

    private static void reset() {
        state = MapReduceState.STARTING;
        slavesMapStatus = new ArrayList<Boolean>();
        slavesShuffle1Status = new ArrayList<Boolean>();
        slavesReduce1Status = new ArrayList<Boolean>();
        reduce1Min = null;
        reduce1Max = null;
        shuffle2Groups = new ArrayList<Integer>();
        slavesShuffle2Status = new ArrayList<Boolean>();
        slavesReduce2Status = new ArrayList<Boolean>();
        output = new HashMap<String, Integer>();

        for (CommunicationHandler communicationHandler : communicationHandlers) {
            communicationHandler.reset();
        }
    }
}
