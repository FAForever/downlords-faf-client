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
  private static final String SOUNDS_FOLDER_NAME = "sounds";

  /**
   * For coop and tutorials the video files(contained in 'movies' folder of zips) need to be extracted from zip and
   * unpacked to 'movies' folder in FAF data directory
   *
   * @param filePath the zip file to extract the movies directory from
   * @param fafDataDirectory the path to the FAF data directory
   * @throws IOException
   */
  public static void extractMoviesAndSoundsIfPresent(Path filePath, Path fafDataDirectory) throws IOException {
    try (ZipFile downloadedFile = new ZipFile(filePath.toFile())) {
      extractFilesInEntry(fafDataDirectory, downloadedFile, MOVIES_FOLDER_NAME);
      extractFilesInEntry(fafDataDirectory, downloadedFile, SOUNDS_FOLDER_NAME);
    } catch (ZipException e) {
      log.info("File was not zip file: {}", filePath);
    }
  }

  private static void extractFilesInEntry(Path fafDataDirectory, ZipFile downloadedFile, String topEntryName) throws IOException {
    ZipEntry movieEntry = downloadedFile.getEntry(topEntryName);
    if (movieEntry != null && movieEntry.isDirectory()) {
      Enumeration<? extends ZipEntry> entries = downloadedFile.entries();
      ZipEntry nextEntry = entries.nextElement();
      for (; entries.hasMoreElements(); nextEntry = entries.nextElement()) {
        String entryName = nextEntry.getName();
        if (!entryName.startsWith(topEntryName)) {
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
  }
}
