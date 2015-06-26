package com.faforever.client.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.awt.Desktop;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ReplayServiceImpl implements ReplayService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  ReplayFileReader replayFileReader;

  @Autowired
  NotificationService notificationService;

  @Autowired
  I18n i18n;

  @Override
  public Collection<ReplayInfoBean> getLocalReplays() throws IOException {
    Collection<ReplayInfoBean> replayInfos = new ArrayList<>();

    String replayFileGlob = environment.getProperty("replayFileGlob");

    Path replaysDirectory = preferencesService.getReplaysDirectory();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(replaysDirectory, replayFileGlob)) {
      for (Path replayFile : directoryStream) {
        ReplayInfo replayInfo = replayFileReader.readReplayFile(replayFile);
        if (replayInfo == null) {
          moveCorruptedReplayFile(replayFile);
          continue;
        }

        replayInfos.add(new ReplayInfoBean(replayInfo));
      }
    }

    return replayInfos;
  }

  private void moveCorruptedReplayFile(Path replayFile) throws IOException {
    Path corruptedReplaysDirectory = preferencesService.getCorruptedReplaysDirectory();
    Files.createDirectories(corruptedReplaysDirectory);

    Path target = corruptedReplaysDirectory.resolve(replayFile.getFileName());

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
  public Collection<ReplayInfoBean> getOnlineReplays() {
    return null;
  }
}
