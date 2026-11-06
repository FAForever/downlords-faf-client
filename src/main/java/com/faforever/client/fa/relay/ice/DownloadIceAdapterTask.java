package com.faforever.client.fa.relay.ice;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import com.faforever.commons.io.ByteCopier;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class DownloadIceAdapterTask extends CompletableTask<Void> {

  private final ClientProperties clientProperties;
  private final I18n i18n;

  @Setter
  @Getter
  @VisibleForTesting
  private String version;

  @Setter
  @Getter
  @VisibleForTesting
  private Path targetFile;

  @Autowired
  public DownloadIceAdapterTask(ClientProperties clientProperties, I18n i18n) {
    super(Priority.HIGH);

    this.clientProperties = clientProperties;
    this.i18n = i18n;
  }

  @Override
  protected Void call() throws Exception {
    Objects.requireNonNull(version, "Version hasn't been set.");
    Objects.requireNonNull(targetFile, "Target file hasn't been set.");

    updateTitle(i18n.get("iceAdapter.download.title", version));

    List<String> downloadUrlFormats = clientProperties.getIceAdapter().getDownloadUrlFormats();
    if (downloadUrlFormats.isEmpty()) {
      throw new IllegalStateException("No ICE adapter download URLs configured");
    }

    Files.createDirectories(targetFile.getParent());

    IOException lastException = null;
    for (String downloadUrlFormat : downloadUrlFormats) {
      URL url = URI.create(String.format(downloadUrlFormat, version)).toURL();
      try {
        downloadTo(url);
        return null;
      } catch (IOException e) {
        log.warn("Could not download ICE adapter version `{}` from `{}`: {}", version, url, e.toString());
        if (lastException != null) {
          e.addSuppressed(lastException);
        }
        lastException = e;
      }
    }

    throw lastException;
  }

  private void downloadTo(URL url) throws IOException {
    log.info("Downloading ICE adapter from `{}`", url);
    URLConnection urlConnection = url.openConnection();
    Path tempFile = Files.createTempFile(targetFile.getParent(), "ice-adapter", null);

    try (
        InputStream inputStream = urlConnection.getInputStream();
        OutputStream outputStream = Files.newOutputStream(tempFile)
    ) {
      ByteCopier.from(inputStream)
                .to(outputStream)
                .totalBytes(urlConnection.getContentLength())
                .listener(this::updateProgress)
                .copy();

      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        log.warn("Could not delete temporary file: `{}`", tempFile, e);
      }
    }
  }
}
