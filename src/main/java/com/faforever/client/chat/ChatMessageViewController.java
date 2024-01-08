package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.fx.FxApplicationThreadExecutor;
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
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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

  private final ObjectFactory<ChatMessageItemCell> chatMessageItemCellFactory;
  private final NotificationService notificationService;
  private final ChatService chatService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final I18n i18n;
  private final ChatPrefs chatPrefs;

  public Button emoticonsButton;
  public TextField messageTextField;
  public ListView<ChatMessage> messagesListView;
  public VBox root;
  public Label typingLabel;
  public Node emoticonsWindow;
  public EmoticonsWindowController emoticonsWindowController;

  private final List<String> userMessageHistory = new ArrayList<>();
  private final ObjectProperty<ChatChannel> chatChannel = new SimpleObjectProperty<>();
  private final ObservableValue<ObservableList<ChatChannelUser>> users = chatChannel.map(ChatChannel::getUsers);
  private final ListChangeListener<ChatChannelUser> typingUserListChangeListener = this::updateTypingUsersLabel;

  private final ObservableList<ChatMessage> rawMessages = FXCollections.synchronizedObservableList(
      FXCollections.observableArrayList(chatMessage -> new Observable[]{chatMessage.getSender().categoryProperty()}));
  private final FilteredList<ChatMessage> filteredMessages = new FilteredList<>(rawMessages);

  private Popup emoticonsPopup;

  private String currentUserMessage = "";
  private int curMessageHistoryIndex = 0;

  @Override
  protected void onInitialize() {
    filteredMessages.predicateProperty().bind(chatPrefs.hideFoeMessagesProperty().map(hideFoes -> {
      if (!hideFoes) {
        return message -> true;
      } else {
        return message -> message.getSender().getCategory() != ChatUserCategory.FOE;
      }
    }));

    messagesListView.setSelectionModel(null);
    messagesListView.setItems(filteredMessages);
    messagesListView.setOrientation(Orientation.VERTICAL);
    messagesListView.setCellFactory(ignored -> chatMessageItemCellFactory.getObject());

    messageTextField.setOnKeyPressed(this::handleKeyEvent);
    messageTextField.textProperty().subscribe(this::updateTypingState);

    currentUserMessage = "";
    curMessageHistoryIndex = 0;

    chatChannel.when(attached).subscribe(((oldValue, newValue) -> {
      userMessageHistory.clear();
      if (oldValue != null) {
        Bindings.bindContent(rawMessages, oldValue.getMessages());
        oldValue.getTypingUsers().removeListener(typingUserListChangeListener);
      }

      rawMessages.clear();

      if (newValue != null) {
        Bindings.bindContent(rawMessages, newValue.getMessages());
        ObservableList<ChatChannelUser> typingUsers = newValue.getTypingUsers();
        setTypingLabel(typingUsers);
        typingUsers.addListener(typingUserListChangeListener);
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

    filteredMessages.subscribe(
        () -> fxApplicationThreadExecutor.execute(() -> messagesListView.scrollTo(filteredMessages.size())));
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
      Bindings.unbindContent(rawMessages, channel.getMessages());
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

    chatService.sendMessageInBackground(chatChannel.get(), text).whenComplete((result, throwable) -> {
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
