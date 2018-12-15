package com.faforever.client.update;

import lombok.Data;

import java.net.URL;
import java.util.List;

@Data
// TODO since this class contains both, update info and configuration, the package 'update' doesn't really fit.
public class ClientConfiguration {
  ReleaseInfo latestRelease;
  List<Endpoints> endpoints;

  @Data
  public static class Endpoints {
    String name;
    SocketEndpoint lobby;
    SocketEndpoint irc;
    SocketEndpoint liveReplay;
    UrlEndpoint api;
  }

  @Data
  public static class SocketEndpoint {
    String host;
    int port;
  }

  @Data
  public static class UrlEndpoint {
    String url;
  }

  @Data
  public class ReleaseInfo {
    String version;
    URL windowsUrl;
    URL linuxUrl;
    URL macUrl;
    boolean mandatory;
    String message;
    URL releaseNotesUrl;
  }
}