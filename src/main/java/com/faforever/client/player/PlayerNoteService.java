package com.faforever.client.player;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.preferences.PreferencesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerNoteService implements InitializingBean {

  private static final String PLAYER_NOTES_FILE_NAME = "playerNotes.json";

  private final PreferencesService preferencesService;
  private final ObjectMapper objectMapper;
  private final EventBus eventBus;

  private Path notesPath;
  private Map<Integer, String> notes;

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
    notesPath = preferencesService.getPreferencesDirectory().resolve(PLAYER_NOTES_FILE_NAME);
    if (!Files.exists(notesPath)) {
      Files.createFile(notesPath);
      notes = new HashMap<>();
      saveToFile();
    } else {
      readFromFile();
    }
  }

  public CompletableFuture<Void> addNote(PlayerBean player, String text) {
    notes.put(player.getId(), text);
    player.setNote(text);
    return saveToFile();
  }

  public CompletableFuture<Void> editNote(PlayerBean player, String text) {
    notes.replace(player.getId(), text);
    player.setNote(text);
    return saveToFile();
  }

  public CompletableFuture<Void> removeNote(PlayerBean player) {
    notes.remove(player.getId());
    player.setNote("");
    return saveToFile();
  }

  @Subscribe
  public void onPlayerOnline(PlayerOnlineEvent event) {
    PlayerBean player = event.getPlayer();
    String note = notes.get(player.getId());
    if (!StringUtils.isBlank(note)) {
      player.setNote(note);
    }
  }

  public boolean containsNote(PlayerBean player) {
    return notes.containsKey(player.getId());
  }

  private void readFromFile() throws IOException {
    notes = objectMapper.readValue(notesPath.toFile(), objectMapper.getTypeFactory()
        .constructMapType(HashMap.class, Integer.class, String.class));
  }

  private CompletableFuture<Void> saveToFile() {
    return CompletableFuture.runAsync(() -> {
      try {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(notesPath.toFile(), notes);
      } catch (IOException e) {
        throw new CompletionException(e);
      }
    }).exceptionally(throwable -> {
      if (throwable != null) {
        log.error("cannot save notes to file: ", throwable.getCause());
      }
      return null;
    });
  }
}
