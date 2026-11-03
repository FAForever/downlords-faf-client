package com.faforever.client.fa;

import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.commons.io.Unzipper;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import static com.google.common.io.Files.asByteSource;

@Service
@Lazy
@RequiredArgsConstructor
@Slf4j
public class FafReReleaseService {

  private static final String VERSION = "v1.1.2";
  private static final URI RELEASE_URL = URI.create("https://github.com/Draiget/faf-re/releases/download/"
      + VERSION + "/faf-engine-" + VERSION + "-x64.zip");
  private static final String SHA256 = "c4ada9994e50c5419799b9bdafc84bf02c6eea227c14d1cfed72ecaa2de07c50";
  private static final String EXECUTABLE = "main_x64.exe";

  private static final Map<String, String> DIRECTX_FILES = Map.of(
      "d3dx9_43.dll", "Jun2010_d3dx9_43_x64.cab",
      "d3dx9_31.dll", "OCT2006_d3dx9_31_x64.cab",
      "d3dx11_43.dll", "Jun2010_d3dx11_43_x64.cab",
      "D3DCompiler_43.dll", "Jun2010_D3DCompiler_43_x64.cab",
      "X3DAudio1_7.dll", "Feb2010_X3DAudio_x64.cab",
      "xactengine2_9.dll", "AUG2007_XACT_x64.cab"
  );

  private final DataPrefs dataPrefs;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  public synchronized Path getExecutable() {
    Path releaseDirectory = dataPrefs.getBaseDataDirectory().resolve("faf-re").resolve(VERSION);
    Path executable = releaseDirectory.resolve(EXECUTABLE);
    if (Files.isRegularFile(executable)) {
      return executable;
    }

    try {
      install(releaseDirectory);
      return executable;
    } catch (IOException exception) {
      throw new GameLaunchException("Could not install faf-re " + VERSION, exception,
          "game.start.fafReInstallFailed", VERSION);
    }
  }

  private void install(Path releaseDirectory) throws IOException {
    Path parent = Files.createDirectories(releaseDirectory.getParent());
    Path staging = Files.createTempDirectory(parent, "download-");
    try {
      log.info("Downloading faf-re {}", VERSION);
      Path archive = staging.resolve("engine.zip");
      URLConnection connection = RELEASE_URL.toURL().openConnection();
      connection.setConnectTimeout(15_000);
      connection.setReadTimeout(30_000);
      try (InputStream input = connection.getInputStream()) {
        Files.copy(input, archive);
      }
      String digest = asByteSource(archive.toFile()).hash(Hashing.sha256()).toString();
      if (!SHA256.equals(digest)) {
        throw new IOException("SHA-256 mismatch for " + RELEASE_URL);
      }

      Path extracted = Files.createDirectory(staging.resolve("engine"));
      try (InputStream input = Files.newInputStream(archive)) {
        Unzipper.from(input).to(extracted).zipBombByteCountThreshold(256_000_000).unzip();
      }
      if (!Files.isRegularFile(extracted.resolve(EXECUTABLE))) {
        throw new IOException("Missing " + EXECUTABLE + " in " + RELEASE_URL);
      }

      stageDirectX(extracted);

      try {
        Files.move(extracted, releaseDirectory, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException exception) {
        if (!Files.isRegularFile(releaseDirectory.resolve(EXECUTABLE))) {
          throw exception;
        }
      }
    } finally {
      try {
        FileSystemUtils.deleteRecursively(staging);
      } catch (IOException exception) {
        log.warn("Could not remove temporary faf-re download {}", staging, exception);
      }
    }
  }

  private void stageDirectX(Path extracted) throws IOException {
    Path systemDirectory = Path.of(System.getenv("SystemRoot"), "System32");
    Path gameDirectory = forgedAlliancePrefs.getInstallationPath();

    for (var entry : DIRECTX_FILES.entrySet()) {
      String dll = entry.getKey();
      if (Files.isRegularFile(systemDirectory.resolve(dll)) || Files.isRegularFile(extracted.resolve(dll))) {
        continue;
      }

      String cabinet = entry.getValue();
      if (gameDirectory == null) {
        throw new GameLaunchException("Missing Forged Alliance installation path", null,
            "game.start.fafReDirectXMissing");
      }
      Path source = gameDirectory.resolve(Path.of("_CommonRedist", "DirectX", "Jun2010", cabinet));
      if (!Files.isRegularFile(source)) {
        source = gameDirectory.resolve(Path.of("DirectX", cabinet));
      }
      if (!Files.isRegularFile(source)) {
        throw new GameLaunchException("Missing DirectX runtime library " + dll, null,
            "game.start.fafReDirectXMissing");
      }

      Process expand = new ProcessBuilder(systemDirectory.resolve("expand.exe").toString(), "-F:" + dll,
          source.toString(), extracted.toString())
          .redirectErrorStream(true)
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .start();
      try {
        if (expand.waitFor() != 0 || !Files.isRegularFile(extracted.resolve(dll))) {
          throw new IOException("Could not extract " + dll + " from " + source);
        }
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while extracting " + dll, exception);
      } finally {
        expand.destroy();
      }
    }
  }
}
