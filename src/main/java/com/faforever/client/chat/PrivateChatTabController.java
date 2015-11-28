package com.faforever.client.chat;

import com.faforever.client.audio.AudioController;
import com.faforever.client.preferences.ChatPrefs;
import com.sun.javafx.scene.control.skin.TabPaneSkin;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.web.WebView;

import javax.annotation.Resource;

import static com.faforever.client.chat.SocialStatus.FOE;
import static javafx.scene.AccessibleAttribute.ITEM_AT_INDEX;

public class PrivateChatTabController extends AbstractChatTabController {

  private static final PseudoClass UNREAD_PSEUDO_STATE = PseudoClass.getPseudoClass("unread");

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

  @FXML
  void initialize() {
    privateChatTabRoot.selectedProperty().addListener((observable, oldValue, newValue) -> setUnread(false));
  }

  private void setUnread(boolean unread) {
    TabPane tabPane = privateChatTabRoot.getTabPane();
    if (tabPane == null) {
      return;
    }
    TabPaneSkin skin = (TabPaneSkin) tabPane.getSkin();
    int tabIndex = tabPane.getTabs().indexOf(privateChatTabRoot);
    if (tabIndex == -1) {
      // Tab has been closed
      return;
    }
    Node tab = (Node) skin.queryAccessibleAttribute(ITEM_AT_INDEX, tabIndex);
    tab.pseudoClassStateChanged(UNREAD_PSEUDO_STATE, unread);
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
      showNotificationIfNecessary(chatMessage);
      setUnread(true);
    }
  }
}
