package com.faforever.client.main;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LinksAndHelpController extends AbstractViewController<Node> {

  private final ClientProperties clientProperties;
  private final UiService uiService;
  private final I18n i18n;
  public ScrollPane root;
  public VBox linkContainer;

  public LinksAndHelpController(ClientProperties clientProperties, UiService uiService, I18n i18n) {
    this.clientProperties = clientProperties;
    this.uiService = uiService;
    this.i18n = i18n;
  }

  public void initialize() {
    clientProperties.getLinks().forEach((name, url) -> {
      LinkDetailController linkDetailController = uiService.loadFxml("theme/link_detail.fxml");
      linkContainer.getChildren().add(linkDetailController.getRoot());
      linkDetailController.setName(i18n.get(name));
      linkDetailController.setUrl(url);
    });
  }

  @Override
  public Node getRoot() {
    return root;
  }
}
