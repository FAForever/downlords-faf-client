package com.faforever.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "faf-client", ignoreUnknownFields = false)
public class ClientProperties {

  private String mainWindowTitle = "Downlord's FAF Client";
  private News news = new News();
  private ForgedAlliance forgedAlliance = new ForgedAlliance();
  private Irc irc = new Irc();
  private Server server = new Server();
  private Vault vault = new Vault();
  private Replay replay = new Replay();
  private Imgur imgur = new Imgur();
  private TrueSkill trueSkill = new TrueSkill();
  private Api api = new Api();
  private UnitDatabase unitDatabase = new UnitDatabase();
  private MapGenerator mapGenerator = new MapGenerator();
  private Website website = new Website();
  private Discord discord = new Discord();
  private String translationProjectUrl;
  private String clientConfigUrl;
  private boolean useRemotePreferences;
  private Duration clientConfigConnectTimeout = Duration.ofSeconds(30);
  private boolean showIceAdapterDebugWindow;
  private String statusPageUrl;
  private Map<String, String> links = new HashMap<>();
  private List<String> vanillaGameHashes = new ArrayList<>();

  @Data
  public static class News {
    /**
     * URL to fetch the RSS news feed from.
     */
    private String feedUrl;
  }

  @Data
  public static class ForgedAlliance {
    /**
     * Title of the Forged Alliance window. Required to find the window handle.
     */
    private String windowTitle = "Forged Alliance";

    /**
     * URL to download the ForgedAlliance.exe from.
     */
    private String exeUrl;
  }

  @Data
  public static class Irc {
    private String host;
    private int port = 8167;
    /**
     * Channel to join by default.
     *
     * @deprecated shouldn't be known by the client but sent from the server.
     */
    @Deprecated
    private String defaultChannel = "#aeolus";
    private int reconnectDelay = (int) Duration.ofSeconds(5).toMillis();
  }

  @Data
  public static class Server {
    private String host;
    private int port = 8001;
  }

  @Data
  public static class Vault {
    private String baseUrl;
    private String rulesUrl;
    private String mapValidationUrl;
    private String mapDownloadUrlFormat;
    private String mapPreviewUrlFormat;
    private String replayDownloadUrlFormat;
    private String modDownloadUrlFormat;
  }

  @Data
  public static class Replay {
    private String remoteHost;
    private int remotePort = 15000;
    private String replayFileFormat = "%d-%s.fafreplay";
    private String replayFileGlob = "*.fafreplay";
    // TODO this should acutally be reported by the server
    private int watchDelaySeconds = 300;
  }

  @Data
  public static class Imgur {
    private Upload upload = new Upload();

    @Data
    public static class Upload {
      private String baseUrl = "https://api.imgur.com/3/image";
      private String clientId;
      private int maxSize = 2097152;
    }
  }

  /**
   * @deprecated load from server
   */
  @Data
  @Deprecated
  public static class TrueSkill {
    private int initialStandardDeviation;
    private int initialMean;
    private int beta;
    private float dynamicFactor;
    private float drawProbability;
  }

  @Data
  public static class Website {
    private String baseUrl;
    private String forgotPasswordUrl;
    private String createAccountUrl;
    private String newsHubUrl;
  }

  @Data
  public static class Api {
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private int maxPageSize = 10_000;
  }

  @Data
  public static class UnitDatabase {
    private String spookiesUrl;
    private String rackOversUrl;
  }

  @Data
  public static class MapGenerator {
    private String downloadUrlFormat;
    private String repoAndOwnerName;
    private String queryLatestVersionUrl;
    private String queryVersionsUrl;
    private int maxSupportedMajorVersion;
    private int minSupportedMajorVersion;
  }

  @Data
  public static class Discord {
    private String applicationId;
    private String smallImageKey;
    private String bigImageKey;
    private String discordPrereleaseFeedbackChannelUrl;
  }
}
