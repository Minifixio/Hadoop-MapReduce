package rs;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * TODO : 
 * - Add path resolving for the files
 */

public class Master {

    private static String sourceFilePath;
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

        sourceFilePath = args[1];
        String splitsFolderPath = "splits";
        splitToFiles(sourceFilePath, splitsFolderPath, slavesCount);

        sendSplits(splitsFolderPath, slavesHostnames);
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
    public static void splitToFiles(String sourceFilePath, String splitsFolderPath, int numberOfSlaves) {

    }

    /**
     * Send the splits to the slaves
     * @param splitsFolderPath
     * @param slavesHostnames
     */
    public static void sendSplits(String splitsFolderPath, ArrayList<String> slavesHostnames) {

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
