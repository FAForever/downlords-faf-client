package com.faforever.client.upnp;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.xml.sax.SAXException;

import javax.annotation.PreDestroy;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.util.Objects;

public class WeUpnpServiceImpl implements UpnpService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String UDP = "UDP";

  @Value("${upnp.timeout}")
  int upnpTimeout;
  private GatewayDevice validGateway;
  private int mappedPort;

  @Override
  public void forwardPort(int port) {
    GatewayDiscover discover = new GatewayDiscover();

    logger.debug("Looking for UPnP capable gateway");

    try {
      discover.setTimeout(upnpTimeout);
      discover.discover();
      validGateway = discover.getValidGateway();

      if (validGateway == null) {
        logger.info("Could not find a UPnP capable gateway");
        return;
      }

      logger.info("Found UPnP capable gateway at {}", validGateway.getLocalAddress().getHostAddress());

      String localAddress = InetAddress.getLocalHost().getHostAddress();

      logger.debug("Looking for existing port mapping for {}:{}", localAddress, port);

      PortMappingEntry portMappingEntry = new PortMappingEntry();
      boolean mappingExists = validGateway.getSpecificPortMappingEntry(port, UDP, portMappingEntry);

      if (mappingExists) {
        String mappedIp = portMappingEntry.getInternalClient();
        int mappedPort = portMappingEntry.getInternalPort();
        if (Objects.equals(mappedIp, localAddress) && Objects.equals(mappedPort, port)) {
          logger.info("Port {} is already mapped to {}:{}, not changing anything", port, localAddress, port);
          return;
        } else {
          logger.info("Port {} is mapped to {}:{}. Replacing entry.", port, localAddress, port);
          deletePortMapping(port);
        }
      }

      addPortMapping(port, localAddress);
      logger.info("Port {} has been mapped to {}:{}", port, localAddress, port);
    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private void deletePortMapping(int port) throws IOException, SAXException {
    if (validGateway != null && !validGateway.deletePortMapping(port, UDP)) {
      logger.warn("Mapping for port {} could not be removed", port);
    }
  }

  private boolean addPortMapping(int port, String localAddress) throws IOException, SAXException {
    boolean added = validGateway.addPortMapping(port, port, localAddress, UDP, "Downlord's FAF Client");
    if (!added) {
      logger.warn("Port {} could not be mapped to {}:{}", port, localAddress, port);
    } else {
      mappedPort = port;
    }
    return added;
  }

  @PreDestroy
  void close() throws IOException, SAXException {
    deletePortMapping(mappedPort);
  }
}
