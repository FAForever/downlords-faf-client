package com.faforever.client.update;

import lombok.Data;

import java.net.URL;
import java.util.List;

@Data
// TODO since this class contains both, update info and configuration, the package 'update' doesn't really fit.
/**
 * A representation of a config file read from the faf server on start up. The file on the server allows to dynamically change settings in the client remotely.
 */
public class ClientConfiguration {
  ReleaseInfo latestRelease;
  List<Integer> recommendedMaps;
  List<Endpoints> endpoints;
  GitHubRepo gitHubRepo;

  @Data
  public static class GitHubRepo {
    /**
     * Api URL to the client GitHub Repo
     */
    private String apiUrl;
  }

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
  public static class ReleaseInfo {
    String version;
    String minimumVersion;
    URL update4jConfigUrl;
    URL windowsUrl;
    URL linuxUrl;
    URL macUrl;
    boolean mandatory;
    String message;
    URL releaseNotesUrl;
  }
}