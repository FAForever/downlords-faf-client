package com.faforever.client.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class FileSizeReader {
  @Async
  public CompletableFuture<Integer> getFileSize(URL url) {
    return CompletableFuture.supplyAsync(() -> {
      HttpURLConnection connection = null;
      try {
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(HttpMethod.HEAD.name());
        return connection.getContentLength();
      } catch (IOException e) {
        log.warn("Could not open connection to file download", e);
        return -1;
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
      }
    });
  }
}
