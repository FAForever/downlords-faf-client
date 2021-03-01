package com.faforever.client.replay;

import com.faforever.commons.replay.QtCompress;
import com.faforever.commons.replay.ReplayData;
import com.faforever.commons.replay.ReplayDataParser;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

@Lazy
@Component
@Slf4j
public class ReplayFileReaderImpl implements ReplayFileReader {

  private final Gson gson;

  public ReplayFileReaderImpl() {
    gson = ReplayFiles.gson();
  }

  @Override
  @SneakyThrows
  public LocalReplayInfo parseMetaData(Path replayPath) {
    log.debug("Parsing metadata of replay file: {}", replayPath);

    byte[] replayData = Files.readAllBytes(replayPath);

    String header = new String(Arrays.copyOf(replayData, findReplayHeaderEnd(replayData)), StandardCharsets.UTF_8);
    return gson.fromJson(header, LocalReplayInfo.class);
  }

  private int findReplayHeaderEnd(byte[] replayData) {
    int headerEnd;
    for (headerEnd = 0; headerEnd < replayData.length; headerEnd++) {
      if (replayData[headerEnd] == '\n') {
        return headerEnd;
      }
    }

    throw new IllegalArgumentException("Missing separator between replay header and body");
  }

  @Override
  @SneakyThrows
  public byte[] readRawReplayData(Path replayFile) {
    return readRawReplayData(replayFile, null);
  }

  @SneakyThrows
  public byte[] readRawReplayData(Path replayPath, @Nullable LocalReplayInfo localReplayInfo) {
    log.debug("Reading replay file: {}", replayPath);

    final LocalReplayInfo metadata;
    if (localReplayInfo == null) {
      metadata = parseMetaData(replayPath);
    } else {
      metadata = localReplayInfo;
    }

    byte[] replayData = Files.readAllBytes(replayPath);
    int replayHeaderEnd = findReplayHeaderEnd(replayData);

    return decompress(Arrays.copyOfRange(replayData, replayHeaderEnd + 1, replayData.length), metadata);
  }

  @SneakyThrows
  public byte[] decompress(byte[] data, @NotNull LocalReplayInfo metadata) {
    CompressionType compressionType = Objects.requireNonNullElse(metadata.getCompression(), CompressionType.QTCOMPRESS);

    return switch (compressionType) {
      case QTCOMPRESS -> QtCompress.qUncompress(BaseEncoding.base64().decode(new String(data)));
      case ZSTD -> {
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(data);
        CompressorInputStream compressorInputStream = new CompressorStreamFactory()
            .createCompressorInputStream(arrayInputStream);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(compressorInputStream, out);
        yield out.toByteArray();
      }
      case UNKNOWN -> throw new IOException("Unknown replay format in replay file");
    };
  }

  @Override
  public ReplayData parseReplay(Path path) {
    return new ReplayDataParser(path).parse();
  }
}
