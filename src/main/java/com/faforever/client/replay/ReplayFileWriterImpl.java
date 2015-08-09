package com.faforever.client.replay;

import com.faforever.client.legacy.io.QtCompress;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

public class ReplayFileWriterImpl implements ReplayFileWriter {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Gson gson;
  @Autowired
  Environment environment;
  @Autowired
  PreferencesService preferencesService;

  public ReplayFileWriterImpl() {
    gson = ReplayFiles.gson();
  }

  @Override
  public void writeReplayDataToFile(ByteArrayOutputStream replayData, LocalReplayInfo replayInfo) throws IOException {
    String fileName = String.format(environment.getProperty("replayFileFormat"), replayInfo.uid, replayInfo.recorder);
    Path replayFile = preferencesService.getReplaysDirectory().resolve(fileName);

    logger.info("Writing replay file to {} ({} bytes)", replayFile, replayData.size());

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
