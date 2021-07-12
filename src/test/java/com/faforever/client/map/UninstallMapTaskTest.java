package com.faforever.client.map;

import com.faforever.client.test.ServiceTest;
import com.faforever.commons.io.ByteCopier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class UninstallMapTaskTest extends ServiceTest {

  private static final ClassPathResource THETA_PASSAGE = new ClassPathResource("/maps/theta_passage_5.v0001.zip");

  @TempDir
  public Path mapsDirectory;

  @Mock
  private MapService mapService;

  private com.faforever.client.map.UninstallMapTask instance;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new com.faforever.client.map.UninstallMapTask(mapService);
  }

  @Test
  public void testCallWithoutMapThrowsException() throws Exception {
    assertEquals("map has not been set", assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Test
  public void testCall() throws Exception {
    copyMap("theta_passage_5.v0001", THETA_PASSAGE);

    MapBean map = MapBeanBuilder.create().uid("b2cde810-15d0-4bfa-af6a-ec2d6ecd561b").get();

    Path mapPath = mapsDirectory.resolve("theta_passage_5.v0001");
    when(mapService.getPathForMap(map)).thenReturn(mapPath);

    instance.setMap(map);
    instance.call();

    assertThat(Files.exists(mapPath), is(false));
  }

  private void copyMap(String directoryName, ClassPathResource classPathResource) throws IOException {
    Path targetDir = Files.createDirectories(mapsDirectory.resolve(directoryName));

    try (InputStream inputStream = classPathResource.getInputStream();
         OutputStream outputStream = Files.newOutputStream(targetDir.resolve("map_info.lua"))) {
      ByteCopier.from(inputStream)
          .to(outputStream)
          .copy();
    }
  }
}
