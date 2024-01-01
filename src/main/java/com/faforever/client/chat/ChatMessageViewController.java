package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.ui.list.NoSelectionModel;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.PopupUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A chat tab displays messages in a {@link WebView}. The WebView is used since text on a JavaFX canvas isn't
 * selectable, but text within a WebView is. This comes with some ugly implications; some logic has to be performed in
 * interaction with JavaScript, like when the user clicks a link.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatMessageViewController extends NodeController<VBox> {

  private static final String ACTION_PREFIX = "/me ";

  private final ObjectFactory<ChatMessageItemCell> chatMessageItemCellFactory;
  private final NotificationService notificationService;
  private final ChatService chatService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final I18n i18n;

  public Button emoticonsButton;
  public TextField messageTextField;
  public ListView<ChatMessage> messagesListView;
  public VBox root;
  public Node emoticonsWindow;
  public Label typingLabel;
  public EmoticonsWindowController emoticonsWindowController;

  private final List<String> userMessageHistory = new ArrayList<>();
  private final ObjectProperty<ChatChannel> chatChannel = new SimpleObjectProperty<>();
  private final ObservableValue<ObservableList<ChatChannelUser>> users = chatChannel.map(ChatChannel::getUsers);
  private final ListChangeListener<ChatChannelUser> typingUserListChangeListener = this::updateTypingUsersLabel;
  private final ListChangeListener<ChatMessage> messageSynchronizationListener = this::synchronizeChange;

  private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();

  private Popup emoticonsPopup;

  private String currentUserMessage = "";
  private int curMessageHistoryIndex = 0;

  @VisibleForTesting
  Pattern mentionPattern;

  @Override
  protected void onInitialize() {
    messages.subscribe(() -> messagesListView.scrollTo(messages.getLast()));

    messagesListView.setSelectionModel(new NoSelectionModel<>());
    messagesListView.setItems(messages);
    messagesListView.setOrientation(Orientation.VERTICAL);
    messagesListView.setCellFactory(ignored -> chatMessageItemCellFactory.getObject());

    mentionPattern = chatService.getMentionPattern();

    messageTextField.setOnKeyPressed(this::handleKeyEvent);
    messageTextField.textProperty().subscribe(this::updateTypingState);

    currentUserMessage = "";
    curMessageHistoryIndex = 0;

    chatChannel.when(attached).subscribe(((oldValue, newValue) -> {
      userMessageHistory.clear();
      if (oldValue != null) {
        oldValue.getMessages().removeListener(messageSynchronizationListener);
        oldValue.getTypingUsers().removeListener(typingUserListChangeListener);
      }

      messages.clear();

      if (newValue != null) {
        addMessagesAtIndex(0, newValue.getMessages());
        newValue.getMessages().addListener(messageSynchronizationListener);
        ObservableList<ChatChannelUser> typingUsers = newValue.getTypingUsers();
        typingUsers.addListener(typingUserListChangeListener);
        setTypingLabel(typingUsers);
      }
    }));

    emoticonsWindowController.setTextInputControl(messageTextField);
    emoticonsPopup = PopupUtil.createPopup(AnchorLocation.WINDOW_BOTTOM_RIGHT, emoticonsWindow);
    emoticonsPopup.setConsumeAutoHidingEvents(false);

    createAutoCompletionHelper().bindTo(messageTextField);
  }

  private AutoCompletionHelper createAutoCompletionHelper() {
    return new AutoCompletionHelper(currentWord -> {
      ObservableList<ChatChannelUser> users = this.users.getValue();
      return users == null ? List.of() : users.stream()
                                              .map(ChatChannelUser::getUsername)
                                              .filter(username -> username.toLowerCase(Locale.ROOT)
                                                                          .startsWith(currentWord.toLowerCase()))
                                              .sorted()
                                              .collect(Collectors.toList());
    });
  }

  private void updateTypingState() {
    ChatChannel channel = chatChannel.get();
    if (!messageTextField.getText().isEmpty()) {
      chatService.setActiveTypingState(channel);
    } else if (!messageTextField.isDisabled()) {
      chatService.setDoneTypingState(channel);
    }
  }

  @Override
  public void onAttached() {
    addAttachedSubscription(StageHolder.getStage().focusedProperty().subscribe(messageTextField::requestFocus));
  }

  @Override
  public void onDetached() {
    ChatChannel channel = chatChannel.get();
    if (channel != null) {
      channel.getTypingUsers().removeListener(typingUserListChangeListener);
      channel.getMessages().removeListener(messageSynchronizationListener);
    }
  }

  private void handleKeyEvent(KeyEvent event) {
    updateMessageWithHistory(event);
  }

  private void updateMessageWithHistory(KeyEvent event) {
    if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.UP) {
      if (curMessageHistoryIndex == 0) {
        currentUserMessage = messageTextField.getText();
      }

      switch (event.getCode()) {
        case DOWN -> curMessageHistoryIndex--;
        case UP -> curMessageHistoryIndex++;
        default -> {}
      }

      int userMessageCount = userMessageHistory.size();
      if (curMessageHistoryIndex <= 0) {
        curMessageHistoryIndex = 0;
        messageTextField.setText(currentUserMessage);
      } else if (curMessageHistoryIndex <= userMessageCount) {
        messageTextField.setText(userMessageHistory.get(userMessageCount - curMessageHistoryIndex));
      } else {
        curMessageHistoryIndex = userMessageCount;
      }

      messageTextField.positionCaret(messageTextField.getText().length());
    } else {
      curMessageHistoryIndex = 0;
    }
  }

  @Override
  public VBox getRoot() {
    return root;
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

  public void onSendMessage() {
    String text = messageTextField.getText();
    if (StringUtils.isEmpty(text)) {
      return;
    }

    updateUserMessageHistory(text);
    sendMessage();
    hideEmoticonsWindow();
  }

  private void updateUserMessageHistory(String text) {
    if (userMessageHistory.size() >= 50) {
      userMessageHistory.removeFirst();
      userMessageHistory.add(text);
    } else {
      userMessageHistory.add(text);
    }
  }

  private void hideEmoticonsWindow() {
    emoticonsPopup.hide();
  }

  private void sendMessage() {
    messageTextField.setDisable(true);

    final String text = messageTextField.getText();
    CompletableFuture<Void> sendFuture;
    if (text.startsWith(ACTION_PREFIX)) {
      sendFuture = chatService.sendActionInBackground(chatChannel.get(),
                                                      text.replaceFirst(Pattern.quote(ACTION_PREFIX), ""));
    } else {
      sendFuture = chatService.sendMessageInBackground(chatChannel.get(), text);
    }

    sendFuture.whenComplete((result, throwable) -> {
      if (throwable != null) {
        throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
        log.warn("Message could not be sent: {}", text, throwable);
        notificationService.addImmediateErrorNotification(throwable, "chat.sendFailed");
      }
    }).whenCompleteAsync((result, throwable) -> {
      if (throwable == null) {
        messageTextField.clear();
      }
      messageTextField.setDisable(false);
      messageTextField.requestFocus();
    }, fxApplicationThreadExecutor);
  }

  private void synchronizeChange(Change<? extends ChatMessage> change) {
    while (change.next()) {
      int from = change.getFrom();
      if (change.wasPermutated()) {
        int to = change.getTo();
        removeMessagesInRange(from, to);
        List<? extends ChatMessage> newMessages = change.getList().subList(from, to);
        addMessagesAtIndex(from, newMessages);
      } else {
        if (change.wasRemoved()) {
          int to = from + change.getRemovedSize();
          removeMessagesInRange(from, to);
        }
        if (change.wasAdded()) {
          addMessagesAtIndex(from, change.getAddedSubList());
        }
      }
    }
  }

  private void addMessagesAtIndex(int from, List<? extends ChatMessage> newMessages) {
    fxApplicationThreadExecutor.execute(() -> messages.addAll(from, newMessages));
  }

  private void removeMessagesInRange(int from, int to) {
    fxApplicationThreadExecutor.execute(() -> messages.subList(from, to).clear());
  }

  private ChatMessageItem createChatMessageItem(ChatMessage chatMessage) {
    return new ChatMessageItem(chatMessage);
  }

  private void updateTypingUsersLabel(Change<? extends ChatChannelUser> change) {
    List<ChatChannelUser> typingUsers = List.copyOf(change.getList());
    setTypingLabel(typingUsers);
  }

  private void setTypingLabel(List<ChatChannelUser> typingUsers) {
    List<String> typingNames = typingUsers.stream().map(ChatChannelUser::getUsername).toList();
    String typingText;
    if (typingNames.isEmpty()) {
      typingText = "";
    } else if (typingNames.size() == 1) {
      typingText = i18n.get("chat.typing.single", typingNames.getFirst());
    } else if (typingNames.size() == 2) {
      typingText = i18n.get("chat.typing.double", typingNames.getFirst(), typingNames.getLast());
    } else {
      typingText = i18n.get("chat.typing.many");
    }

    fxApplicationThreadExecutor.execute(() -> {
      typingLabel.setVisible(!typingUsers.isEmpty());
      typingLabel.setText(typingText);
    });
  }

  public void openEmoticonsPopupWindow() {
    Bounds screenBounds = emoticonsButton.localToScreen(emoticonsButton.getBoundsInLocal());
    double anchorX = screenBounds.getMaxX() - 5;
    double anchorY = screenBounds.getMinY() - 5;

    messageTextField.requestFocus();
    emoticonsPopup.show(emoticonsButton.getScene().getWindow(), anchorX, anchorY);
  }
}
