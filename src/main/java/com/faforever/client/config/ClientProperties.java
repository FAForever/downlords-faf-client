package com.faforever.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "faf-client", ignoreUnknownFields = false)
public class ClientProperties {

  private String mainWindowTitle = "Downlord's FAF Client";
  private News news = new News();
  private ForgedAlliance forgedAlliance = new ForgedAlliance();
  private Irc irc = new Irc();
  private Server server = new Server();
  private Ice ice = new Ice();
  private Vault vault = new Vault();
  private Replay replay = new Replay();
  private GitHub gitHub = new GitHub();
  private Imgur imgur = new Imgur();
  private TrueSkill trueSkill = new TrueSkill();
  private Api api = new Api();
  private UnitDatabase unitDatabase = new UnitDatabase();
  private Website website = new Website();
  private String translationProjectUrl;

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
  public static class Ice {
    private Turn turn = new Turn();
    private Stun stun = new Stun();

    @Data
    public static class Turn {
      private String host;
      private int port = 3478;
    }

    @Data
    public static class Stun {
      private String host;
    }
  }

  @Data
  public static class Vault {
    private String baseUrl;
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
  public static class GitHub {
    private String releasesUrl = "https://api.github.com/repos/FAForever/downlords-faf-client/releases";
    private int timeout = (int) Duration.ofSeconds(5).toMillis();
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
}
