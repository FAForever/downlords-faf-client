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

    URL url = URI.create(String.format(clientProperties.getIceAdapter().getDownloadUrlFormat(), version)).toURL();

    URLConnection urlConnection = url.openConnection();

    Files.createDirectories(targetFile.getParent());
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
    }

    try {
      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        log.warn("Could not delete temporary file: `{}`", tempFile, e);
      }
    }

    return null;
  }
}
