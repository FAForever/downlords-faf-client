package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.PopupUtil;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatMessageViewController extends NodeController<VBox> {

  private final ObjectFactory<ChatMessageCell> chatMessageCellFactory;
  private final NotificationService notificationService;
  private final ChatService chatService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final I18n i18n;
  private final ChatPrefs chatPrefs;

  public Button emoticonsButton;
  public TextField messageTextField;
  public VBox root;
  public Label typingLabel;
  public Node emoticonsWindow;
  public EmoticonsWindowController emoticonsWindowController;
  public Button cancelReplyButton;
  public Label replyPreviewLabel;
  public Label replyAuthorLabel;
  public HBox replyContainer;
  public VBox messagesContainer;

  private VirtualFlow<ChatMessage, Cell<ChatMessage, Node>> messageListView;

  private final List<String> userMessageHistory = new ArrayList<>();
  private final ObjectProperty<ChatChannel> chatChannel = new SimpleObjectProperty<>();
  private final ObservableValue<ObservableList<ChatChannelUser>> users = chatChannel.map(ChatChannel::getUsers);
  private final ListChangeListener<ChatChannelUser> typingUsersChangeListener = this::updateTypingUsersLabel;
  private final ObjectProperty<ChatMessage> targetMessage = new SimpleObjectProperty<>();

  private final ObservableList<ChatMessage> rawMessages = FXCollections.synchronizedObservableList(
      FXCollections.observableArrayList(chatMessage -> new Observable[]{chatMessage.getSender().categoryProperty()}));
  private final FilteredList<ChatMessage> filteredMessages = new FilteredList<>(
      new SortedList<>(rawMessages, Comparator.comparing(ChatMessage::getType).thenComparing(ChatMessage::getTime)));

  private final SetChangeListener<ChatMessage> chatMessageListener = this::onMessageChange;

  private Popup emoticonsPopup;

  private String currentUserMessage = "";
  private int curMessageHistoryIndex = 0;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(replyContainer);

    filteredMessages.predicateProperty().bind(chatPrefs.hideFoeMessagesProperty().map(hideFoes -> {
      if (!hideFoes) {
        return message -> true;
      } else {
        return message -> message.getSender().getCategory() != ChatUserCategory.FOE;
      }
    }));

    messageTextField.setOnKeyPressed(this::handleKeyEvent);
    messageTextField.textProperty().subscribe(this::updateTypingState);

    currentUserMessage = "";
    curMessageHistoryIndex = 0;

    chatChannel.when(attached).subscribe(((oldValue, newValue) -> {
      userMessageHistory.clear();
      if (oldValue != null) {
        oldValue.getMessages().removeListener(chatMessageListener);
        oldValue.getTypingUsers().removeListener(typingUsersChangeListener);
      }

      rawMessages.clear();

      if (newValue != null) {
        newValue.getMessages().addListener(chatMessageListener);
        fxApplicationThreadExecutor.execute(() -> rawMessages.addAll(newValue.getMessages()));
        ObservableList<ChatChannelUser> typingUsers = newValue.getTypingUsers();
        setTypingLabel(typingUsers);
        typingUsers.addListener(typingUsersChangeListener);

        scrollToEnd();
      }
    }));

    emoticonsWindowController.setOnEmoticonClicked(emoticon -> {
      messageTextField.appendText(" " + emoticon.shortcodes().getFirst() + " ");
      messageTextField.requestFocus();
      messageTextField.selectEnd();
    });
    emoticonsPopup = PopupUtil.createPopup(AnchorLocation.WINDOW_BOTTOM_RIGHT, emoticonsWindow);
    emoticonsPopup.setConsumeAutoHidingEvents(false);

    createAutoCompletionHelper().bindTo(messageTextField);

    replyContainer.visibleProperty().bind(targetMessage.isNotNull().when(showing));
    replyPreviewLabel.textProperty().bind(targetMessage.map(ChatMessage::getContent).when(showing));
    replyAuthorLabel.textProperty()
                    .bind(targetMessage.map(ChatMessage::getSender)
                                       .map(ChatChannelUser::getUsername)
                                       .map(username -> i18n.get("chat.replyingTo", username))
                                       .when(showing));

    messageListView = VirtualFlow.createVertical(filteredMessages, item -> {
      ChatMessageCell cell = chatMessageCellFactory.getObject();
      cell.showDetailsProperty()
          .bind(Bindings.valueAt(filteredMessages, cell.indexProperty().subtract(1))
                        .flatMap(previousMessage -> cell.itemProperty()
                                                        .map(currentMessage -> showDetails(previousMessage,
                                                                                           currentMessage)))
                        .orElse(true)
                        .when(showing));
      cell.updateItem(item);
      cell.setOnReplyButtonClicked(message -> {
        targetMessage.set(message);
        messageTextField.requestFocus();
      });
      cell.setOnReplyClicked(message -> fxApplicationThreadExecutor.execute(
          () -> messageListView.showAsFirst(filteredMessages.indexOf(message))));
      return cell;
    }, Gravity.FRONT);
    VirtualizedScrollPane<VirtualFlow<ChatMessage, Cell<ChatMessage, Node>>> scrollPane = new VirtualizedScrollPane<>(
        messageListView);
    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    VBox.setVgrow(scrollPane, Priority.ALWAYS);

    messagesContainer.getChildren().add(scrollPane);

    filteredMessages.subscribe(() -> {
      if (messageListView.getLastVisibleIndex() == filteredMessages.size() - 2) {
        scrollToEnd();
      }
    });
  }

  private boolean showDetails(ChatMessage previousMessage, ChatMessage currentMessage) {
    if (currentMessage == null) {
      return false;
    }

    if (previousMessage == null) {
      return true;
    }

    return !Objects.equals(previousMessage.getSender(), currentMessage.getSender());
  }

  private void scrollToEnd() {
    fxApplicationThreadExecutor.execute(() -> messageListView.showAsLast(filteredMessages.size() - 1));
  }

  private void onMessageChange(SetChangeListener.Change<? extends ChatMessage> change) {
    if (change.wasAdded()) {
      fxApplicationThreadExecutor.execute(() -> rawMessages.add(change.getElementAdded()));
    } else if (change.wasRemoved()) {
      fxApplicationThreadExecutor.execute(() -> rawMessages.remove(change.getElementRemoved()));
    }
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
      channel.getTypingUsers().removeListener(typingUsersChangeListener);
      channel.getMessages().removeListener(chatMessageListener);
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
    emoticonsPopup.hide();
  }

  private void updateUserMessageHistory(String text) {
    if (userMessageHistory.size() >= 50) {
      userMessageHistory.removeFirst();
      userMessageHistory.add(text);
    } else {
      userMessageHistory.add(text);
    }
  }

  private void sendMessage() {
    messageTextField.setDisable(true);

    final String text = messageTextField.getText();

    CompletableFuture<Void> sendFuture;
    ChatMessage targetMessage = this.targetMessage.get();
    if (targetMessage == null) {
      sendFuture = chatService.sendMessageInBackground(chatChannel.get(), text);
    } else {
      sendFuture = chatService.sendReplyInBackground(targetMessage, text);
    }

    sendFuture.whenComplete((result, throwable) -> {
      if (throwable != null) {
        throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
        log.warn("Message could not be sent: {}", text, throwable);
        notificationService.addImmediateErrorNotification(throwable, "chat.sendFailed");
      }
    }).whenCompleteAsync((_, _) -> {
      messageTextField.clear();
      messageTextField.setDisable(false);
      messageTextField.requestFocus();
      removeReply();
      scrollToEnd();
    }, fxApplicationThreadExecutor);
  }

  private void updateTypingUsersLabel(ListChangeListener.Change<? extends ChatChannelUser> change) {
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
    Bounds bounds = emoticonsButton.localToScreen(emoticonsButton.getBoundsInLocal());

    messageTextField.requestFocus();
    emoticonsPopup.show(emoticonsButton.getScene().getWindow(), bounds.getMaxX() - 5, bounds.getMinY() - 5);
  }

  public void removeReply() {
    targetMessage.set(null);
  }
}
