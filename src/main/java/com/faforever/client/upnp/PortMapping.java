package com.faforever.client.upnp;

/**
 * This immutable class represents a port mapping / forwarding on a router.
 */
public class PortMapping {

  public static final String MAPPING_ENTRY_LEASE_DURATION = "NewLeaseDuration";
  public static final String MAPPING_ENTRY_ENABLED = "NewEnabled";
  public static final String MAPPING_ENTRY_REMOTE_HOST = "NewRemoteHost";
  public static final String MAPPING_ENTRY_INTERNAL_CLIENT = "NewInternalClient";
  public static final String MAPPING_ENTRY_PORT_MAPPING_DESCRIPTION = "NewPortMappingDescription";
  public static final String MAPPING_ENTRY_PROTOCOL = "NewProtocol";
  public static final String MAPPING_ENTRY_INTERNAL_PORT = "NewInternalPort";
  public static final String MAPPING_ENTRY_EXTERNAL_PORT = "NewExternalPort";

  private static final long DEFAULT_LEASE_DURATION = 0;

  private final int externalPort;
  private final Protocol protocol;
  private final int internalPort;
  private final String description;
  private final String internalClient;
  private final String remoteHost;
  private final boolean enabled;
  private final long leaseDuration;

  public PortMapping(final Protocol protocol, final String remoteHost, final int externalPort,
                     final String internalClient, final int internalPort, final String description) {
    this(protocol, remoteHost, externalPort, internalClient, internalPort, description, true,
        DEFAULT_LEASE_DURATION);
  }

  public PortMapping(final Protocol protocol, final String remoteHost, final int externalPort,
                     final String internalClient, final int internalPort, final String description, final boolean enabled,
                     final long leaseDuration) {
    super();
    this.protocol = protocol;
    this.remoteHost = remoteHost;
    this.externalPort = externalPort;
    this.internalClient = internalClient;
    this.internalPort = internalPort;
    this.description = description;
    this.enabled = enabled;
    this.leaseDuration = leaseDuration;
  }

  public long getLeaseDuration() {
    return leaseDuration;
  }

  public int getExternalPort() {
    return externalPort;
  }

  public Protocol getProtocol() {
    return protocol;
  }

  public int getInternalPort() {
    return internalPort;
  }

  public String getDescription() {
    return description;
  }

  public String getInternalClient() {
    return internalClient;
  }

  public String getRemoteHost() {
    return remoteHost;
  }

  public boolean isEnabled() {
    return enabled;
  }

}
