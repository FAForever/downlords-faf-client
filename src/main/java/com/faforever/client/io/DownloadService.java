package com.faforever.client.io;

import com.faforever.commons.io.ByteCopier;
import com.faforever.commons.io.ByteCountListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

@Service
@Lazy
@Slf4j
@RequiredArgsConstructor
public class DownloadService {

  /*
   * Download a file from a URL using a temporary path and copy it to targetFile if it downloaded and the checksum
   * matched.
   */
  public void downloadFile(URL url, Map<String, String> requestProperties, Path targetFile, ByteCountListener progressListener, String md5sum) throws IOException, NoSuchAlgorithmException, ChecksumMismatchException {
    Path tempFile = Files.createTempFile(targetFile.getParent(), "download", null);


    URLConnection urlConnection = url.openConnection();
    requestProperties.forEach(urlConnection::setRequestProperty);

    log.info("Downloading file from `{}` to `{}`", url, tempFile);

    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
    try (InputStream inputStream = urlConnection.getInputStream();
         DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest);
         OutputStream outputStream = Files.newOutputStream(tempFile)) {

      ByteCopier.from(digestInputStream)
          .to(outputStream)
          .totalBytes(urlConnection.getContentLength())
          .listener(progressListener)
          .copy();

      // NOTE: It is crucial that we verify the checksum before using the file when downloading from mirrors! We don't
      // want to be running unverified executables!
      String checksum = HexFormat.of().formatHex(messageDigest.digest()).toLowerCase();
      if (!Objects.equals(md5sum, checksum)) {
        throw new ChecksumMismatchException(url, checksum, md5sum);
      }

      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        log.warn("Could not delete temporary file: `{}`", tempFile.toAbsolutePath(), e);
      }
    }
  }
}
