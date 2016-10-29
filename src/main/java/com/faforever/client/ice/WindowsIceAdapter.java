package com.faforever.client.ice;

import com.nbarraille.jjsonrpc.JJsonPeer;
import com.nbarraille.jjsonrpc.TcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.util.SocketUtils;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.os.OsUtils.gobbleLines;
import static java.util.Arrays.asList;

// TODO except the file name, windows/linux/mac will most likely use the exact same code.
public class WindowsIceAdapter implements IceAdapter, IceAdapterApi {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${stun.host}")
  String stunServerAddress;

  @Value("${turn.host}")
  String turnServerAddress;

  @Resource
  ApplicationContext applicationContext;

  private CompletableFuture<Void> iceAdapterClientFuture;
  private Process process;
  private IceAdapterApi iceAdapterProxy;

  @Override
  public CompletableFuture<Void> start() {
    iceAdapterClientFuture = new CompletableFuture<>();
    new Thread(() -> {
      // TODO rename to nativeDir and reuse in UID service
      String uidDir = System.getProperty("uid.dir", "lib");

      int adapterPort = SocketUtils.findAvailableTcpPort();

      String[] cmd = new String[]{
          Paths.get(uidDir, "faf-ice-adapter.exe").toAbsolutePath().toString(),
          "-s", stunServerAddress,
          "-t", turnServerAddress,
          "-p", String.valueOf(adapterPort)
      };

      try {
        logger.debug("Starting ICE adapter with command: {}", (Object[]) cmd);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(cmd);

        process = processBuilder.start();
        gobbleLines(process.getInputStream(), logger::debug);
        gobbleLines(process.getErrorStream(), logger::error);

        IceAdapterCallbacks iceAdapterCallbacks = applicationContext.getBean(IceAdapterCallbacks.class);

        TcpClient tcpClient = new TcpClient("localhost", adapterPort, iceAdapterCallbacks);
        iceAdapterProxy = newIceAdapterProxy(tcpClient.getPeer());

        iceAdapterClientFuture.complete(null);

        int exitCode = process.waitFor();
        if (exitCode == 0) {
          logger.debug("ICE adapter terminated normally");
        } else {
          logger.warn("ICE adapter terminated with exit code: {}", exitCode);
        }
      } catch (Exception e) {
        iceAdapterClientFuture.completeExceptionally(e);
      }
    }).start();

    return iceAdapterClientFuture;
  }

  private IceAdapterApi newIceAdapterProxy(JJsonPeer peer) {
    return (IceAdapterApi) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IceAdapterApi.class},
        (Object proxy, Method method, Object[] args) -> {
          List<Object> argList = asList(args);
          if (!peer.isAlive()) {
            logger.warn("Ignoring call to ICE adapter as we are not connected: {}({})", method.getName(), argList);
            return null;
          }
          logger.debug("Calling {}({})", method.getName(), argList);
          peer.sendAsyncRequest(method.getName(), argList, null, false);
          return null;
        }
    );
  }

  @Override
  public void stop() {
    quit();
  }

  @Override
  public void quit() {
    iceAdapterProxy.quit();
  }

  @Override
  public void hostGame(String mapName) {
    iceAdapterProxy.hostGame(mapName);
  }

  @Override
  public void joinGame(String remotePlayerLogin, int remotePlayerId) {
    iceAdapterProxy.joinGame(remotePlayerLogin, remotePlayerId);
  }

  @Override
  public void connectToPeer(String remotePlayerLogin, int remotePlayerId) {
    iceAdapterProxy.connectToPeer(remotePlayerLogin, remotePlayerId);
  }

  @Override
  public void disconnectFromPeer(int remotePlayerId) {
    iceAdapterProxy.disconnectFromPeer(remotePlayerId);
  }

  @Override
  public void setSdp(int remotePlayerId, String sdp64) {
    iceAdapterProxy.setSdp(remotePlayerId, sdp64);
  }

  @Override
  public void sendToGpgNet(String header, List<Object> chunks) {
    iceAdapterProxy.sendToGpgNet(header, chunks);
  }

  @Override
  public void status(String header, List<Object> chunks) {
    iceAdapterProxy.status(header, chunks);
  }
}
