package com.faforever.client.chat;

import com.faforever.client.fx.TabController;
import com.faforever.client.ui.StageHolder;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static javafx.scene.AccessibleAttribute.ITEM_AT_INDEX;

/**
 * A chat tab displays messages in a {@link WebView}. The WebView is used since text on a JavaFX canvas isn't
 * selectable, but text within a WebView is. This comes with some ugly implications; some logic has to be performed in
 * interaction with JavaScript, like when the user clicks a link.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractChatTabController extends TabController {

  private static final PseudoClass UNREAD_PSEUDO_STATE = PseudoClass.getPseudoClass("unread");

  protected final ChatService chatService;

  protected final ObjectProperty<ChatChannel> chatChannel = new SimpleObjectProperty<>();
  protected final ObservableValue<String> channelName = chatChannel.map(ChatChannel::getName);
  private final ObservableValue<Number> unreadMessagesCount = chatChannel.flatMap(
      ChatChannel::numUnreadMessagesProperty).orElse(0);

  public Node chatMessagesView;
  public ChatMessageViewController chatMessagesViewController;

  @Override
  protected void onInitialize() {
    chatMessagesViewController.chatChannelProperty().bind(chatChannel.when(attached));

    chatChannel.when(attached).subscribe(((oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.openProperty().unbind();
        oldValue.setOpen(false);
      }

      if (newValue != null) {
        newValue.openProperty().bind(showing.and(StageHolder.getStage().focusedProperty()).when(attached));
      }
    }));

    unreadMessagesCount.when(attached).subscribe(newValue -> setUnread(newValue.intValue() > 0));

    getRoot().setOnClosed(this::onClosed);
  }

  @Override
  public void onDetached() {
    ChatChannel channel = chatChannel.get();
    if (channel != null) {
      channel.openProperty().unbind();
      channel.setOpen(false);
    }
  }

  protected void setUnread(boolean unread) {
    TabPane tabPane = getRoot().getTabPane();
    if (tabPane == null) {
      return;
    }
    TabPaneSkin skin = (TabPaneSkin) tabPane.getSkin();
    if (skin == null) {
      return;
    }
    int tabIndex = tabPane.getTabs().indexOf(getRoot());
    if (tabIndex == -1) {
      // Tab has been closed
      return;
    }
    Node tabSkin = (Node) skin.queryAccessibleAttribute(ITEM_AT_INDEX, tabIndex);
    tabSkin.pseudoClassStateChanged(UNREAD_PSEUDO_STATE, unread);
  }

  @Override
  public abstract Tab getRoot();

  public ChatChannel getChatChannel() {
    return chatChannel.get();
  }

  public void setChatChannel(ChatChannel chatChannel) {
    this.chatChannel.set(chatChannel);
  }

  public ObjectProperty<ChatChannel> chatChannelProperty() {
    return chatChannel;
  }

  protected void onClosed(Event event) {
    ChatChannel channel = chatChannel.get();
    if (channel != null) {
      chatService.leaveChannel(channel);
    }
  }
}
