package rs;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * TODO : 
 * - Add path resolving for the files
 */

public class Master {

    private static final String SPLIT_FOLDER_NAME = "splits";

    private static int slavesCount;
    private static ArrayList<String> slavesHostnames = new ArrayList<String>();
    private static ArrayList<MasterListenerThread> slavesListenerThreads = new ArrayList<MasterListenerThread>();
    private static ArrayList<Boolean> slavesShuffleStatus = new ArrayList<Boolean>();
    private static ArrayList<Boolean> slavesReduceStatus = new ArrayList<Boolean>();
    private static HashMap<String, Integer> output = new HashMap<String, Integer>();

    public static void main(String[] args) {

        if(args.length != 2) {
			System.err.println("syntax: java -jar Master.jar <slaves hostnames file path> <source file path>");
			System.exit(1);
		}

        String slavesHostnamesFileName = args[0];
        retreiveSlavesHostnames(slavesHostnamesFileName);

        String sourceFilePath = args[1];
        splitToFiles(sourceFilePath, slavesCount);

        sendSplits(slavesHostnames);
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
                System.out.println("Added slave!  (hostname : " + line.trim() + ")");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier : " + file.getAbsolutePath() + " (" + e.getMessage() + ")");
        }
    }

    /**
     * Split the file into multiple parts
     * @param filePath
     * @param splitsFolderPath
     * @param numberOfSlaves
     */
    public static void splitToFiles(String sourceFilePath, int numberOfSlaves) {
        System.out.println("Splitting file into " + numberOfSlaves + " parts...");
        
        // Create a folder named SPLIT_FOLDER_NAME at workind_dir/SPLIT_FOLDER_NAME if it doesn't exist
        String userDir = System.getProperty("user.dir");
        File splitsFolder = new File(userDir, SPLIT_FOLDER_NAME);
        if (!splitsFolder.exists()) {
            System.out.println("Creating folder : " + splitsFolder.getAbsolutePath());
            splitsFolder.mkdir();
        }

        // Read the source file using BufferedInputStream
        // Split the file in chunks of size = file_size / numberOfSlaves (make sure to stop at a space character not to cut a word)
        // Write the chunks to a file named SPLIT_FOLDER_NAME/split_i.txt
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFilePath))) {
            long fileSize = new File(sourceFilePath).length();
            long chunkSize = fileSize / numberOfSlaves; // taille de chaque morceau
            System.out.println("Chunk size : " + chunkSize);
            
            int defaultMaxBufferSize = 8192;

            for (int i = 0; i < numberOfSlaves; i++) {
                System.out.println("\n\nSplitting file " + i + "...");
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

            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture ou de l'écriture de fichier : " + e.getMessage());
        }
    }

    /**
     * Send the splits to the slaves
     * @param splitsFolderPath
     * @param slavesHostnames
     */
    public static void sendSplits(ArrayList<String> slavesHostnames) {

    }

    /**
     * Update the status of the shuffle for a slave
     * @param slaveID
     * @param status
     */
    public static void updateShuffleStatus(String slaveID, boolean status) {

    }

    public static void connectSocket(String host) {

       Socket socketOfClient = null;
       BufferedWriter os = null;
       BufferedReader is = null;

       try {
           // Send a request to connect to the server is listening
           // on machine 'localhost' port 9999.
           socketOfClient = new Socket(host, 9999);

           // Create output stream at the client (to send data to the server)
           os = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));
           // Input stream at Client (Receive data from the server).
           is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));

       } catch (UnknownHostException e) {
           System.err.println("Don't know about host " + host);
           return;
       } catch (IOException e) {
           System.err.println("Couldn't get I/O for the connection to " + host);
           return;
       }

       try {
           // Write data to the output stream of the Client Socket.
           os.write("HELLO");
           os.newLine(); // End of line
   
           // Flush data.
           os.flush();  
           os.write("I am Tom Cat");
           os.newLine();
           os.flush();
           os.write("QUIT");
           os.newLine();
           os.flush();
           
           // Read data sent from the server.
           // By reading the input stream of the Client Socket.
           String responseLine;
           while ((responseLine = is.readLine()) != null) {
               System.out.println("Server: " + responseLine);
               if (responseLine.indexOf("OK") != -1) {
                   break;
               }
           }

           os.close();
           is.close();
           socketOfClient.close();
       } catch (UnknownHostException e) {
           System.err.println("Trying to connect to unknown host: " + e);
       } catch (IOException e) {
           System.err.println("IOException:  " + e);
       }
    }

    public static void sendFTPfile(String serverHost) {
        int FTPport = 3456;
        String FTPusername = "elegallic-22";
        String FTPpassword = "slr207wew";//password

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(serverHost, FTPport);
            ftpClient.login(FTPusername, FTPpassword);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Code to display files
            FTPFile[] files = ftpClient.listFiles();
            boolean fileExists = false;
            for (FTPFile file : files) {
                if (file.getName().equals("helloworld.txt")) {
                    fileExists = true;
                    break;
                }
            }

            if (!fileExists) {
                String content = "hello world";
                ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
                ftpClient.storeFile("helloworld.txt", inputStream);
                int errorCode = ftpClient.getReplyCode();
                if (errorCode != 226) {
                    System.out.println("File upload failed. FTP Error code: " + errorCode);
                } else {
                    System.out.println("File uploaded successfully.");
                }
            } else {
                // Code to retrieve and display file content
                InputStream inputStream = ftpClient.retrieveFileStream("helloworld.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                reader.close();
                ftpClient.completePendingCommand();
            }

            ftpClient.logout();
            ftpClient.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
