package com.faforever.client.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

@Slf4j
public class UpdaterUtil {
  private static final String MOVIES_FOLDER_NAME = "movies";

  /**
   * For coop and tutorials the video files(contained in 'movies' folder of zips) need to be extracted from zip and
   * unpacked to 'movies' folder in FAF data directory
   *
   * @param filePath the zip file to extract the movies directory from
   * @param fafDataDirectory the path to the FAF data directory
   */
  public static void extractMoviesIfPresent(Path filePath, Path fafDataDirectory) throws IOException {
    try {
      ZipFile downloadedFile = new ZipFile(filePath.toFile());
      ZipEntry movieEntry = downloadedFile.getEntry(MOVIES_FOLDER_NAME);
      if (movieEntry != null && movieEntry.isDirectory()) {
        Enumeration<? extends ZipEntry> entries = downloadedFile.entries();
        ZipEntry nextEntry = entries.nextElement();
        for (; entries.hasMoreElements(); nextEntry = entries.nextElement()) {
          String entryName = nextEntry.getName();
          if (!entryName.startsWith(MOVIES_FOLDER_NAME)) {
            continue;
          }
          if (nextEntry.isDirectory()) {
            Files.createDirectories(fafDataDirectory.resolve(entryName));
          } else {
            InputStream inputStream = downloadedFile.getInputStream(nextEntry);
            Files.copy(inputStream, fafDataDirectory.resolve(entryName), StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    } catch (ZipException e) {
      log.debug("File was not zip file", e);
    }
  }
}
