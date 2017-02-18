package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.Bytes;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.io.QtCompress;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

@Lazy
@Component
public class ReplayFileWriterImpl implements ReplayFileWriter {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Gson gson;

  private final I18n i18n;
  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;

  @Inject
  public ReplayFileWriterImpl(I18n i18n, ClientProperties clientProperties, PreferencesService preferencesService) {
    this.i18n = i18n;
    this.clientProperties = clientProperties;
    this.preferencesService = preferencesService;

    gson = ReplayFiles.gson();
  }

  @Override
  public void writeReplayDataToFile(ByteArrayOutputStream replayData, LocalReplayInfo replayInfo) throws IOException {
    String fileName = String.format(clientProperties.getReplay().getReplayFileFormat(), replayInfo.getUid(), replayInfo.getRecorder());
    Path replayFile = preferencesService.getReplaysDirectory().resolve(fileName);

    logger.info("Writing replay file to {} ({})", replayFile, Bytes.formatSize(replayData.size(), i18n.getUserSpecificLocale()));

    Files.createDirectories(replayFile.getParent());

    try (BufferedWriter writer = Files.newBufferedWriter(replayFile, UTF_8, CREATE_NEW)) {
      byte[] compressedBytes = QtCompress.qCompress(replayData.toByteArray());
      String base64ReplayData = BaseEncoding.base64().encode(compressedBytes);

      gson.toJson(replayInfo, writer);
      writer.write('\n');
      writer.write(base64ReplayData);
    }
  }
}
