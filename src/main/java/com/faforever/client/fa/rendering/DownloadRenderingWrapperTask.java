package com.faforever.client.fa.rendering;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.RenderingBackend;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.faforever.commons.io.ByteCopier;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class DownloadRenderingWrapperTask extends CompletableTask<Void> {

  private static final int CONNECT_TIMEOUT_MS = 30_000;
  private static final int READ_TIMEOUT_MS = 300_000;

  private final I18n i18n;

  @Setter
  private RenderingBackend backend;
  @Setter
  private URL downloadUrl;
  @Setter
  private String version;
  @Setter
  private Path wrapperDirectory;

  public DownloadRenderingWrapperTask(I18n i18n) {
    super(Priority.HIGH);
    this.i18n = i18n;
  }

  @Override
  protected Void call() throws Exception {
    updateTitle(i18n.get("rendering.download.title", backend.name()));

    Path wrapperDir = wrapperDirectory;
    Files.createDirectories(wrapperDir);

    Path tempFile = Files.createTempFile(wrapperDir, "rendering-wrapper", null);

    try {
      downloadArchive(tempFile);
      extractDll(tempFile, wrapperDir);
      Files.writeString(wrapperDir.resolve("version.txt"), version);
      log.info("Successfully installed {} rendering wrapper version {}", backend, version);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        log.warn("Could not delete temporary file: {}", tempFile.toAbsolutePath(), e);
      }
    }

    return null;
  }

  private void downloadArchive(Path tempFile) throws IOException {
    URLConnection urlConnection = downloadUrl.openConnection();
    urlConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
    urlConnection.setReadTimeout(READ_TIMEOUT_MS);

    ResourceLocks.acquireDownloadLock();
    try (InputStream inputStream = urlConnection.getInputStream();
         OutputStream outputStream = Files.newOutputStream(tempFile)) {
      ByteCopier.from(inputStream)
          .to(outputStream)
          .totalBytes(urlConnection.getContentLengthLong())
          .listener(this::updateProgress)
          .copy();
    } finally {
      ResourceLocks.freeDownloadLock();
    }
  }

  private void extractDll(Path archiveFile, Path wrapperDir) throws IOException {
    String format = backend.getArchiveFormat();
    String dllPath = backend.getDllPathInArchive();

    if ("tar.gz".equals(format)) {
      extractFromTarGz(archiveFile, wrapperDir, dllPath);
    } else if ("zip".equals(format)) {
      extractFromZip(archiveFile, wrapperDir, dllPath);
    } else {
      throw new IllegalStateException("Unsupported archive format: " + format);
    }
  }

  private static boolean matchesEntryPath(String entryName, String dllPath) {
    String normalized = entryName.replace('\\', '/');
    String normalizedDll = dllPath.replace('\\', '/');
    return normalized.equals(normalizedDll) || normalized.endsWith("/" + normalizedDll);
  }

  private void extractFromTarGz(Path archiveFile, Path wrapperDir, String dllPath) throws IOException {
    try (GZIPInputStream gzIn = new GZIPInputStream(Files.newInputStream(archiveFile));
         TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn)) {
      TarArchiveEntry entry;
      while ((entry = tarIn.getNextEntry()) != null) {
        if (!entry.isDirectory() && matchesEntryPath(entry.getName(), dllPath)) {
          Path targetDll = wrapperDir.resolve("d3d9.dll");
          Files.copy(tarIn, targetDll, StandardCopyOption.REPLACE_EXISTING);
          log.debug("Extracted {} from archive to {}", entry.getName(), targetDll);
          return;
        }
      }
    }
    throw new IOException("DLL not found in archive: " + dllPath);
  }

  private void extractFromZip(Path archiveFile, Path wrapperDir, String dllPath) throws IOException {
    try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(archiveFile))) {
      ZipEntry entry;
      while ((entry = zipIn.getNextEntry()) != null) {
        if (!entry.isDirectory() && matchesEntryPath(entry.getName(), dllPath)) {
          Path targetDll = wrapperDir.resolve("d3d9.dll");
          Files.copy(zipIn, targetDll, StandardCopyOption.REPLACE_EXISTING);
          log.debug("Extracted {} from archive to {}", entry.getName(), targetDll);
          return;
        }
      }
    }
    throw new IOException("DLL not found in archive: " + dllPath);
  }
}
