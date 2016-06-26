package com.faforever.client.chat;

import com.faforever.client.audio.AudioController;
import com.faforever.client.preferences.ChatPrefs;
import com.google.common.annotations.VisibleForTesting;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputControl;
import javafx.scene.web.WebView;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Instant;

import static com.faforever.client.chat.SocialStatus.FOE;

public class PrivateChatTabController extends AbstractChatTabController {

  @FXML
  Tab privateChatTabRoot;
  @FXML
  WebView messagesWebView;
  @FXML
  TextInputControl messageTextField;

  @Resource
  AudioController audioController;
  @Resource
  ChatService chatService;
  private boolean userOffline;

  public boolean isUserOffline() {
    return userOffline;
  }

  @Override
  public Tab getRoot() {
    return privateChatTabRoot;
  }

  @Override
  public void setReceiver(String username) {
    super.setReceiver(username);
    privateChatTabRoot.setId(username);
    privateChatTabRoot.setText(username);
  }

  @PostConstruct
  @Override
  void postConstruct() {
    super.postConstruct();
    userOffline = false;
    chatService.addChatUsersByNameListener(change -> {
      if (change.wasRemoved()) {
        onPlayerDisconnected(change.getKey(), change.getValueRemoved());
      }
      if (change.wasAdded()) {
        onPlayerConnected(change.getKey(), change.getValueRemoved());
      }

    });
  }

  @Override
  protected TextInputControl getMessageTextField() {
    return messageTextField;
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }

  @Override
  public void onChatMessage(ChatMessage chatMessage) {
    PlayerInfoBean playerInfoBean = playerService.getPlayerForUsername(chatMessage.getUsername());
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    if (playerInfoBean != null && playerInfoBean.getSocialStatus() == FOE && chatPrefs.getHideFoeMessages()) {
      return;
    }

    super.onChatMessage(chatMessage);

    if (!hasFocus()) {
      audioController.playPrivateMessageSound();
      showNotificationIfNecessary(chatMessage);
      setUnread(true);
      incrementUnreadMessagesCount(1);
    }
  }

  @VisibleForTesting
  void onPlayerDisconnected(String userName, ChatUser userItem) {
    if (userName.equals(getReceiver())) {
      userOffline = true;
      Platform.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerLeft", userName), true)));
    }
  }

  @VisibleForTesting
  void onPlayerConnected(String userName, ChatUser userItem) {
    if (userOffline && userName.equals(getReceiver())) {
      userOffline = false;
      Platform.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerReconnect", userName), true)));
    }
  }
}
