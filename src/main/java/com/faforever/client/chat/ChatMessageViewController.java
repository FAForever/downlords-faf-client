package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.PopupUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlow.Gravity;
import org.fxmisc.flowless.VirtualizedScrollPane;
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
  public VBox messagesContainer;
  public VBox root;
  public Node emoticonsWindow;
  public Label typingLabel;
  public EmoticonsWindowController emoticonsWindowController;

  private VirtualFlow<ChatMessage, Cell<ChatMessage, Node>> messageView;

  private final List<String> userMessageHistory = new ArrayList<>();
  private final ObjectProperty<ChatChannel> chatChannel = new SimpleObjectProperty<>();
  private final ObservableValue<ObservableList<ChatChannelUser>> users = chatChannel.map(ChatChannel::getUsers);
  private final ListChangeListener<ChatChannelUser> typingUserListChangeListener = this::updateTypingUsersLabel;
  private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();

  private Popup emoticonsPopup;

  private String currentUserMessage = "";
  private int curMessageHistoryIndex = 0;

  @VisibleForTesting
  Pattern mentionPattern;

  @Override
  protected void onInitialize() {
    messageView = VirtualFlow.createVertical(messages, this::createCellWithItem, Gravity.FRONT);
    VirtualizedScrollPane<VirtualFlow<ChatMessage, Cell<ChatMessage, Node>>> scrollPane = new VirtualizedScrollPane<>(
        messageView);

    messages.subscribe(() -> messageView.showAsLast(messages.size() - 1));

    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    VBox.setVgrow(scrollPane, Priority.ALWAYS);
    messagesContainer.getChildren().add(scrollPane);

    mentionPattern = chatService.getMentionPattern();

    messageTextField.setOnKeyPressed(this::handleKeyEvent);
    messageTextField.textProperty().subscribe(this::updateTypingState);

    currentUserMessage = "";
    curMessageHistoryIndex = 0;

    chatChannel.when(attached).subscribe(((oldValue, newValue) -> {
      userMessageHistory.clear();
      if (oldValue != null) {
        Bindings.unbindContent(messages, oldValue.getMessages());
        oldValue.getTypingUsers().removeListener(typingUserListChangeListener);
      }

      messages.clear();

      if (newValue != null) {
        Bindings.bindContent(messages, newValue.getMessages());
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

  private ChatMessageItemCell createCellWithItem(ChatMessage item) {
    ChatMessageItemCell cell = chatMessageItemCellFactory.getObject();
    cell.updateItem(item);
    return cell;
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
