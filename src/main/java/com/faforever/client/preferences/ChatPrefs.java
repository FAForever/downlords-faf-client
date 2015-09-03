package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class ChatPrefs {

  private final DoubleProperty zoom;
  private final BooleanProperty learnedAutoComplete;
  private final BooleanProperty previewImageUrls;
  private final IntegerProperty maxMessages;
  private final ObjectProperty<Color> selfChatColor;
  private final ObjectProperty<Color> friendsChatColor;
  private final ObjectProperty<Color> foesChatColor;
  private final ObjectProperty<Color> modsChatColor;
  private final ObjectProperty<Color> ircChatColor;
  private final ObjectProperty<Color> othersChatColor;
  private final BooleanProperty prettyColors;
  private final IntegerProperty channelTabScrollPaneWidth;

  public ChatPrefs() {
    maxMessages = new SimpleIntegerProperty(500);
    zoom = new SimpleDoubleProperty(1);
    learnedAutoComplete = new SimpleBooleanProperty(false);
    previewImageUrls = new SimpleBooleanProperty(true);
    selfChatColor = new SimpleObjectProperty<>(Color.web("#FFA500"));
    friendsChatColor = new SimpleObjectProperty<>(Color.web("#16B7EB"));
    foesChatColor = new SimpleObjectProperty<>(Color.web("#FF0000"));
    modsChatColor = new SimpleObjectProperty<>(Color.web("#FFFFFF"));
    ircChatColor = new SimpleObjectProperty<>(Color.web("#808080"));
    othersChatColor = new SimpleObjectProperty<>(Color.web("#BFBEBA"));
    prettyColors = new SimpleBooleanProperty(true);
    channelTabScrollPaneWidth = new SimpleIntegerProperty(250);

  }

  public boolean getPreviewImageUrls() {
    return previewImageUrls.get();
  }

  public void setPreviewImageUrls(boolean previewImageUrls) {
    this.previewImageUrls.set(previewImageUrls);
  }

  public BooleanProperty previewImageUrlsProperty() {
    return previewImageUrls;
  }

  public Double getZoom() {
    return zoom.getValue();
  }

  public void setZoom(Double zoom) {
    this.zoom.set(zoom);
  }

  public DoubleProperty zoomProperty() {
    return zoom;
  }

  public boolean getLearnedAutoComplete() {
    return learnedAutoComplete.get();
  }

  public void setLearnedAutoComplete(boolean learnedAutoComplete) {
    this.learnedAutoComplete.set(learnedAutoComplete);
  }

  public BooleanProperty learnedAutoCompleteProperty() {
    return learnedAutoComplete;
  }

  public int getMaxMessages() {
    return maxMessages.get();
  }

  public void setMaxMessages(int maxMessages) {
    this.maxMessages.set(maxMessages);
  }

  public IntegerProperty maxMessagesProperty() {
    return maxMessages;
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


  public boolean getPrettyColors() {
    return prettyColors.get();
  }

  public BooleanProperty prettyColorsProperty() {
    return prettyColors;
  }

  public void setPrettyColors(boolean prettyColors) {
    this.prettyColors.set(prettyColors);
  }

  public void setZoom(double zoom) {
    this.zoom.set(zoom);
  }

  public int getChannelTabScrollPaneWidth() {
    return channelTabScrollPaneWidth.get();
  }

  public IntegerProperty channelTabScrollPaneWidthProperty() {
    return channelTabScrollPaneWidth;
  }

  public void setChannelTabScrollPaneWidth(int channelTabScrollPaneWidth) {
    this.channelTabScrollPaneWidth.set(channelTabScrollPaneWidth);
  }
}
