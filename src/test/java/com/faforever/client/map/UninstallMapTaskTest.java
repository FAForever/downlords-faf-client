package com.faforever.client.map;

import com.faforever.client.io.ByteCopier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class UninstallMapTaskTest {

  private static final ClassPathResource THETA_PASSAGE = new ClassPathResource("/maps/theta_passage_5.v0001.zip");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder mapsDirectory = new TemporaryFolder();

  @Mock
  private MapService mapService;

  private com.faforever.client.map.UninstallMapTask instance;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new com.faforever.client.map.UninstallMapTask();
    instance.mapService = mapService;
  }

  @Test
  public void testCallWithoutMapThrowsException() throws Exception {
    expectedException.expectMessage("map");
    expectedException.expect(NullPointerException.class);

    instance.call();
  }

  @Test
  public void testCall() throws Exception {
    copyMap("theta_passage_5.v0001", THETA_PASSAGE);

    MapBean map = MapBeanBuilder.create().uid("b2cde810-15d0-4bfa-af6a-ec2d6ecd561b").get();

    Path mapPath = mapsDirectory.getRoot().toPath().resolve("theta_passage_5.v0001");
    when(mapService.getPathForMap(map)).thenReturn(mapPath);

    instance.setMap(map);
    instance.call();

    assertThat(Files.exists(mapPath), is(false));
  }

  private void copyMap(String directoryName, ClassPathResource classPathResource) throws IOException {
    Path targetDir = mapsDirectory.getRoot().toPath().resolve(directoryName);
    Files.createDirectories(targetDir);

    try (InputStream inputStream = classPathResource.getInputStream();
         OutputStream outputStream = Files.newOutputStream(targetDir.resolve("map_info.lua"))) {
      ByteCopier.from(inputStream)
          .to(outputStream)
          .copy();
    }
  }
}
