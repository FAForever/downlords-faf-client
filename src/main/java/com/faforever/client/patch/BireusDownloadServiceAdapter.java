package com.faforever.client.patch;

import com.faforever.commons.io.ByteCountListener;
import com.google.common.io.Resources;
import net.brutus5000.bireus.service.DownloadException;
import net.brutus5000.bireus.service.DownloadService;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class BireusDownloadServiceAdapter implements DownloadService {
  private final com.faforever.client.io.DownloadService downloadService;
  private final ByteCountListener progressListener;

  public BireusDownloadServiceAdapter(com.faforever.client.io.DownloadService downloadService, ByteCountListener progressListener) {
    this.downloadService = downloadService;
    this.progressListener = progressListener;
  }

  @Override
  public void download(URL url, Path targetFile) throws DownloadException {
    try {
      downloadService.downloadFile(url, targetFile, progressListener);
    } catch (IOException e) {
      throw new DownloadException(e, url);
    }
  }

  @Override
  public byte[] read(URL url) throws DownloadException {
    try {
      return Resources.toByteArray(url);
    } catch (IOException e) {
      throw new DownloadException(e, url);
    }
  }
}
