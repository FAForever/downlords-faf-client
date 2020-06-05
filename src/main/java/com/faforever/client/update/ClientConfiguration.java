package com.faforever.client.update;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

// TODO since this class contains both, update info and configuration, the package 'update' doesn't really fit.

/**
 * A representation of a config file read from the faf server on start up. The file on the server allows to dynamically
 * change settings in the client remotely.
 */
@Data
public class ClientConfiguration {
  ReleaseInfo latestRelease = new ReleaseInfo();
  List<Integer> recommendedMaps = new LinkedList<>();
  List<Endpoints> endpoints = new LinkedList<>();
  GitHubRepo gitHubRepo = new GitHubRepo();

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
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReleaseInfo {
    ComparableVersion version;
    ComparableVersion minimumVersion;
    URL update4jConfigUrl;
    URL windowsUrl;
    URL linuxUrl;
    URL macUrl;
    boolean mandatory;
    String message;
    URL releaseNotesUrl;
  }
}