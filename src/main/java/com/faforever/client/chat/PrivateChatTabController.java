package com.faforever.client.chat;

import com.faforever.client.audio.AudioController;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputControl;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.annotation.Autowired;

public class PrivateChatTabController extends AbstractChatTabController {

  @FXML
  Tab privateChatTabRoot;

  @FXML
  WebView messagesWebView;

  @FXML
  TextInputControl messageTextField;

  @Autowired
  AudioController audioController;

  public void setUsername(String username) {
    super.setReceiver(username);
    privateChatTabRoot.setId(username);
    privateChatTabRoot.setText(username);
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }

  @Override
  protected TextInputControl getMessageTextField() {
    return messageTextField;
  }

  @Override
  public void onChatMessage(ChatMessage chatMessage) {
    super.onChatMessage(chatMessage);

    if (!hasFocus()) {
      audioController.playPrivateMessageSound();
    }
  }

  @Override
  public Tab getRoot() {
    return privateChatTabRoot;
  }
}
