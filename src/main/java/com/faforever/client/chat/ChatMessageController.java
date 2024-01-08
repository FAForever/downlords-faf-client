package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.chat.ChatMessage.Type;
import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.TimeService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableSet;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatMessageController extends NodeController<VBox> {

  private final AvatarService avatarService;
  private final CountryFlagService countryFlagService;
  private final TimeService timeService;
  private final PlatformService platformService;
  private final ChatService chatService;
  private final EmoticonService emoticonService;
  private final UiService uiService;
  private final ImageViewHelper imageViewHelper;
  private final I18n i18n;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public VBox root;
  public HBox detailsContainer;
  public ImageView avatarImageView;
  public ImageView countryImageView;
  public Label clanLabel;
  public Label authorLabel;
  public Label timeLabel;
  public TextFlow message;
  public HBox messageActionsContainer;
  public Button reactButton;
  public Button replyButton;
  public FlowPane reactionsContainer;

  private final Tooltip avatarTooltip = new Tooltip();
  private final ObjectProperty<ChatMessage> chatMessage = new SimpleObjectProperty<>();
  private final BooleanProperty showDetails = new SimpleBooleanProperty();
  private final StringProperty inlineTextColorStyleProperty = new SimpleStringProperty();
  private final MapChangeListener<Emoticon, ObservableSet<String>> reactionChangeListener = this::onReactionChange;

  private final Map<Emoticon, HBox> reactionNodeMap = new HashMap<>();

  private Pattern mentionPattern;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(detailsContainer, message);

    mentionPattern = chatService.getMentionPattern();

    ObservableValue<ChatChannelUser> sender = chatMessage.map(ChatMessage::getSender);
    ObservableValue<PlayerBean> player = sender.flatMap(ChatChannelUser::playerProperty);
    avatarImageView.imageProperty()
                   .bind(player.flatMap(PlayerBean::avatarProperty)
                               .map(avatarService::loadAvatar)
                               .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
                               .when(showing));
    countryImageView.imageProperty()
                    .bind(player.flatMap(PlayerBean::countryProperty)
                                .map(countryFlagService::loadCountryFlag)
                                .map(flagOptional -> flagOptional.orElse(null))
                                .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
                                .when(showing));
    inlineTextColorStyleProperty.bind(sender.flatMap(ChatChannelUser::colorProperty)
                                            .map(JavaFxUtil::toRgbCode)
                                            .map(rgb -> String.format("-fx-fill: %s; -fx-text-fill: %s;", rgb, rgb))
                                            .when(showing));
    clanLabel.textProperty().bind(player.flatMap(PlayerBean::clanProperty).map("[%s]"::formatted).when(showing));
    clanLabel.styleProperty().bind(inlineTextColorStyleProperty);
    ObservableValue<String> usernameProperty = sender.map(ChatChannelUser::getUsername).when(showing);
    authorLabel.textProperty().bind(usernameProperty);
    authorLabel.styleProperty().bind(inlineTextColorStyleProperty);
    authorLabel.setOnMouseClicked(event -> {
      String username = usernameProperty.getValue();
      if (username != null && event.getClickCount() == 2) {
        chatService.joinPrivateChat(username);
      }
    });
    timeLabel.textProperty()
             .bind(chatMessage.map(message -> message.getType() != Type.PENDING ? message.getTime() : null)
                              .map(timeService::asShortTime)
                              .orElse(i18n.get("pending"))
                              .when(showing));
    chatMessage.map(ChatMessage::getContent).map(this::convertMessageToNodes).when(showing).subscribe(messageNodes -> {
      Collection<? extends Node> children = messageNodes == null ? List.of() : messageNodes;
      message.getChildren().setAll(children);
    });

    detailsContainer.visibleProperty().bind(showDetails.when(showing));

    avatarTooltip.textProperty()
                 .bind(
                     player.flatMap(PlayerBean::avatarProperty).flatMap(AvatarBean::descriptionProperty).when(showing));
    avatarTooltip.setShowDelay(Duration.ZERO);
    avatarTooltip.setShowDuration(Duration.seconds(30));
    Tooltip.install(avatarImageView, avatarTooltip);

    chatMessage.subscribe((oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.getReactions().removeListener(reactionChangeListener);
      }

      reactionNodeMap.clear();
      fxApplicationThreadExecutor.execute(() -> reactionsContainer.getChildren().clear());

      if (newValue != null) {
        newValue.getReactions().forEach(this::addReaction);
        newValue.getReactions().addListener(reactionChangeListener);
      }
    });

    reactionsContainer.visibleProperty()
                      .bind(chatMessage.map(ChatMessage::getReactions).flatMap(Bindings::isNotEmpty).when(showing));
  }

  private void onReactionChange(MapChangeListener.Change<? extends Emoticon, ? extends ObservableSet<String>> change) {
    Emoticon reaction = change.getKey();
    if (change.wasRemoved()) {
      HBox reactionRoot = reactionNodeMap.remove(reaction);
      fxApplicationThreadExecutor.execute(() -> reactionsContainer.getChildren().remove(reactionRoot));
    }

    if (change.wasAdded()) {
      addReaction(reaction, change.getValueAdded());
    }
  }

  private void addReaction(Emoticon reaction, ObservableSet<String> reactors) {
    ReactionController reactionController = uiService.loadFxml("theme/chat/emoticons/reaction.fxml");
    reactionController.setReaction(reaction);
    reactionController.setReactors(reactors);
    reactionController.onReactionClickedProperty()
                      .bind(
                          chatMessage.map(message -> react -> chatService.reactToMessageInBackground(message, react)));
    HBox reactionRoot = reactionController.getRoot();
    reactionNodeMap.put(reaction, reactionRoot);
    fxApplicationThreadExecutor.execute(() -> reactionsContainer.getChildren().add(reactionRoot));
  }

  public void onMouseExited() {
    messageActionsContainer.setVisible(false);
  }

  public void onMouseEntered() {
    messageActionsContainer.setVisible(true);
  }

  private List<? extends Node> convertMessageToNodes(String message) {
    return Arrays.stream(message.split("\\s+")).map(this::convertWordToNode).peek(this::styleMessageNode).toList();
  }

  private Node convertWordToNode(String word) {
    return switch (word) {
      case String url when PlatformService.URL_REGEX_PATTERN.matcher(url).matches() -> createExternalHyperlink(url);
      case String channel when channel.startsWith("#") -> createChannelLink(channel);
      case String shortcode when emoticonService.isEmoticonShortcode(shortcode) -> createEmoticon(shortcode);
      default -> new Text(word + " ");
    };
  }

  private Pane createEmoticon(String shortcode) {
    ImageView imageView = new ImageView();
    imageView.setImage(emoticonService.getImageByShortcode(shortcode));
    imageView.setFitHeight(24);
    imageView.setFitWidth(24);
    Pane pane = new Pane(imageView);
    pane.setPadding(new Insets(0, 5, 0, 0));
    return pane;
  }

  private Hyperlink createChannelLink(String channelName) {
    Hyperlink hyperlink = new Hyperlink(channelName + " ");
    hyperlink.setOnAction(event -> chatService.joinChannel(channelName));
    return hyperlink;
  }

  private Hyperlink createExternalHyperlink(String url) {
    Hyperlink hyperlink = new Hyperlink(url + " ");
    hyperlink.setOnAction(event -> platformService.showDocument(url));
    return hyperlink;
  }

  private void styleMessageNode(Node node) {
    switch (node) {
      case ImageView ignored -> {}
      case Hyperlink ignored -> {}
      case Text text when mentionPattern.matcher(text.getText()).matches() -> text.setStyle("-fx-fill: #FFA500");
      default -> node.styleProperty().bind(inlineTextColorStyleProperty);
    }
  }

  @Override
  public VBox getRoot() {
    return root;
  }

  public ChatMessage getChatMessage() {
    return chatMessage.get();
  }

  public ObjectProperty<ChatMessage> chatMessageProperty() {
    return chatMessage;
  }

  public void setChatMessage(ChatMessage chatMessage) {
    this.chatMessage.set(chatMessage);
  }

  public boolean isShowDetails() {
    return showDetails.get();
  }

  public BooleanProperty showDetailsProperty() {
    return showDetails;
  }

  public void setShowDetails(boolean showDetails) {
    this.showDetails.set(showDetails);
  }

  public void onReply() {}

  public void onReact() {
    EmoticonsWindowController emoticonsWindowController = uiService.loadFxml(
        "theme/chat/emoticons/emoticons_window.fxml");
    Popup emoticonsPopup = PopupUtil.createPopup(AnchorLocation.WINDOW_BOTTOM_RIGHT,
                                                 emoticonsWindowController.getRoot());
    emoticonsPopup.setConsumeAutoHidingEvents(false);
    emoticonsWindowController.setOnEmoticonClicked(emoticon -> {
      ChatMessage target = getChatMessage();
      if (target != null && !target.getReactions()
                                   .getOrDefault(emoticon, FXCollections.emptyObservableSet())
                                   .contains(chatService.getCurrentUsername())) {
        chatService.reactToMessageInBackground(target, emoticon);
      }
      emoticonsPopup.hide();
    });

    Bounds bounds = reactButton.localToScreen(reactButton.getBoundsInLocal());
    if (bounds == null) {
      return;
    }

    emoticonsPopup.show(reactButton.getScene().getWindow(), bounds.getMaxX() - 5, bounds.getMinY() - 5);
  }
}
