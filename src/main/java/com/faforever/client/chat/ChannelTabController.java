package com.faforever.client.chat;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.preferences.ChatPrefs;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.faforever.client.fx.PlatformService.STRICT_URL_REGEX_PATTERN;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChannelTabController extends AbstractChatTabController {

  private static final int TOPIC_CHARACTERS_LIMIT = 350;

  private final PlatformService platformService;
  private final ChatPrefs chatPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Tab root;
  public SplitPane chatPane;
  public HBox topicPane;
  public Label topicCharactersLimitLabel;
  public TextField topicTextField;
  public TextFlow topicText;
  public Button changeTopicTextButton;
  public Button cancelChangesTopicTextButton;
  public ToggleButton userListVisibilityToggleButton;
  public Node chatUserList;
  public ChatUserListController chatUserListController;
  public VBox loadingPane;

  private final ObservableValue<ChannelTopic> channelTopic = chatChannel.flatMap(ChatChannel::topicProperty);
  private final BooleanProperty loaded = new SimpleBooleanProperty();

  public ChannelTabController(ChatService chatService, PlatformService platformService, ChatPrefs chatPrefs,
                              FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(chatService);
    this.platformService = platformService;
    this.chatPrefs = chatPrefs;
    this.fxApplicationThreadExecutor = fxApplicationThreadExecutor;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    JavaFxUtil.bindManagedToVisible(topicPane, chatUserList, changeTopicTextButton, topicTextField,
                                    cancelChangesTopicTextButton, topicText, topicCharactersLimitLabel, loadingPane,
                                    chatPane);

    loaded.bind(chatChannel.flatMap(ChatChannel::loadedProperty).when(attached));
    chatPane.visibleProperty().bind(loaded.when(showing));
    loadingPane.visibleProperty().bind(loaded.not().when(showing));

    topicCharactersLimitLabel.visibleProperty().bind(topicTextField.visibleProperty());
    cancelChangesTopicTextButton.visibleProperty().bind(topicTextField.visibleProperty());
    chatUserList.visibleProperty().bind(userListVisibilityToggleButton.selectedProperty());
    userListVisibilityToggleButton.selectedProperty().bindBidirectional(chatPrefs.playerListShownProperty());

    topicTextField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText()
                                                                        .length() <= TOPIC_CHARACTERS_LIMIT ? change : null));

    topicCharactersLimitLabel.textProperty()
                             .bind(topicTextField.textProperty()
                                                 .length()
                                                 .map(length -> String.format("%d / %d", length.intValue(),
                                                                              TOPIC_CHARACTERS_LIMIT))
                                                 .when(showing));

    topicPane.visibleProperty()
             .bind(topicText.visibleProperty()
                            .or(changeTopicTextButton.visibleProperty())
                            .or(topicTextField.visibleProperty())
                            .when(showing));

    root.textProperty().bind(channelName.map(name -> name.replaceFirst("^#", "")).when(attached));

    chatUserListController.chatChannelProperty().bind(chatChannel.when(loaded));

    ObservableValue<Boolean> isModerator = chatChannel.map(
                                                          channel -> channel.getUser(chatService.getCurrentUsername()).orElse(null))
                                                      .flatMap(ChatChannelUser::moderatorProperty)
                                                      .orElse(false)
                                                      .when(showing);
    changeTopicTextButton.visibleProperty()
                         .bind(BooleanExpression.booleanExpression(isModerator)
                                                .and(topicTextField.visibleProperty().not()).when(loaded));

    channelTopic.when(loaded).subscribe(this::updateChannelTopic);
    userListVisibilityToggleButton.selectedProperty().when(loaded).subscribe(this::updateDividerPosition);
  }

  private void updateDividerPosition(boolean selected) {
    chatPane.setDividerPositions(selected ? 0.8 : 1);
  }

  private void setChannelTopic(String content) {
    List<Node> children = topicText.getChildren();
    children.clear();
    boolean notBlank = StringUtils.isNotBlank(content);
    if (notBlank) {
      Arrays.stream(content.split("\\s")).forEach(word -> {
        if (STRICT_URL_REGEX_PATTERN.matcher(word).matches()) {
          Hyperlink link = new Hyperlink(word);
          link.setOnAction(_ -> platformService.showDocument(word));
          children.add(link);
        } else {
          children.add(new Label(word + " "));
        }
      });
    }
    topicText.setVisible(notBlank);
  }

  private void updateChannelTopic(ChannelTopic oldTopic, ChannelTopic newTopic) {
    fxApplicationThreadExecutor.execute(() -> {
      setChannelTopic(newTopic.content());

      if (topicPane.isDisable()) {
        topicTextField.setVisible(false);
        topicPane.setDisable(false);
      }
    });
  }

  public void onChangeTopicTextButtonClicked() {
    topicText.setVisible(false);
    topicTextField.setText(channelTopic.getValue().content());
    topicTextField.setVisible(true);
    topicTextField.requestFocus();
    topicTextField.selectEnd();
  }

  public void onTopicTextFieldEntered() {
    String normalizedText = StringUtils.normalizeSpace(topicTextField.getText());
    if (!normalizedText.equals(channelTopic.getValue().content())) {
      topicPane.setDisable(true);
      chatService.setChannelTopic(chatChannel.getValue(), normalizedText);
    } else {
      onCancelChangesTopicTextButtonClicked();
    }
  }

  public void onCancelChangesTopicTextButtonClicked() {
    topicTextField.setText(channelTopic.getValue().content());
    topicTextField.setVisible(false);
    topicText.setVisible(true);
    changeTopicTextButton.setVisible(true);
  }

  @Override
  public Tab getRoot() {
    return root;
  }
}