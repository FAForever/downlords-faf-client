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

public class TurnClient {

  private static final int STUN_PORT = 3478;
  private static final TransportAddress TURN_SERVER = new TransportAddress("dev.faforever.com", STUN_PORT, Transport.UDP);
  private static final int GAME_PORT = 6112;

  public static void main(String[] args) throws IOException, StunException {
    TransportAddress localAddress = new TransportAddress(InetAddress.getLocalHost(), GAME_PORT, Transport.UDP);
    DatagramSocket datagramSocket = new DatagramSocket(GAME_PORT, localAddress.getAddress());

    StunStack stunStack = new StunStack();
    stunStack.addSocket(new IceUdpSocketWrapper(datagramSocket));

    Request allocateRequest = MessageFactory.createAllocateRequest(RequestedTransportAttribute.UDP, false);

    BlockingRequestSender blockingRequestSender = new BlockingRequestSender(stunStack, localAddress);
    StunMessageEvent response = blockingRequestSender.sendRequestAndWaitForResponse(
        allocateRequest, TURN_SERVER
    );
  }
}
