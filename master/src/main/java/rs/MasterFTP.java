package rs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class MasterFTP {

    private static final int FTP_PORT = 3456;
    private static final String FTP_USERNAME = "elegallic-22";
    private static final String FTP_PASSWORD = "slr207wew";//password

    private FTPClient ftpClient;

    public MasterFTP(String serverHostname) {
        init(serverHostname);
        listFiles();
        close();
    }

    public void write(String fileName, byte[] bytes) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ftpClient.storeFile(fileName, inputStream);
            int errorCode = ftpClient.getReplyCode();
            if (errorCode != 226) {
                System.out.println("[FTP] File upload failed. FTP Error code: " + errorCode);
            } else {
                System.out.println("[FTP] File uploaded successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void listFiles() {
        System.out.println("[FTP] Listing remote files");
        try {
            FTPFile[] files = ftpClient.listFiles();
            for (FTPFile file : files) {
                System.out.println(file.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init(String serverHost) {

        ftpClient = new FTPClient();

        try {
            ftpClient.connect(serverHost, FTP_PORT);
            ftpClient.login(FTP_USERNAME, FTP_PASSWORD);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
