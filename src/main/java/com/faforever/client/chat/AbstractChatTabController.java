package com.faforever.client.chat;

import com.faforever.client.fx.TabController;
import com.faforever.client.ui.StageHolder;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

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

  private final IntegerProperty unreadMessagesCount = new SimpleIntegerProperty();
  protected final ObjectProperty<ChatChannel> chatChannel = new SimpleObjectProperty<>();
  protected final ObservableValue<String> channelName = chatChannel.map(ChatChannel::getName);

  private final Consumer<ChatMessage> messageListener = this::onChatMessage;

  public Node chatMessagesView;
  public ChatMessageViewController chatMessagesViewController;

  @Override
  protected void onInitialize() {
    chatMessagesViewController.chatChannelProperty().bind(chatChannel.when(attached));

    chatChannel.when(attached).subscribe(((oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.openProperty().unbind();
        oldValue.setOpen(false);
        oldValue.removeMessageListener(messageListener);
      }
      if (newValue != null) {
        newValue.openProperty().bind(showing.and(StageHolder.getStage().focusedProperty()).when(attached));
        newValue.addMessageListener(messageListener);
      }
    }));

    showing.subscribe(showing -> {
      if (showing) {
        clearUnreadIfFocused();
      }
    });

    unreadMessagesCount.subscribe(
        (oldValue, newValue) -> incrementUnreadMessageCount(newValue.intValue() - oldValue.intValue()));
    getRoot().setOnClosed(this::onClosed);
  }

  @Override
  public void onAttached() {
    addAttachedSubscription(StageHolder.getStage().focusedProperty().subscribe(this::clearUnreadIfFocused));
  }

  @Override
  public void onDetached() {
    ChatChannel channel = chatChannel.get();
    if (channel != null) {
      channel.openProperty().unbind();
      channel.setOpen(false);
      channel.removeMessageListener(messageListener);
    }
  }

  private void clearUnreadIfFocused() {
    if (hasFocus()) {
      setUnread(false);
    }
  }

  /**
   * Returns true if this chat tab is currently focused by the user. Returns false if a different tab is selected, the
   * user is not in "chat" or if the window has no focus.
   */
  protected boolean hasFocus() {
    if (!getRoot().isSelected()) {
      return false;
    }

    TabPane tabPane = getRoot().getTabPane();
    if (tabPane == null) {
      return false;
    }

    Scene scene = tabPane.getScene();
    if (scene == null) {
      return false;
    }

    Window window = scene.getWindow();
    if (window == null) {
      return false;
    }

    return window.isFocused() && window.isShowing();
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

    if (!unread) {
      synchronized (unreadMessagesCount) {
        unreadMessagesCount.setValue(0);
      }
    }
  }

  @Override
  public abstract Tab getRoot();

  protected void incrementUnreadMessagesCount() {
    synchronized (unreadMessagesCount) {
      unreadMessagesCount.set(unreadMessagesCount.get() + 1);
    }
  }

  public ChatChannel getChatChannel() {
    return chatChannel.get();
  }

  public void setChatChannel(ChatChannel chatChannel) {
    this.chatChannel.set(chatChannel);
  }

  public ObjectProperty<ChatChannel> chatChannelProperty() {
    return chatChannel;
  }

  private void incrementUnreadMessageCount(int delta) {
    chatService.incrementUnreadMessagesCount(delta);
  }

  protected void onClosed(Event event) {
    ChatChannel channel = chatChannel.get();
    if (channel != null) {
      chatService.leaveChannel(channel);
      channel.removeMessageListener(messageListener);
    }
  }

  protected void onChatMessage(ChatMessage chatMessage) {
    // Can be overridden
  }
}
