package com.faforever.client.clan;

import com.faforever.client.player.Player;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Instant;
import java.util.stream.Collectors;

public class Clan {

  private final IntegerProperty id;
  private final StringProperty description;
  private final ObjectProperty<Player> founder;
  private final ObjectProperty<Player> leader;
  private final StringProperty name;
  private final StringProperty tag;
  private final StringProperty tagColor;
  private final StringProperty websiteUrl;
  private final ListProperty<Player> members;
  private final ObjectProperty<Instant> createTime;

  public Clan() {
    id = new SimpleIntegerProperty();
    description = new SimpleStringProperty();
    founder = new SimpleObjectProperty<>();
    leader = new SimpleObjectProperty<>();
    name = new SimpleStringProperty();
    tag = new SimpleStringProperty();
    tagColor = new SimpleStringProperty();
    websiteUrl = new SimpleStringProperty();
    members = new SimpleListProperty<>(FXCollections.observableArrayList());
    createTime = new SimpleObjectProperty<>();
  }

  public static Clan fromDto(com.faforever.client.api.dto.Clan dto) {
    Clan clan = new Clan();
    clan.setId(dto.getId());
    clan.setName(dto.getName());
    clan.setDescription(dto.getDescription());
    clan.setFounder(Player.fromDto(dto.getFounder()));
    clan.setLeader(Player.fromDto(dto.getLeader()));
    clan.setCreateTime(dto.getCreateTime().toInstant());
    clan.setTag(dto.getTag());
    clan.setTagColor(dto.getTagColor());
    clan.membersProperty().setAll(dto.getMemberships().stream()
        .map(membership -> Player.fromDto(membership.getPlayer()))
        .collect(Collectors.toList()));
    return clan;
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

  public String getDescription() {
    return description.get();
  }

  public void setDescription(String description) {
    this.description.set(description);
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public Player getFounder() {
    return founder.get();
  }

  public void setFounder(Player founder) {
    this.founder.set(founder);
  }

  public ObjectProperty<Player> founderProperty() {
    return founder;
  }

  public Player getLeader() {
    return leader.get();
  }

  public void setLeader(Player leader) {
    this.leader.set(leader);
  }

  public ObjectProperty<Player> leaderProperty() {
    return leader;
  }

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  public StringProperty nameProperty() {
    return name;
  }

  public String getTag() {
    return tag.get();
  }

  public void setTag(String tag) {
    this.tag.set(tag);
  }

  public StringProperty tagProperty() {
    return tag;
  }

  public String getTagColor() {
    return tagColor.get();
  }

  public void setTagColor(String tagColor) {
    this.tagColor.set(tagColor);
  }

  public StringProperty tagColorProperty() {
    return tagColor;
  }

  public Instant getCreateTime() {
    return createTime.get();
  }

  public void setCreateTime(Instant createTime) {
    this.createTime.set(createTime);
  }

  public ObjectProperty<Instant> createTimeProperty() {
    return createTime;
  }

  public ObservableList<Player> getMembers() {
    return members.get();
  }

  public ListProperty<Player> membersProperty() {
    return members;
  }

  public String getWebsiteUrl() {
    return websiteUrl.get();
  }

  public void setWebsiteUrl(String websiteUrl) {
    this.websiteUrl.set(websiteUrl);
  }

  public StringProperty websiteUrlProperty() {
    return websiteUrl;
  }
}
