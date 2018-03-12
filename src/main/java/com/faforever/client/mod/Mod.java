package com.faforever.client.mod;

import com.faforever.client.api.dto.Player;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class Mod {
  private final StringProperty id;
  private final StringProperty displayName;
  private final StringProperty author;
  private final ObjectProperty<OffsetDateTime> createTime;
  private final ObjectProperty<OffsetDateTime> updateTime;
  private final ObjectProperty<Player> uploader;
  private final ObservableList<ModVersion> versions;
  private final ObjectProperty<ModVersion> latestVersion;

  public Mod() {
    latestVersion = new SimpleObjectProperty<>();
    id = new SimpleStringProperty();
    author = new SimpleStringProperty();
    displayName = new SimpleStringProperty();
    createTime = new SimpleObjectProperty<>();
    updateTime = new SimpleObjectProperty<>();
    uploader = new SimpleObjectProperty<>();
    versions = FXCollections.observableArrayList();
  }

  public static Mod fromDto(com.faforever.client.api.dto.Mod dto) {
    Mod mod = new Mod();
    mod.setId(dto.getId());
    mod.setDisplayName(dto.getDisplayName());
    mod.setAuthor(dto.getAuthor());
    mod.setCreateTime(dto.getCreateTime());
    mod.setUpdateTime(dto.getUpdateTime());
    if (dto.getUploader() != null) {
      mod.setUploader(dto.getUploader());
    }
    mod.addVersions(dto.getVersions().stream().map(modVersion -> ModVersion.fromDto(modVersion, mod)).collect(Collectors.toList()));
    mod.setLatestVersion(ModVersion.fromDto(dto.getLatestVersion(), mod));
    return mod;
  }

  public String getId() {
    return id.get();
  }

  public StringProperty idProperty() {
    return id;
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public String getDisplayName() {
    return displayName.get();
  }

  public StringProperty displayNameProperty() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName.set(displayName);
  }

  public String getAuthor() {
    return author.get();
  }

  public void setAuthor(String author) {
    this.author.set(author);
  }

  public StringProperty authorProperty() {
    return author;
  }

  public OffsetDateTime getCreateTime() {
    return createTime.get();
  }

  public void setCreateTime(OffsetDateTime createTime) {
    this.createTime.set(createTime);
  }

  public ObjectProperty<OffsetDateTime> createTimeProperty() {
    return createTime;
  }

  public OffsetDateTime getUpdateTime() {
    return updateTime.get();
  }

  public void setUpdateTime(OffsetDateTime updateTime) {
    this.updateTime.set(updateTime);
  }

  public ObjectProperty<OffsetDateTime> updateTimeProperty() {
    return updateTime;
  }

  public Player getUploader() {
    return uploader.get();
  }

  public void setUploader(Player uploader) {
    this.uploader.set(uploader);
  }

  public ObjectProperty<Player> uploaderProperty() {
    return uploader;
  }

  public ObservableList<ModVersion> getVersions() {
    return versions;
  }

  public ModVersion getLatestVersion() {
    return latestVersion.get();
  }

  public void setLatestVersion(ModVersion latestVersion) {
    this.latestVersion.set(latestVersion);
  }

  public ObjectProperty<ModVersion> latestVersionProperty() {
    return latestVersion;
  }

  public void addVersions(List<ModVersion> versions) {
    this.versions.addAll(versions);
  }
  
}
