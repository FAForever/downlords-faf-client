package com.faforever.client.io;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class ZipperAndUnzipperTest {

  @Rule
  public TemporaryFolder folderToZip = new TemporaryFolder();
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule
  public TemporaryFolder targetFolder = new TemporaryFolder();

  @Test
  public void testZip() throws Exception {
    folderToZip.create();
    Path file1 = folderToZip.newFile("file1").toPath();
    folderToZip.newFile("file2");

    folderToZip.newFolder("folder1");
    folderToZip.newFile("folder1/file1");
    folderToZip.newFolder("folder1", "folder11");
    folderToZip.newFile("folder1/folder11/file1");

    folderToZip.newFolder("folder2");
    folderToZip.newFile("folder2/file1");

    folderToZip.newFolder("folder3");

    byte[] file1Contents = RandomUtils.nextBytes(1024);
    Files.write(file1, file1Contents);

    Path zipFile = targetFolder.newFile("target.zip").toPath();
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      Zipper.contentOf(folderToZip.getRoot().toPath())
          .to(zipOutputStream)
          .zip();
    }

    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
      Unzipper.from(zipInputStream)
          .to(targetFolder.getRoot().toPath())
          .unzip();
    }

    Path targetDirectory = targetFolder.getRoot().toPath();

    assertTrue(Files.exists(targetDirectory.resolve("file1")));
    assertTrue(Files.exists(targetDirectory.resolve("file2")));
    assertTrue(Files.exists(targetDirectory.resolve("folder1")));
    assertTrue(Files.exists(targetDirectory.resolve("folder1").resolve("file1")));
    assertTrue(Files.exists(targetDirectory.resolve("folder2")));
    assertTrue(Files.exists(targetDirectory.resolve("folder2").resolve("file1")));
    assertTrue(Files.exists(targetDirectory.resolve("folder3")));

    assertArrayEquals(file1Contents, Files.readAllBytes(targetDirectory.resolve("file1")));
  }
}
