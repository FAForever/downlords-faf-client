package com.faforever.client.upnp;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.meta.UDAVersion;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collection;

public class ClingRouter {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The maximum number of port mappings that we will try to retrieve from the router.
   */
  private final static int MAX_NUM_PORTMAPPINGS = 500;


  private final RemoteService service;

  private final Registry registry;

  private final ActionService actionService;

  public ClingRouter(final RemoteService service, final Registry registry, final ControlPoint controlPoint) {
    this.service = service;
    this.registry = registry;
    actionService = new ActionService(service, controlPoint);
  }

  public RemoteService getService() {
    return service;
  }

  public String getInternalHostName() {
    final URI uri = getUri();
    return uri != null ? uri.getHost() : null;
  }

  public int getInternalPort() throws RouterException {
    final URI uri = getUri();
    return uri != null ? uri.getPort() : null;
  }

  private URI getUri() {
    if (service.getDevice().getDetails().getPresentationURI() != null) {
      return service.getDevice().getDetails().getPresentationURI();
    }
    if (service.getControlURI() != null) {
      return service.getControlURI();
    }
    if (service.getDescriptorURI() != null) {
      return service.getDescriptorURI();
    }
    if (service.getEventSubscriptionURI() != null) {
      return service.getEventSubscriptionURI();
    }
    return null;
  }

  public void logRouterInfo() throws RouterException {
    logger.info("Service id: " + service.getServiceId());
    logger.info("Reference: " + service.getReference());
    logger.info("Display name: " + service.getDevice().getDisplayString());
    final UDAVersion version = service.getDevice().getVersion();
    logger.info("Version: " + version.getMajor() + "." + version.getMinor());
    logger.info("Control uri: {}", service.getControlURI());
    logger.info("Descriptor uri: {}", service.getDescriptorURI());
    logger.info("Event subscription uri: {}", service.getEventSubscriptionURI());
    logger.info("Device base url: {}", service.getDevice().getDetails().getBaseURL());
    logger.info("Device presentation uri: {}", service.getDevice().getDetails().getPresentationURI());
  }

  public void addPortMappings(final Collection<PortMapping> mappings) throws RouterException {
    for (final PortMapping portMapping : mappings) {
      addPortMapping(portMapping);
    }
  }

  public void addPortMapping(final PortMapping mapping) throws RouterException {
    actionService.run(new AddPortMappingAction(service, mapping));
  }

  public void removeMapping(final PortMapping mapping) throws RouterException {
    actionService.run(new DeletePortMappingAction(service, mapping));
  }

  public void removePortMapping(final Protocol protocol, final String remoteHost, final int externalPort)
      throws RouterException {
    removeMapping(new PortMapping(protocol, remoteHost, externalPort, null, 0, null));
  }

  public void disconnect() {
    logger.debug("Shutdown registry");
    registry.shutdown();
  }
}
