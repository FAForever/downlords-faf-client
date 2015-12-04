package test;

import org.ice4j.StunException;
import org.ice4j.StunMessageEvent;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.attribute.RequestedTransportAttribute;
import org.ice4j.message.MessageFactory;
import org.ice4j.message.Request;
import org.ice4j.socket.IceUdpSocketWrapper;
import org.ice4j.stack.StunStack;
import org.ice4j.stunclient.BlockingRequestSender;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class TurnClient {

  private static final int STUN_PORT = 3478;
  private static final TransportAddress TURN_SERVER = new TransportAddress("dev.faforever.com", STUN_PORT, Transport.UDP);

  public static void main(String[] args) throws IOException, StunException {
    DatagramSocket datagramSocket = new DatagramSocket(0, InetAddress.getLocalHost());
    TransportAddress localAddress = new TransportAddress((InetSocketAddress) datagramSocket.getLocalSocketAddress(), Transport.UDP);

    StunStack stunStack = new StunStack();
    stunStack.addSocket(new IceUdpSocketWrapper(datagramSocket), TURN_SERVER);

    Request allocateRequest = MessageFactory.createAllocateRequest(RequestedTransportAttribute.UDP, false);

    BlockingRequestSender blockingRequestSender = new BlockingRequestSender(stunStack, localAddress);
    StunMessageEvent response = blockingRequestSender.sendRequestAndWaitForResponse(
        allocateRequest, TURN_SERVER
    );
    System.out.println(response);
  }
}
