package com.faforever.client.chat.test;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.AbstractChatTabController;
import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.ChatUserService;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChannelTabControllerVersion2 extends AbstractChatTabController {

  public Tab root;
  public SplitPane splitPane;
  public HBox searchFieldContainer;
  public Button closeSearchFieldButton;
  public TextField searchField;
  public WebView messagesWebView;
  public TextField messageTextField;
  public VBox topicPane;
  public TextFlow topicText;
  public ToggleButton toggleSidePaneButton;
  public Node chatUserList;
  public ChatUserListController chatUserListController;

  private String channelName;

  public ChannelTabControllerVersion2(WebViewConfigurer webViewConfigurer, UserService userService, ChatService chatService, PreferencesService preferencesService, PlayerService playerService, AudioService audioService, TimeService timeService, I18n i18n, ImageUploadService imageUploadService, NotificationService notificationService, ReportingService reportingService, UiService uiService, EventBus eventBus, CountryFlagService countryFlagService, ChatUserService chatUserService, EmoticonService emoticonService) {
    super(webViewConfigurer, userService, chatService, preferencesService, playerService, audioService, timeService, i18n, imageUploadService, notificationService, reportingService, uiService, eventBus, countryFlagService, chatUserService, emoticonService);
  }

  public void setChatChannel(ChatChannel chatChannel, BooleanBinding chatTabSelectedProperty) {
    this.channelName = chatChannel.getName();
    chatUserListController.setChatChannel(chatChannel, getRoot(), chatTabSelectedProperty);
    chatUserListController.getAutoCompletionHelper().bindTo(messageTextField());

    updateTabProperties();
    setReceiver(chatChannel.getName());
  }

  private void updateTabProperties() {
    root.setId(channelName);
    root.setText(channelName);
    getRoot().setOnCloseRequest(event -> {
      chatService.leaveChannel(channelName);
      chatUserListController.onTabClosed();
    });
  }



  @Override
  public Tab getRoot() {
    return root;
  }

  @Override
  protected TextInputControl messageTextField() {
    return messageTextField;
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }
}
