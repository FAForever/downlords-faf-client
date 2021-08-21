package com.faforever.client.domain;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.List;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Value
public class ClanBean extends AbstractEntityBean<ClanBean> {
  
  StringProperty description = new SimpleStringProperty();
  ObjectProperty<PlayerBean> founder = new SimpleObjectProperty<>();
  ObjectProperty<PlayerBean> leader = new SimpleObjectProperty<>();
  StringProperty name = new SimpleStringProperty();
  @ToString.Include
  StringProperty tag = new SimpleStringProperty();
  StringProperty tagColor = new SimpleStringProperty();
  StringProperty websiteUrl = new SimpleStringProperty();
  ObservableList<PlayerBean> members = FXCollections.observableArrayList();

  public String getDescription() {
    return description.get();
  }

  public void setDescription(String description) {
    this.description.set(description);
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public PlayerBean getFounder() {
    return founder.get();
  }

  public void setFounder(PlayerBean founder) {
    this.founder.set(founder);
  }

  public ObjectProperty<PlayerBean> founderProperty() {
    return founder;
  }

  public PlayerBean getLeader() {
    return leader.get();
  }

  public void setLeader(PlayerBean leader) {
    this.leader.set(leader);
  }

  public ObjectProperty<PlayerBean> leaderProperty() {
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

  public String getWebsiteUrl() {
    return websiteUrl.get();
  }

  public void setWebsiteUrl(String websiteUrl) {
    this.websiteUrl.set(websiteUrl);
  }

  public StringProperty websiteUrlProperty() {
    return websiteUrl;
  }

  public ObservableList<PlayerBean> getMembers() {
    return members;
  }

  public void setMembers(List<PlayerBean> members) {
    if (members == null) {
      members = List.of();
    }
    this.members.setAll(members);
  }
}
