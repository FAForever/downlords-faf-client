package com.faforever.client.mod;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
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
  FlowPane recommendedUiModsPane;
  @FXML
  FlowPane newestModsPane;
  @FXML
  FlowPane popularModsPane;
  @FXML
  Pane modVaultRoot;

  @Resource
  ModService modService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  ModDetailController modDetailController;

  public Node getRoot() {
    return modVaultRoot;
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

  private void displayMods(List<ModInfoBean> modInfoBeans) {
    populateMods(modInfoBeans, ModInfoBean.DOWNLOADS_COMPARATOR, popularModsPane);
    populateMods(modInfoBeans, ModInfoBean.PUBLISH_DATE_COMPARATOR, newestModsPane);

    List<ModInfoBean> uiMods = modInfoBeans.stream().filter(ModInfoBean::getUiOnly).collect(Collectors.toList());
    populateMods(uiMods, ModInfoBean.LIKES_COMPARATOR, recommendedUiModsPane);
  }

  private void populateMods(List<ModInfoBean> modInfoBeans, Comparator<? super ModInfoBean> comparator, FlowPane popularModsPane) {
    ObservableList<Node> children = popularModsPane.getChildren();
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
    modService.requestMods().thenAccept(this::displayMods);
  }

  @FXML
  void onShowModDetail(ModInfoBean mod) {
    modDetailController.setMod(mod);
    modDetailController.getRoot().setVisible(true);
  }
}
