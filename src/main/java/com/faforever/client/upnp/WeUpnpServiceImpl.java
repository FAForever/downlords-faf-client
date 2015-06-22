package com.faforever.client.upnp;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;

public class WeUpnpServiceImpl implements UpnpService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String UDP = "UDP";

  @Override
  public void forwardPort(int port) {
    GatewayDiscover discover = new GatewayDiscover();

    logger.debug("Looking for UPnP capable gateway");

    try {
      discover.discover();
      GatewayDevice validGateway = discover.getValidGateway();

      if (validGateway == null) {
        logger.info("Could not find a UPnP capable gateway");
        return;
      }

      logger.info("Found UPnP capable gateway at {}", validGateway.getLocalAddress().getHostAddress());

      String localAddress = InetAddress.getLocalHost().getHostAddress();

      logger.debug("Looking for existing port mapping for {}:{}", localAddress, port);

      PortMappingEntry portMappingEntry = new PortMappingEntry();
      boolean found = validGateway.getSpecificPortMappingEntry(port, UDP, portMappingEntry);

      if (found) {
        logger.info("Port {} is already mapped to {}:{}. Removing existing entry.", port, localAddress, port);
        if (!validGateway.deletePortMapping(port, UDP)) {
          logger.warn("Mapping for port {} could not be removed", port);
        }
      }

      logger.info("Trying to map port {} to {}:{}", port, localAddress, port);
      boolean added = validGateway.addPortMapping(port, port, localAddress, "UDP", "FAF Client");
      if (!added) {
        logger.warn("Port {} could not be mapped to {}:{}", port, localAddress, port);
        return;
      }

      logger.info("Port {} has been mapped to {}:{}", port, localAddress, port);

    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }
}
