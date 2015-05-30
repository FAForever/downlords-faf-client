package com.faforever.client.chat;

import javafx.fxml.FXML;
import javafx.scene.control.TextInputControl;
import javafx.scene.web.WebView;

public class PrivateChatTab extends AbstractChatTab {

  @FXML
  WebView messagesWebView;

  @FXML
  TextInputControl messageTextField;

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
}
