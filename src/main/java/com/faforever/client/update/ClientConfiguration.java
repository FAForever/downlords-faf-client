package com.faforever.client.update;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;
import java.util.List;

// TODO since this class contains both, update info and configuration, the package 'update' doesn't really fit.

/**
 * A representation of a config file read from the faf server on start up. The file on the server allows to dynamically
 * change settings in the client remotely.
 */
@Data
public class ClientConfiguration {
  private ReleaseInfo latestRelease;
  private List<ServerEndpoints> endpoints;
  private GitHubRepo gitHubRepo;

  @Data
  public static class GitHubRepo {
    /**
     * Api URL to the client GitHub Repo
     */
    private String apiUrl;
  }

  @Data
  public static class ServerEndpoints {
    private String name;
    private SocketEndpoint lobby;
    private SocketEndpoint irc;
    private SocketEndpoint liveReplay;
    private UrlEndpoint api;
    private UrlEndpoint oauth;
  }

  @Data
  public static class SocketEndpoint {
    private String host;
    private int port;
  }

  @Data
  public static class UrlEndpoint {
    private String url;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReleaseInfo {
    private ComparableVersion version;
    private ComparableVersion minimumVersion;
    private URL update4jConfigUrl;
    private URL windowsUrl;
    private URL linuxUrl;
    private URL macUrl;
    private boolean mandatory;
    private String message;
    private URL releaseNotesUrl;
  }
}