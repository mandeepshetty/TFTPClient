import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by mandeep on 8/9/14.
 */


public class TFTPClient {


    static Packets p;

    static String server;

    public static void main(String[] args) throws IOException, InterruptedException {

        Scanner in = new Scanner(System.in);
        try {
            while (true) {
		// Prompt input.
                System.out.print("myTftp >");
                String command = in.nextLine();
                processCommand(command);
            }
        } catch (Exception exp) {
            System.out.println(exp.getMessage());
        }
    }

    // Print a message with preceding prompt.
    public static void printPromptMessage(String message) {
        System.out.println("myTftp > " + message);
    }

    private static void processCommand(String command) throws IOException {

        StringTokenizer st = new StringTokenizer(command);

        // When enter pressed without any command.
        if (st.countTokens() == 0) return;

        // Performing actions acc. to command.
        String comm = st.nextToken();
        if (comm.equalsIgnoreCase("get")) {
            processGet(st);
        } else if (comm.equalsIgnoreCase("connect")) {
            processConnect(st);
        }
        else if (comm.equalsIgnoreCase("quit")) System.exit(0);
        else    printPromptMessage("Invalid command");

    }
    
    // Check for server name and attempt to connect to it.
    private static void processConnect(StringTokenizer st) {
        try {
            // Check for server name.
            if (!st.hasMoreTokens()) {
                printPromptMessage("Server name not given.");
                return;
            }
            p = new Packets(st.nextToken());
            System.out.println("Connected to " + p.TFTPServerIP);
        } catch (UnknownHostException unk) {
            printPromptMessage("Unknown host.");
            return;
        }
    }
	
    private static void processGet(StringTokenizer st) throws IOException {
	
	// Check if connection has been established.
    	if (p == null) {
            printPromptMessage("Connect to server with: connect <server>");
            return;
        }
        if (st.hasMoreTokens()){
            while (st.hasMoreTokens())
                getFile(st.nextToken().trim());
        }
        else {
            printPromptMessage("get file1 file2 ....");
            return;
        }
    }

    private static void getFile(String fileName) throws IOException {
        if (fileName.isEmpty()) {
            printPromptMessage("get file1 file2 ...");
            return;
        }
        DatagramSocket clientSocket = new DatagramSocket();
        DatagramPacket sendPacket = p.getReadPacket(fileName);

        clientSocket.send(sendPacket);
        DatagramPacket receivePacket;
        String dirPath = "./mhs1841";
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdir();
        File f = new File(fileName);

        OutputStream os = new FileOutputStream(dir + "/" + f.getName(), true);
        long size = 0;
        long start = System.currentTimeMillis();
        try {
            do {
                receivePacket = receivePacket(clientSocket);
                int opcode = receivePacket.getData()[1];
		// Data packet with opcode =  3
                if (opcode == 3) {
                    size = processDataPacket(clientSocket, receivePacket, os, size, start, f.getName());
                }
                // Error message
                else if (opcode == 5) {
                    printPromptMessage(new String(receivePacket.getData(), 4, receivePacket.getLength() - 2));

		    // Delete partial file.
                    new File(dir + "/" + f.getName()).delete();
                    break;
                }

            } while (receivePacket.getLength() == 516);
            os.close();
            clientSocket.close();
        } catch (SocketTimeoutException stExp) {
            printPromptMessage("Timeout.");
            new File(dir + "/" + f.getName()).delete();
        }
    }

    private static long processDataPacket(DatagramSocket clientSocket,
                                          DatagramPacket receivePacket, OutputStream os,
                                          long size, long start, String file) throws IOException {
        byte[] data = receivePacket.getData();
        os.write(data, 4, receivePacket.getLength() - 4);
        System.out.print("\r");

        // Get ACK for received packet.
        DatagramPacket ack = p.getACK(receivePacket);

        size += receivePacket.getLength() - 4;

        // Write status.
        System.out.print("Downloaded " + size + " bytes in " +
                (System.currentTimeMillis() - start) / 1000 +
                " seconds");

        // Send ack
        clientSocket.send(ack);
        if (receivePacket.getLength() < 516)
            System.out.println("\n" + file +" done." );
        return size;
    }


    private static DatagramPacket receivePacket(DatagramSocket clientSocket) throws IOException {
        byte[] receiveData = new byte[516];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.setSoTimeout(10000);
        clientSocket.receive(receivePacket);
        return receivePacket;
    }
}
