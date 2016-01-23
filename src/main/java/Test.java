import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class Test {

  public static void main(String[] args) throws SocketException {

    DatagramSocket socket1 = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
    DatagramSocket socket2 = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

    new Thread(new Runnable() {
      @Override
      public void run() {
        byte[] data = new byte[]{0x01};
        DatagramPacket packet = new DatagramPacket(data, data.length);
//        socket1.receive(packet);
      }
    });
  }
}
