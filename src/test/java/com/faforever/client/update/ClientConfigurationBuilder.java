package com.faforever.client.update;

import com.faforever.client.update.ClientConfiguration.GitHubRepo;
import com.faforever.client.update.ClientConfiguration.ReleaseInfo;
import com.faforever.client.update.ClientConfiguration.ServerEndpoints;
import com.faforever.client.update.ClientConfiguration.SocketEndpoint;
import com.faforever.client.update.ClientConfiguration.UrlEndpoint;

import java.net.URL;
import java.util.List;

public class ClientConfigurationBuilder {
  private final ClientConfiguration clientConfiguration = new ClientConfiguration();

  public static ClientConfigurationBuilder create() {
    return new ClientConfigurationBuilder();
  }

  public ClientConfigurationBuilder defaultValues() {
    ServerEndpoints serverEndpoints = ServerEndpointsBuilder.create().defaultValues().get();
    endpoints(List.of(serverEndpoints));
    latestRelease().minimumVersion("1.0.0").then();
    return this;
  }

  public LatestReleaseBuilder latestRelease() {
    return new LatestReleaseBuilder();
  }

  public ClientConfigurationBuilder endpoints(List<ServerEndpoints> endpoints) {
    clientConfiguration.setEndpoints(endpoints);
    return this;
  }

  public GitHubRepoBuilder gitHubRepo() {
    return new GitHubRepoBuilder();
  }

  public ClientConfiguration get() {
    return clientConfiguration;
  }

  public static class UrlEndpointBuilder {
    private final UrlEndpoint urlEndpoint = new UrlEndpoint();

    public static UrlEndpointBuilder create() {
      return new UrlEndpointBuilder();
    }

    public UrlEndpointBuilder defaultValues() {
      return this;
    }

    public UrlEndpointBuilder url(String url) {
      urlEndpoint.setUrl(url);
      return this;
    }

    public UrlEndpoint get() {
      return urlEndpoint;
    }

  }

  public static class SocketEndpointBuilder {
    private final SocketEndpoint socketEndpoint = new SocketEndpoint();

    public static SocketEndpointBuilder create() {
      return new SocketEndpointBuilder();
    }

    public SocketEndpointBuilder defaultValues() {
      return this;
    }

    public SocketEndpointBuilder host(String host) {
      socketEndpoint.setHost(host);
      return this;
    }

    public SocketEndpointBuilder port(int port) {
      socketEndpoint.setPort(port);
      return this;
    }

    public SocketEndpoint get() {
      return socketEndpoint;
    }

  }

  public static class ServerEndpointsBuilder {
    private final ServerEndpoints serverEndpoints = new ServerEndpoints();

    public static ServerEndpointsBuilder create() {
      return new ServerEndpointsBuilder();
    }

    public ServerEndpointsBuilder defaultValues() {
      lobby("lobby", 8001);
      irc("irc", 6667);
      liveReplay("liveReplay", 15000);
      api("api");
      oauth("oauth");
      return this;
    }

    public ServerEndpointsBuilder name(String name) {
      serverEndpoints.setName(name);
      return this;
    }

    public ServerEndpointsBuilder lobby(String host, int port) {
      serverEndpoints.setLobby(SocketEndpointBuilder.create().host(host).port(port).get());
      return this;
    }

    public ServerEndpointsBuilder irc(String host, int port) {
      serverEndpoints.setIrc(SocketEndpointBuilder.create().host(host).port(port).get());
      return this;
    }

    public ServerEndpointsBuilder liveReplay(String host, int port) {
      serverEndpoints.setLiveReplay(SocketEndpointBuilder.create().host(host).port(port).get());
      return this;
    }

    public ServerEndpointsBuilder api(String url) {
      serverEndpoints.setApi(UrlEndpointBuilder.create().url(url).get());
      return this;
    }

    public ServerEndpointsBuilder oauth(String url) {
      serverEndpoints.setOauth(UrlEndpointBuilder.create().url(url).get());
      return this;
    }

    public ServerEndpoints get() {
      return serverEndpoints;
    }
  }

  public class LatestReleaseBuilder {
    private final ReleaseInfo latestRelease = new ReleaseInfo();

    public LatestReleaseBuilder version(String version) {
      latestRelease.setVersion(version);
      return this;
    }

    public LatestReleaseBuilder minimumVersion(String minimumVersion) {
      latestRelease.setMinimumVersion(minimumVersion);
      return this;
    }

    public LatestReleaseBuilder windowsUrl(URL windowsUrl) {
      latestRelease.setWindowsUrl(windowsUrl);
      return this;
    }

    public LatestReleaseBuilder linuxUrl(URL linuxUrl) {
      latestRelease.setLinuxUrl(linuxUrl);
      return this;
    }

    public LatestReleaseBuilder macUrl(URL macUrl) {
      latestRelease.setMacUrl(macUrl);
      return this;
    }

    public LatestReleaseBuilder message(String message) {
      latestRelease.setMessage(message);
      return this;
    }

    public LatestReleaseBuilder releaseNotesUrl(URL releaseNotesUrl) {
      latestRelease.setReleaseNotesUrl(releaseNotesUrl);
      return this;
    }

    public ClientConfigurationBuilder then() {
      clientConfiguration.setLatestRelease(latestRelease);
      return ClientConfigurationBuilder.this;
    }

  }

  public class GitHubRepoBuilder {
    private final GitHubRepo gitHubRepo = new GitHubRepo();

    public GitHubRepoBuilder apiUrl(String apiUrl) {
      gitHubRepo.setApiUrl(apiUrl);
      return this;
    }

    public ClientConfigurationBuilder then() {
      clientConfiguration.setGitHubRepo(gitHubRepo);
      return ClientConfigurationBuilder.this;
    }

  }
}