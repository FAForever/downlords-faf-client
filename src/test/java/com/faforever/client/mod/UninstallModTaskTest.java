package com.faforever.client.mod;

import com.faforever.client.io.ByteCopier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UninstallModTaskTest {

  private static final ClassPathResource BLACKOPS_SUPPORT_MOD_INFO = new ClassPathResource("/mods/blackops_support_mod_info.lua");
  private static final ClassPathResource ECO_MANAGER_MOD_INFO = new ClassPathResource("/mods/eco_manager_mod_info.lua");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder modsDirectory = new TemporaryFolder();

  @Mock
  private ModService modService;

  private UninstallModTask instance;

  @Before
  public void setUp() throws Exception {
    instance = new UninstallModTask();
    instance.modService = modService;
  }

  @Test
  public void testCallWithoutModThrowsException() throws Exception {
    expectedException.expectMessage("mod");
    expectedException.expect(NullPointerException.class);

    instance.call();
  }

  @Test
  public void testCall() throws Exception {
    copyMod("blackOpsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    copyMod("ecoManager", ECO_MANAGER_MOD_INFO);

    Mod mod = ModInfoBeanBuilder.create().uid("b2cde810-15d0-4bfa-af66-ec2d6ecd561b").get();

    Path ecoManagerPath = modsDirectory.getRoot().toPath().resolve("ecoManager");
    when(modService.getPathForMod(mod)).thenReturn(ecoManagerPath);

    instance.setMod(mod);
    instance.call();

    assertThat(Files.exists(modsDirectory.getRoot().toPath().resolve("blackOpsSupport")), is(true));
    assertThat(Files.exists(ecoManagerPath), is(false));
  }

  private void copyMod(String directoryName, ClassPathResource classPathResource) throws IOException {
    Path targetDir = modsDirectory.getRoot().toPath().resolve(directoryName);
    Files.createDirectories(targetDir);

    try (InputStream inputStream = classPathResource.getInputStream();
         OutputStream outputStream = Files.newOutputStream(targetDir.resolve("mod_info.lua"))) {
      ByteCopier.from(inputStream)
          .to(outputStream)
          .copy();
    }
  }
}
