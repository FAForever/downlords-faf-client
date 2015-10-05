package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.reporting.ReportingService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ModVaultController {

  private static final int TOP_ELEMENT_COUNT = 7;
  @FXML
  Pane searchResultGroup;
  @FXML
  Pane searchResultPane;
  @FXML
  Pane showroomGroup;
  @FXML
  TextField searchTextField;
  @FXML
  Pane recommendedUiModsPane;
  @FXML
  Pane newestModsPane;
  @FXML
  Pane popularModsPane;
  @FXML
  Pane modVaultRoot;

  @Resource
  ModService modService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  ModDetailController modDetailController;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  ReportingService reportingService;

  public Node getRoot() {
    return modVaultRoot;
  }

  @FXML
  void initialize() {
    showroomGroup.managedProperty().bind(showroomGroup.visibleProperty());
    searchResultGroup.managedProperty().bind(searchResultGroup.visibleProperty());
  }

  @PostConstruct
  void postConstruct() {
    Node modDetailRoot = modDetailController.getRoot();
    modVaultRoot.getChildren().add(modDetailRoot);
    AnchorPane.setTopAnchor(modDetailRoot, 0d);
    AnchorPane.setRightAnchor(modDetailRoot, 0d);
    AnchorPane.setBottomAnchor(modDetailRoot, 0d);
    AnchorPane.setLeftAnchor(modDetailRoot, 0d);
    modDetailRoot.setVisible(false);
  }

  private void displayShowroomMods(List<ModInfoBean> modInfoBeans) {
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);

    List<ModInfoBean> mods = new ArrayList<>(modInfoBeans);
    populateMods(mods, ModInfoBean.DOWNLOADS_COMPARATOR, popularModsPane);
    populateMods(mods, ModInfoBean.PUBLISH_DATE_COMPARATOR, newestModsPane);

    List<ModInfoBean> uiMods = mods.stream().filter(ModInfoBean::getUiOnly).collect(Collectors.toList());
    populateMods(uiMods, ModInfoBean.LIKES_COMPARATOR, recommendedUiModsPane);
  }

  private void populateMods(List<ModInfoBean> modInfoBeans, Comparator<? super ModInfoBean> comparator, Pane pane) {
    ObservableList<Node> children = pane.getChildren();
    List<ModInfoBean> mods = getTopElements(modInfoBeans, comparator);
    Platform.runLater(() -> {
      children.clear();
      for (ModInfoBean mod : mods) {
        ModTileController controller = applicationContext.getBean(ModTileController.class);
        controller.setMod(mod);
        controller.setOnOpenDetailListener(this::onShowModDetail);
        children.add(controller.getRoot());
      }
    });
  }

  private List<ModInfoBean> getTopElements(List<ModInfoBean> modInfoBeans, Comparator<? super ModInfoBean> comparator) {
    Collections.sort(modInfoBeans, comparator.reversed());
    List<ModInfoBean> newestMods = new ArrayList<>();
    for (ModInfoBean modInfoBean : modInfoBeans) {
      newestMods.add(modInfoBean);
      if (newestMods.size() == TOP_ELEMENT_COUNT) {
        return newestMods;
      }
    }
    return newestMods;
  }

  public void setUpIfNecessary() {
    modService.requestMods().thenAccept(this::displayShowroomMods);
  }

  @FXML
  void onShowModDetail(ModInfoBean mod) {
    modDetailController.setMod(mod);
    modDetailController.getRoot().setVisible(true);
  }

  @FXML
  void onSearchModButtonClicked() {
    modService.searchMod(searchTextField.getText())
        .thenAccept(this::displaySearchResult)
        .exceptionally(throwable -> {
          notificationService.addNotification(
              new ImmediateNotification(
                  i18n.get("errorTitle"),
                  i18n.get("modVault.searchFailed"),
                  Severity.ERROR,
                  Collections.singletonList(new ReportAction(i18n, reportingService, throwable)))
          );
          return null;
        });
  }

  private void displaySearchResult(List<ModInfoBean> modInfoBeans) {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);

    populateMods(modInfoBeans, ModInfoBean.LIKES_COMPARATOR, searchResultPane);
  }

  @FXML
  void onResetButtonClicked(ActionEvent event) {
    searchTextField.clear();
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
  }
}
