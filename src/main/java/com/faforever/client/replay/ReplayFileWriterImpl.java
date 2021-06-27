package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.commons.io.Bytes;
import com.faforever.commons.replay.QtCompress;
import com.faforever.commons.replay.ReplayMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.io.BaseEncoding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;

@Lazy
@Component
@RequiredArgsConstructor
@Slf4j
public class ReplayFileWriterImpl implements ReplayFileWriter {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

  private final I18n i18n;
  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;

  @Override
  public void writeReplayDataToFile(ByteArrayOutputStream replayData, ReplayMetadata replayInfo) throws IOException {
    String fileName = String.format(clientProperties.getReplay().getReplayFileFormat(), replayInfo.getUid(), replayInfo.getRecorder());
    Path replayFile = preferencesService.getReplaysDirectory().resolve(fileName);
    Path temporaryReplayFile = Files.createTempFile(preferencesService.getCacheDirectory(), fileName, "fafreplay");

    log.info("Writing replay file to {} ({})", replayFile, Bytes.formatSize(replayData.size(), i18n.getUserSpecificLocale()));

    Files.createDirectories(replayFile.getParent());

    try (BufferedWriter writer = Files.newBufferedWriter(temporaryReplayFile, UTF_8, APPEND)) {
      byte[] compressedBytes = QtCompress.qCompress(replayData.toByteArray());
      String base64ReplayData = BaseEncoding.base64().encode(compressedBytes);

      StringWriter replayInfoWriter = new StringWriter();

      objectMapper.writeValue(replayInfoWriter, replayInfo);
      writer.write(replayInfoWriter.toString());
      writer.write('\n');
      writer.write(base64ReplayData);
    }

    Files.move(temporaryReplayFile, replayFile, StandardCopyOption.ATOMIC_MOVE);
  }
}
