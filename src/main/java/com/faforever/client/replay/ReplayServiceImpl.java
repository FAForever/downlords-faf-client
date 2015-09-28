package com.faforever.client.replay;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameType;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.TaskService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.primitives.Bytes;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

public class ReplayServiceImpl implements ReplayService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Byte offset at which a SupCom replay's version number starts.
   */
  private static final int VERSION_OFFSET = 0x18;
  private static final String FAF_REPLAY_FILE_ENDING = ".fafreplay";
  private static final String SUP_COM_REPLAY_FILE_ENDING = ".scfareplay";
  private static final String FAF_LIFE_PROTOCOL = "faflive";
  private static final String GPGNET_SCHEME = "gpgnet";
  private static final String TEMP_SCFA_REPLAY_FILE_NAME = "temp.scfareplay";

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  ReplayFileReader replayFileReader;

  @Autowired
  NotificationService notificationService;

  @Autowired
  GameService gameService;

  @Autowired
  TaskService taskService;

  @Autowired
  I18n i18n;

  @Autowired
  ReportingService reportingService;

  @Autowired
  ReplayServerAccessor replayServerAccessor;

  @Autowired
  ApplicationContext applicationContext;

  boolean urlFactoryHasBeenSet = false;

  @Override
  public Collection<ReplayInfoBean> getLocalReplays() throws IOException {
    Collection<ReplayInfoBean> replayInfos = new ArrayList<>();

    String replayFileGlob = environment.getProperty("replayFileGlob");

    Path replaysDirectory = preferencesService.getReplaysDirectory();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(replaysDirectory, replayFileGlob)) {
      for (Path replayFile : directoryStream) {
        LocalReplayInfo replayInfo = null;
        try {
          replayInfo = replayFileReader.readReplayInfo(replayFile);
          if (replayInfo == null) {
            moveCorruptedReplayFile(replayFile);
            continue;
          }
          replayInfos.add(new ReplayInfoBean(replayInfo, replayFile));
        } catch (Exception e) {
          logger.warn("Could not read replay file {}", replayFile);
          moveCorruptedReplayFile(replayFile);
        }
      }
    }

    return replayInfos;
  }

  private void moveCorruptedReplayFile(Path replayFile) throws IOException {
    Path corruptedReplaysDirectory = preferencesService.getCorruptedReplaysDirectory();
    Files.createDirectories(corruptedReplaysDirectory);

    Path target = corruptedReplaysDirectory.resolve(replayFile.getFileName());

    logger.debug("Moving corrupted replay file from {} to {}", replayFile, target);

    Files.move(replayFile, target);

    notificationService.addNotification(new PersistentNotification(
        i18n.get("corruptedReplayFiles.notification"),
        Severity.WARN,
        Collections.singletonList(
            new Action(i18n.get("corruptedReplayFiles.show"), event -> {
              try {
                // Argh, using AWT since JavaFX doesn't provide a proper method :-(
                Desktop.getDesktop().open(corruptedReplaysDirectory.toFile());
              } catch (IOException e) {
                logger.warn("Could not reveal corrupted replay directory", e);
              }
            })
        )
    ));
  }

  @Override
  public CompletableFuture<List<ReplayInfoBean>> getOnlineReplays() {
    return replayServerAccessor.requestOnlineReplays();
  }

  @Override
  public void runReplay(ReplayInfoBean item) {
    if (item.getReplayFile() != null) {
      runReplayFile(item.getReplayFile());
    } else {
      runOnlineReplay(item.getId());
    }
  }

  @Override
  public void runLiveReplay(int replayId, String playerName) throws IOException {
    //FIXME if getByUid returns null then handle null

    GameInfoBean gameInfoBean = gameService.getByUid(replayId);

    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme(FAF_LIFE_PROTOCOL);
    uriBuilder.setHost(environment.getProperty("lobby.host"));
    uriBuilder.setPath("/" + replayId + "/" + playerName + SUP_COM_REPLAY_FILE_ENDING);
    uriBuilder.addParameter("map", gameInfoBean.getMapTechnicalName());
    uriBuilder.addParameter("mod", gameInfoBean.getFeaturedMod());

    URI uri = null;
    try {
      uri = uriBuilder.build();
      runLiveReplay(uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void runLiveReplay(URI uri) throws IOException {

    if (!uri.getScheme().equals(FAF_LIFE_PROTOCOL)) {
      throw new IllegalArgumentException("Invalid protocol: " + uri.getScheme());
    }

    Map<String, String> queryParams = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(uri.getQuery());

    String mod = queryParams.get("mod");
    String mapName = queryParams.get("map");
    Integer replayId = Integer.parseInt(uri.getPath().split("/")[1]);

    if(gameService.getByUid(replayId) == null){
      runOnlineReplay(replayId);
    }

    //FIXME I don't understand this and I know this is ugly
    if(!urlFactoryHasBeenSet) {
      URLStreamHandlerFactory urlStreamHandlerFactory = protocol -> new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
          return u.openConnection();
        }
      };

      URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);

      urlFactoryHasBeenSet = true;
    }

    try {

      URL gpgReplayUrl = new URL(GPGNET_SCHEME, uri.getHost(), uri.getPort(), uri.getPath());
      gameService.runWithReplay(gpgReplayUrl, replayId);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private void runOnlineReplay(int replayId) {
    downloadReplayToTemporaryDirectory(replayId)
        .thenAccept(replayFile -> runReplayFile(replayFile))
        .exceptionally(throwable -> {
          notificationService.addNotification(new PersistentNotification(
              i18n.get("replayCouldNotBeDownloaded", replayId),
              Severity.ERROR,
              Collections.singletonList(new Action(i18n.get("report"), event -> reportingService.reportError(throwable)))
          ));

          return null;
        });
  }

  private CompletableFuture<Path> downloadReplayToTemporaryDirectory(int replayId) {
    ReplayDownloadTask task = applicationContext.getBean(ReplayDownloadTask.class);
    task.setReplayId(replayId);
    return taskService.submitTask(task);
  }

  @VisibleForTesting
  static Integer parseSupComVersion(byte[] rawReplayBytes) {
    int versionDelimiterIndex = Bytes.indexOf(rawReplayBytes, (byte) 0x00);
    return Integer.parseInt(new String(rawReplayBytes, VERSION_OFFSET, versionDelimiterIndex - VERSION_OFFSET, US_ASCII));
  }

  @VisibleForTesting
  static String guessModByFileName(String fileName) {
    String[] splitFileName = fileName.split("\\.");
    if (splitFileName.length > 2) {
      return splitFileName[splitFileName.length - 2];
    }
    return GameType.DEFAULT.getString();
  }
}
