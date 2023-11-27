package com.faforever.client.preferences;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.update.ClientConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.nio.charset.StandardCharsets.US_ASCII;

@Slf4j
@Service
public class PreferencesService implements InitializingBean {

  public static final String SUPREME_COMMANDER_EXE = "SupremeCommander.exe";
  public static final String FORGED_ALLIANCE_EXE = "ForgedAlliance.exe";

  private final ObjectReader configurationReader;
  private final ClientProperties clientProperties;
  private final Preferences preferences;

  private ClientConfiguration clientConfiguration;

  public PreferencesService(ClientProperties clientProperties, Preferences preferences, ObjectMapper objectMapper) {
    this.clientProperties = clientProperties;
    this.preferences = preferences;

    configurationReader = objectMapper.readerFor(ClientConfiguration.class);
  }

  @Override
  public void afterPropertiesSet() throws IOException, InterruptedException {
    Path gamePrefs = preferences.getForgedAlliance().getPreferencesFile();
    if (Files.notExists(gamePrefs)) {
      log.info("Initializing game preferences file: `{}`", gamePrefs);
      Files.createDirectories(gamePrefs.getParent());
      InputStream gamePrefsResource = getClass().getResourceAsStream("/game.prefs");
      if (gamePrefsResource != null) {
        Files.copy(gamePrefsResource, gamePrefs);
      }
    }
  }

  public boolean isValidGamePath() {
    Path installationPath = preferences.getForgedAlliance().getInstallationPath();
    try {
      return isGamePathValidWithErrorMessage(installationPath) == null;
    } catch (IOException e) {
      throw new GameLaunchException("Could not load installation directory " + installationPath, e, "gamePath.select.error");
    } catch (NoSuchAlgorithmException e) {
      throw new GameLaunchException("Could not compute hashes of files in installation directory " + installationPath, e, "gamePath.select.error");
    }
  }

  public String isGamePathValidWithErrorMessage(Path installationPath) throws IOException, NoSuchAlgorithmException {
    boolean valid = installationPath != null && isValidGamePath(installationPath.resolve("bin"));
    if (!valid) {
      return "gamePath.select.noValidExe";
    }
    Path binPath = installationPath.resolve("bin");
    String exeHash;
    if (Files.exists(binPath.resolve(FORGED_ALLIANCE_EXE))) {
      exeHash = sha256OfFile(binPath.resolve(FORGED_ALLIANCE_EXE));
    } else {
      exeHash = sha256OfFile(binPath.resolve(SUPREME_COMMANDER_EXE));
    }
    for (String hash : clientProperties.getVanillaGameHashes()) {
      log.info("Hash of Supreme Commander.exe in selected User directory: " + exeHash);
      if (hash.equals(exeHash)) {
        return "gamePath.select.vanillaGameSelected";
      }
    }

    if (binPath.equals(preferences.getData().getBinDirectory())) {
      return "gamePath.select.fafDataSelected";
    }

    return null;
  }

  private String sha256OfFile(Path path) throws IOException, NoSuchAlgorithmException {
    byte[] buffer = new byte[4096];
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    BufferedInputStream bufferedFileStream = new BufferedInputStream(new FileInputStream(path.toFile()));
    DigestInputStream digestInputStream = new DigestInputStream(bufferedFileStream, digest);
    while (digestInputStream.read(buffer) > -1) {}
    digest = digestInputStream.getMessageDigest();
    digestInputStream.close();
    byte[] sha256 = digest.digest();
    StringBuilder sb = new StringBuilder();
    for (byte b : sha256) {
      sb.append(String.format("%02X", b));
    }
    return sb.toString().toUpperCase();
  }

  public boolean isValidGamePath(Path binPath) {
    return binPath != null && (Files.isRegularFile(binPath.resolve(FORGED_ALLIANCE_EXE)) || Files.isRegularFile(binPath.resolve(SUPREME_COMMANDER_EXE)));
  }

  public boolean isVaultBasePathInvalidForAscii() {
    Path vaultBaseDirectory = preferences.getForgedAlliance().getVaultBaseDirectory();
    return vaultBaseDirectory != null && !US_ASCII.newEncoder().canEncode(vaultBaseDirectory.toString());
  }

  private ClientConfiguration getRemotePreferences() throws IOException {
    if (clientConfiguration != null) {
      return clientConfiguration;
    }

    URL url = new URL(clientProperties.getClientConfigUrl());
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    urlConnection.setConnectTimeout((int) clientProperties.getClientConfigConnectTimeout().toMillis());

    try (Reader reader = new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8)) {
      clientConfiguration = configurationReader.readValue(reader);
      return clientConfiguration;
    }
  }


  public CompletableFuture<ClientConfiguration> getRemotePreferencesAsync() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return getRemotePreferences();
      } catch (IOException e) {
        throw new CompletionException(e);
      }
    });
  }
}
