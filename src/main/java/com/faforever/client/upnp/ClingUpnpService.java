package com.faforever.client.upnp;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ClingUpnpService implements UpnpService {

  private static final long DISCOVERY_TIMEOUT_SECONDS = 5;

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final long LEASE_DURATION = 3000;

  private UpnpServiceImpl clingUpnpService;
  private ClingRegistryListener clingRegistryListener;

  @Override
  public void forwardPort(int port) {
    List<ClingRouter> routers = findRouters();
    if (routers.isEmpty()) {
      logger.info("No UPnP capable router has been found.");
      return;
    }

    ClingRouter router = routers.get(0);

    if (routers.size() > 1) {
      logger.warn("More than one UPnP capable router has been found. Using {}", router.getInternalHostName());
    }

    InetAddress localHost;
    try {
      localHost = InetAddress.getLocalHost();

      final PortMapping mapping = new PortMapping(Protocol.UDP, null, port, localHost.getHostAddress(), port, "FAF game port");

      ActionService actionService = new ActionService(router.getService(), clingUpnpService.getControlPoint());
      actionService.run(new AddPortMappingAction(router.getService(), mapping));

      router.addPortMapping(mapping);
    } catch (UnknownHostException | RouterException e) {
      throw new IllegalStateException(e);
    }
  }

  private UpnpServiceImpl getService() {
    if (clingUpnpService == null) {
      final UpnpServiceConfiguration config = new DefaultUpnpServiceConfiguration();
      clingRegistryListener = new ClingRegistryListener();
      clingUpnpService = new UpnpServiceImpl(config, clingRegistryListener);
      clingUpnpService.getControlPoint().search();
    }

    return clingUpnpService;
  }

  protected List<ClingRouter> findRouters() {
    UpnpServiceImpl upnpService = getService();

    logger.debug("Start searching using upnp service");
    upnpService.getControlPoint().search();

    final RemoteService service = (RemoteService) clingRegistryListener.waitForServiceFound(
        DISCOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    if (service == null) {
      logger.debug("Did not find a service after {} seconds", DISCOVERY_TIMEOUT_SECONDS);
      return Collections.emptyList();
    }

    logger.debug("Found service {}", service);
    return Collections.singletonList(new ClingRouter(service, upnpService.getRegistry(), upnpService.getControlPoint()));
  }
}
