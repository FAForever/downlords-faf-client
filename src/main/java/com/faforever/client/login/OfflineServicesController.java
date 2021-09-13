package com.faforever.client.login;

import com.faforever.client.fx.Controller;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.Pane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class OfflineServicesController implements Controller<Pane> {

  public Pane offlineServicesRoot;
  public Pane offlineServicesContainer;

  private final UiService uiService;

  @Override
  public Pane getRoot() {
    return offlineServicesRoot;
  }

  public void addService(String serviceName, String reason, OffsetDateTime lastSeen) {
    OfflineServiceController controller = uiService.loadFxml("theme/login/offline_service.fxml");
    controller.setInfo(serviceName, reason, lastSeen);
    offlineServicesContainer.getChildren().add(controller.getRoot());
  }

}
