import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by mandeep on 8/9/14.
 */
public class Packets {

    InetAddress TFTPServerIP;

    Packets(String ip) throws UnknownHostException {
        this.TFTPServerIP = InetAddress.getByName(ip);
    }
    
    // Construct new read packet for given filename.
    public DatagramPacket getReadPacket(String fileName){

        byte [] packet = new byte[fileName.length() + 20];
        int lengthFileName = fileName.length();
        String payLoad = "01" + fileName + "0octet0";
        packet = payLoad.getBytes();
        packet[0] = 0;
        packet[1] = 1;
        packet[lengthFileName + 2] = 0;
        packet[lengthFileName + 8] = 0;
	
	// TFTP listens at port 69.
        return new DatagramPacket(packet, packet.length, TFTPServerIP, 69);
    }
    
    // Get acknowledgement for packet.
    public DatagramPacket getACK(DatagramPacket packet){

        byte [] packetData = packet.getData();
	
	// Construct ACK for packet ID read from packet itself.
        byte [] ack = {0, 4, packetData[2], packetData[3]};

        return new DatagramPacket(ack, ack.length, TFTPServerIP, packet.getPort());
    }
}
