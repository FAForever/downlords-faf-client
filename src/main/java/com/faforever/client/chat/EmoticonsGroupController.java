package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonsGroup;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.theme.UiService;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class EmoticonsGroupController implements Controller<VBox> {

  private final UiService uiService;
  private final PlatformService platformService;

  public VBox root;
  public Label groupLabel;
  public HBox attributionPane;
  public Hyperlink attributionHyperlink;
  public FlowPane emoticonsPane;

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(attributionPane);
  }

  public void setGroup(EmoticonsGroup group, Consumer<String> onEmoticonAction) {
    groupLabel.setText(group.getName());
    String attribution = group.getAttribution();
    if (!StringUtils.isBlank(attribution)) {
      attributionHyperlink.setText(attribution);
      attributionHyperlink.setOnAction(event -> platformService.showDocument(attribution));
      attributionPane.setVisible(true);
    }
    List<AnchorPane> emoticonViewList = group.getEmoticons().stream().map(emoticon -> {
      EmoticonController controller = uiService.loadFxml("theme/chat/emoticon/emoticon.fxml");
      controller.setEmoticon(emoticon, onEmoticonAction);
      return controller.getRoot();
    }).collect(Collectors.toList());
    emoticonsPane.getChildren().addAll(emoticonViewList);
  }

  @Override
  public VBox getRoot() {
    return root;
  }
}
