package com.faforever.client.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class FileSizeReader {

  public CompletableFuture<Integer> getFileSize(URL url) {
    return CompletableFuture.supplyAsync(() -> {
      HttpURLConnection connection = null;
      try {
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(HttpMethod.GET.name());
        return connection.getContentLength();
      } catch (IOException e) {
        log.error("Could not open connection to file download", e);
        return -1;
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
      }
    });
  }
}
