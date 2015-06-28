package com.faforever.client.chat;

import com.faforever.client.sound.SoundService;
import javafx.fxml.FXML;
import javafx.scene.control.TextInputControl;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.annotation.Autowired;

public class PrivateChatTab extends AbstractChatTab {

  @FXML
  WebView messagesWebView;

  @FXML
  TextInputControl messageTextField;

  @Autowired
  SoundService soundService;

  public PrivateChatTab(String username) {
    super(username, "private_chat_tab.fxml");

    setId(username);
    setText(username);
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
      soundService.playPrivateMessageSound();
    }
  }
}
