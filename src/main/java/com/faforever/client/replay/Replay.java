package com.faforever.client.replay;

import com.faforever.client.api.dto.Game;
import com.faforever.client.mod.FeaturedMod;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.faforever.client.util.TimeUtil.fromPythonTime;

public class Replay {

  private final IntegerProperty id;
  private final StringProperty title;
  private final MapProperty<String, List<String>> teams;
  private final ObjectProperty<Instant> startTime;
  private final ObjectProperty<Instant> endTime;
  private final ObjectProperty<FeaturedMod> featuredMod;
  private final StringProperty map;
  private final ObjectProperty<Path> replayFile;
  private final IntegerProperty views;
  private final ListProperty<ChatMessage> chatMessages;
  private final ListProperty<GameOption> gameOptions;

  public Replay(String title) {
    this();
    this.title.set(title);
  }

  public Replay() {
    id = new SimpleIntegerProperty();
    title = new SimpleStringProperty();
    teams = new SimpleMapProperty<>(FXCollections.observableHashMap());
    startTime = new SimpleObjectProperty<>();
    endTime = new SimpleObjectProperty<>();
    featuredMod = new SimpleObjectProperty<>();
    map = new SimpleStringProperty();
    replayFile = new SimpleObjectProperty<>();
    views = new SimpleIntegerProperty();
    chatMessages = new SimpleListProperty<>(FXCollections.observableArrayList());
    gameOptions = new SimpleListProperty<>(FXCollections.observableArrayList());
  }

  public Replay(LocalReplayInfo replayInfo, Path replayFile, FeaturedMod featuredMod) {
    this();
    id.set(replayInfo.getUid());
    title.set(StringEscapeUtils.unescapeHtml4(replayInfo.getTitle()));
    startTime.set(fromPythonTime(replayInfo.getGameTime() > 0 ? replayInfo.getGameTime() : replayInfo.getLaunchedAt()));
    endTime.set(fromPythonTime(replayInfo.getGameEnd()));
    this.featuredMod.set(featuredMod);
    map.set(replayInfo.getMapname());
    this.replayFile.set(replayFile);
    if (replayInfo.getTeams() != null) {
      teams.putAll(replayInfo.getTeams());
    }
  }

  public Replay(ServerReplayInfo replayInfo, FeaturedMod featuredMod) {
    this();
    id.setValue(replayInfo.getId());
    title.setValue(replayInfo.getName());
    this.featuredMod.setValue(featuredMod);
    map.setValue(replayInfo.getMap());
    startTime.setValue(Instant.ofEpochMilli(replayInfo.getStart() * 1000));
    endTime.setValue(Instant.ofEpochMilli(replayInfo.getEnd() * 1000));
  }

  public static Replay fromDto(Game dto) {
    Replay replay = new Replay();
    replay.setId(Integer.parseInt(dto.getId()));
    replay.setFeaturedMod(FeaturedMod.fromFeaturedMod(dto.getFeaturedMod()));
    replay.setTitle(dto.getName());
    replay.setStartTime(dto.getStartTime());
    Optional.ofNullable(dto.getEndTime()).ifPresent(replay::setEndTime);
    Optional.ofNullable(dto.getMapVersion()).ifPresent(mapVersion -> replay.setMap(mapVersion.getFilename()));
//    replay.setViews(dto.getViews());
//    replay.setTeams(dto.getTeams());
    return replay;
  }

  public Path getReplayFile() {
    return replayFile.get();
  }

  public void setReplayFile(Path replayFile) {
    this.replayFile.set(replayFile);
  }

  public ObjectProperty<Path> replayFileProperty() {
    return replayFile;
  }

  public String getTitle() {
    return title.get();
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  public StringProperty titleProperty() {
    return title;
  }

  public ObservableMap<String, List<String>> getTeams() {
    return teams.get();
  }

  public void setTeams(ObservableMap<String, List<String>> teams) {
    this.teams.set(teams);
  }

  public MapProperty<String, List<String>> teamsProperty() {
    return teams;
  }

  public int getId() {
    return id.get();
  }

  public void setId(int id) {
    this.id.set(id);
  }

  public IntegerProperty idProperty() {
    return id;
  }

  public Instant getStartTime() {
    return startTime.get();
  }

  public void setStartTime(Instant startTime) {
    this.startTime.set(startTime);
  }

  public ObjectProperty<Instant> startTimeProperty() {
    return startTime;
  }

  @Nullable
  public Instant getEndTime() {
    return endTime.get();
  }

  public void setEndTime(Instant endTime) {
    this.endTime.set(endTime);
  }

  public ObjectProperty<Instant> endTimeProperty() {
    return endTime;
  }

  public FeaturedMod getFeaturedMod() {
    return featuredMod.get();
  }

  public void setFeaturedMod(FeaturedMod featuredMod) {
    this.featuredMod.set(featuredMod);
  }

  public ObjectProperty<FeaturedMod> featuredModProperty() {
    return featuredMod;
  }

  public String getMap() {
    return map.get();
  }

  public void setMap(String map) {
    this.map.set(map);
  }

  public StringProperty mapProperty() {
    return map;
  }

  public int getViews() {
    return views.get();
  }

  public void setViews(int views) {
    this.views.set(views);
  }

  public IntegerProperty viewsProperty() {
    return views;
  }

  public ObservableList<ChatMessage> getChatMessages() {
    return chatMessages.get();
  }

  public void setChatMessages(ObservableList<ChatMessage> chatMessages) {
    this.chatMessages.set(chatMessages);
  }

  public ListProperty<ChatMessage> chatMessagesProperty() {
    return chatMessages;
  }

  public ObservableList<GameOption> getGameOptions() {
    return gameOptions.get();
  }

  public void setGameOptions(ObservableList<GameOption> gameOptions) {
    this.gameOptions.set(gameOptions);
  }

  public ListProperty<GameOption> gameOptionsProperty() {
    return gameOptions;
  }

  public static class ChatMessage {
    private final ObjectProperty<Duration> time;
    private final StringProperty sender;
    private final StringProperty message;

    public ChatMessage(Duration time, String sender, String message) {
      this.time = new SimpleObjectProperty<>(time);
      this.sender = new SimpleStringProperty(sender);
      this.message = new SimpleStringProperty(message);
    }

    public Duration getTime() {
      return time.get();
    }

    public void setTime(Duration time) {
      this.time.set(time);
    }

    public ObjectProperty<Duration> timeProperty() {
      return time;
    }

    public String getSender() {
      return sender.get();
    }

    public void setSender(String sender) {
      this.sender.set(sender);
    }

    public StringProperty senderProperty() {
      return sender;
    }

    public String getMessage() {
      return message.get();
    }

    public void setMessage(String message) {
      this.message.set(message);
    }

    public StringProperty messageProperty() {
      return message;
    }
  }

  public static class GameOption {
    private final StringProperty key;
    private final StringProperty value;

    public GameOption(String key, Object value) {
      this.key = new SimpleStringProperty(key);
      this.value = new SimpleStringProperty(String.valueOf(value));
    }

    public String getKey() {
      return key.get();
    }

    public void setKey(String key) {
      this.key.set(key);
    }

    public StringProperty keyProperty() {
      return key;
    }

    public String getValue() {
      return value.get();
    }

    public void setValue(String value) {
      this.value.set(value);
    }

    public StringProperty valueProperty() {
      return value;
    }
  }
}
