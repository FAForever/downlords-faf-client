package com.faforever.client.chat;

import com.faforever.client.audio.AudioController;
import com.faforever.client.preferences.ChatPrefs;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputControl;
import javafx.scene.web.WebView;

import javax.annotation.Resource;

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

  public void setUsername(String username) {
    super.setReceiver(username);
    privateChatTabRoot.setId(username);
    privateChatTabRoot.setText(username);
  }

  @Override
  public Tab getRoot() {
    return privateChatTabRoot;
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

    if (playerInfoBean.getSocialStatus() == FOE && chatPrefs.getHideFoeMessages()) {
      return;
    }

    super.onChatMessage(chatMessage);

    if (!hasFocus()) {
      audioController.playPrivateMessageSound();
    }
  }
}
