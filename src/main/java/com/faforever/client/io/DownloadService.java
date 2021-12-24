package com.faforever.client.io;

import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.ResourceLocks;
import com.faforever.commons.io.ByteCopier;
import com.faforever.commons.io.ByteCountListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.xml.bind.DatatypeConverter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Lazy
@Slf4j
@RequiredArgsConstructor
public class DownloadService {
  private final PreferencesService preferencesService;

  /*
   * Download file first trying each mirror in order. If all mirrors fail then try the original URL.
   */
  public void downloadFileWithMirrors(URL url, Path targetFile, ByteCountListener progressListener, String md5sum) throws IOException, NoSuchAlgorithmException, ChecksumMismatchException {
    for (URL mirrorUrl : getMirrorsFor(url)) {
      try {
        downloadFile(mirrorUrl, targetFile, progressListener, md5sum);
        return;
      } catch (FileNotFoundException e) {
        // URL throws FileNotFoundException when it encounters HTTP errors such as 404
        log.info("Could not find file at {}", mirrorUrl);
      } catch (ChecksumMismatchException e) {
        log.warn("Checksum did not match for {}", mirrorUrl, e);
      } catch (IOException e) {
        log.warn("Could not download file at {}", mirrorUrl, e);
      }
    }

    // No mirrors available or they all failed
    downloadFile(url, targetFile, progressListener, md5sum);
  }

  /*
   * Download a file from an URL using a temporary path and copy it to targetFile if it downloaded and the checksum
   * matched.
   */
  public void downloadFile(URL url, Path targetFile, ByteCountListener progressListener, String md5sum) throws IOException, NoSuchAlgorithmException, ChecksumMismatchException {
    Path tempFile = Files.createTempFile(targetFile.getParent(), "download", null);
    log.debug("Downloading file {} to {}", url, tempFile);

    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    ResourceLocks.acquireDownloadLock();
    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
    try (InputStream inputStream = url.openStream();
         DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest);
         OutputStream outputStream = Files.newOutputStream(tempFile)) {

      ByteCopier.from(digestInputStream)
          .to(outputStream)
          .totalBytes(urlConnection.getContentLength())
          .listener(progressListener)
          .copy();

      // NOTE: It is crucial that we verify the checksum before using the file when downloading from mirrors! We don't
      // want to be running unverified executables!
      String checksum = DatatypeConverter.printHexBinary(messageDigest.digest()).toLowerCase();
      if (!Objects.equals(md5sum, checksum)) {
        throw new ChecksumMismatchException(url, checksum, md5sum);
      }

      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      ResourceLocks.freeDownloadLock();
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        log.warn("Could not delete temporary file: {}", tempFile.toAbsolutePath(), e);
      }
    }
  }

  /*
   * Get URLs to the same file on all available mirrors.
   */
  public List<URL> getMirrorsFor(URL url) {
    return preferencesService.getPreferences().getMirror().getMirrorURLs().stream()
        .map(mirror -> getMirrorURL(mirror, url))
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
  }

  public Optional<URL> getMirrorURL(URI mirror, URL url) {
      URI uri = mirror.resolve(url.getPath());
      try {
        return Optional.of(uri.toURL());
      } catch (MalformedURLException e) {
        log.warn("Failed to create URL from URI: {}", uri);
        return Optional.empty();
      }
  }
}
