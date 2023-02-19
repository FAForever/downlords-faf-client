package com.faforever.client.io;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DownloadServiceTest extends ServiceTest {
  private static final ClassPathResource SAMPLE_FILE = new ClassPathResource("/io/sample-file.txt");
  private static final String SAMPLE_FILE_CHECKSUM = "b53227da4280f0e18270f21dd77c91d0";



  @InjectMocks
  private DownloadService instance;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
  }

  @Test
  public void testDownloadFile() throws Exception {
    Path temp = Files.createTempFile("download", ".dat");
    instance.downloadFile(SAMPLE_FILE.getURL(), Map.of(), temp, (processed, total) -> {}, SAMPLE_FILE_CHECKSUM);

    byte[] data = Files.readAllBytes(temp);
    assertArrayEquals(data, "Some content".getBytes());
  }

  @Test
  public void testDownloadFileBadChecksum() throws Exception {
    Path temp = Files.createTempFile("download", ".dat");
    assertThrows(ChecksumMismatchException.class, () -> instance.downloadFile(SAMPLE_FILE.getURL(), Map.of(), temp, (processed, total) -> {
    }, "00000000000000000000000000000000"));
  }
}
