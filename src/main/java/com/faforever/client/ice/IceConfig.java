package com.faforever.client.ice;

import com.nbarraille.jjsonrpc.JJsonPeer;
import com.nbarraille.jjsonrpc.TcpClient;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
public class IceConfig {

  @Bean
  public WindowsIceAdapterService windowsIceAdapterService() {
    return new WindowsIceAdapterService();
  }

  @Bean
  public TcpClient rpcClient(IceAdapterClientApi iceAdapterClientApi) {
    return noCatch(() -> new TcpClient("localhost", 7236, iceAdapterClientApi));
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public IceAdapterClient iceAdapterClient(TcpClient tcpClient) {
    return (IceAdapterClient) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IceAdapterClient.class},
        (proxy, method, args) -> {
          JJsonPeer peer = tcpClient.getPeer();
          if (!peer.isAlive()) {
            peer.start();
          }

          peer.sendAsyncRequest(method.getName(), Arrays.asList(args), null, false);
          return null;
        }
    );
  }
}
