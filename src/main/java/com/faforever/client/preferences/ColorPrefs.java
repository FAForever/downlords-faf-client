package com.faforever.client.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class ColorPrefs {

  private final ObjectProperty<Color> selfChatColor;
  private final ObjectProperty<Color> friendsChatColor;
  private final ObjectProperty<Color> foesChatColor;
  private final ObjectProperty<Color> modsChatColor;
  private final ObjectProperty<Color> ircChatColor;
  private final ObjectProperty<Color> othersChatColor;

  public ColorPrefs() {
    selfChatColor = new SimpleObjectProperty<>(Color.web("#FFA500"));
    friendsChatColor = new SimpleObjectProperty<>(Color.web("#00B7EB"));
    foesChatColor = new SimpleObjectProperty<>(Color.web("#FF0000"));
    modsChatColor = new SimpleObjectProperty<>(Color.web("#FFFFFF"));
    ircChatColor = new SimpleObjectProperty<>(Color.web("#808080"));
    othersChatColor = new SimpleObjectProperty<>(Color.web("#BFBEBA"));
  }

  public Color getSelfChatColor() {
    return selfChatColor.get();
  }

  public ObjectProperty<Color> selfChatColorProperty() {
    return selfChatColor;
  }

  public void setSelfChatColor(Color selfChatColor) {
    this.selfChatColor.set(selfChatColor);
  }

  public Color getFriendsChatColor() {
    return friendsChatColor.get();
  }

  public ObjectProperty<Color> friendsChatColorProperty() {
    return friendsChatColor;
  }

  public void setFriendsChatColor(Color friendsChatColor) {
    this.friendsChatColor.set(friendsChatColor);
  }

  public Color getFoesChatColor() {
    return foesChatColor.get();
  }

  public ObjectProperty<Color> foesChatColorProperty() {
    return foesChatColor;
  }

  public void setFoesChatColor(Color foesChatColor) {
    this.foesChatColor.set(foesChatColor);
  }

  public Color getModsChatColor() {
    return modsChatColor.get();
  }

  public ObjectProperty<Color> modsChatColorProperty() {
    return modsChatColor;
  }

  public void setModsChatColor(Color modsChatColor) {
    this.modsChatColor.set(modsChatColor);
  }

  public Color getIrcChatColor() {
    return ircChatColor.get();
  }

  public ObjectProperty<Color> ircChatColorProperty() {
    return ircChatColor;
  }

  public void setIrcChatColor(Color ircChatColor) {
    this.ircChatColor.set(ircChatColor);
  }

  public Color getOthersChatColor() {
    return othersChatColor.get();
  }

  public ObjectProperty<Color> othersChatColorProperty() {
    return othersChatColor;
  }

  public void setOthersChatColor(Color othersChatColor) {
    this.othersChatColor.set(othersChatColor);
  }
}
