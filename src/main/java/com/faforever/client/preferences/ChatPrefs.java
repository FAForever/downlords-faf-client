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
  private final BooleanProperty useRandomColors;
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
    useRandomColors = new SimpleBooleanProperty(false);
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

  public void setZoom(double zoom) {
    this.zoom.set(zoom);
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

  public void setSelfChatColor(Color selfChatColor) {
    this.selfChatColor.set(selfChatColor);
  }

  public ObjectProperty<Color> selfChatColorProperty() {
    return selfChatColor;
  }

  public Color getFriendsChatColor() {
    return friendsChatColor.get();
  }

  public void setFriendsChatColor(Color friendsChatColor) {
    this.friendsChatColor.set(friendsChatColor);
  }

  public ObjectProperty<Color> friendsChatColorProperty() {
    return friendsChatColor;
  }

  public Color getFoesChatColor() {
    return foesChatColor.get();
  }

  public void setFoesChatColor(Color foesChatColor) {
    this.foesChatColor.set(foesChatColor);
  }

  public ObjectProperty<Color> foesChatColorProperty() {
    return foesChatColor;
  }

  public Color getModsChatColor() {
    return modsChatColor.get();
  }

  public void setModsChatColor(Color modsChatColor) {
    this.modsChatColor.set(modsChatColor);
  }

  public ObjectProperty<Color> modsChatColorProperty() {
    return modsChatColor;
  }

  public Color getIrcChatColor() {
    return ircChatColor.get();
  }

  public void setIrcChatColor(Color ircChatColor) {
    this.ircChatColor.set(ircChatColor);
  }

  public ObjectProperty<Color> ircChatColorProperty() {
    return ircChatColor;
  }

  public boolean getUseRandomColors() {
    return useRandomColors.get();
  }

  public void setUseRandomColors(boolean useRandomColors) {
    this.useRandomColors.set(useRandomColors);
  }

  public BooleanProperty useRandomColorsProperty() {
    return useRandomColors;
  }

  public int getChannelTabScrollPaneWidth() {
    return channelTabScrollPaneWidth.get();
  }

  public void setChannelTabScrollPaneWidth(int channelTabScrollPaneWidth) {
    this.channelTabScrollPaneWidth.set(channelTabScrollPaneWidth);
  }

  public IntegerProperty channelTabScrollPaneWidthProperty() {
    return channelTabScrollPaneWidth;
  }
}
