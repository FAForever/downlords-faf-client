package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.util.TimeService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A chat tab displays messages in a {@link WebView}. The WebView is used since text on a JavaFX canvas isn't
 * selectable, but text within a WebView is. This comes with some ugly implications; some logic has to be performed in
 * interaction with JavaScript, like when the user clicks a link.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatMessageController extends NodeController<VBox> {

  private final AvatarService avatarService;
  private final CountryFlagService countryFlagService;
  private final TimeService timeService;
  private final ImageViewHelper imageViewHelper;

  public VBox root;
  public HBox detailsContainer;
  public ImageView avatarImageView;
  public ImageView countryImageView;
  public Label clanLabel;
  public Label authorLabel;
  public Label timeLabel;
  public TextFlow message;

  private final ObjectProperty<ChatMessage> chatMessage = new SimpleObjectProperty<>();
  private final StringProperty colorProperty = new SimpleStringProperty();

  @Override
  protected void onInitialize() {
    ObservableValue<ChatChannelUser> sender = chatMessage.map(ChatMessage::sender);
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
    colorProperty.bind(sender.flatMap(ChatChannelUser::colorProperty).map(JavaFxUtil::toRgbCode).when(showing));
    clanLabel.textProperty().bind(player.flatMap(PlayerBean::clanProperty).map("[%s]"::formatted).when(showing));
    authorLabel.textProperty().bind(sender.map(ChatChannelUser::getUsername).when(showing));
    timeLabel.textProperty().bind(chatMessage.map(ChatMessage::time).map(timeService::asShortTime));
    chatMessage.map(ChatMessage::message).map(this::convertMessageToNodes).when(showing).subscribe(messageNodes -> {
      Collection<? extends Node> children = messageNodes == null ? List.of() : messageNodes;
      message.getChildren().setAll(children);
    });
  }

  private List<? extends Node> convertMessageToNodes(String message) {
    return Arrays.stream(message.split("\\s"))
                 .map(word -> word + " ")
                 .map(this::convertWordToNode)
                 .peek(this::styleMessageNode)
                 .toList();
  }

  private Text convertWordToNode(String message) {
    return new Text(message);
  }

  private void styleMessageNode(Node node) {
    switch (node) {
      case Text text -> text.getStyleClass().add("text");
      default -> {}
    }

    node.styleProperty().bind(colorProperty.map(rgb -> String.format("-fx-fill: %s", rgb)).when(showing));
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
}
