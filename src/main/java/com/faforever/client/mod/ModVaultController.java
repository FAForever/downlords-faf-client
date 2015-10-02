package com.faforever.client.mod;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
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

    modService.getAvailableMods().addListener((Observable observable) -> {
      ObservableSet<ModInfoBean> availableMods = modService.getAvailableMods();
      // TODO after writing this code, I had to take a shower. If you read this, please help me getting a proper server API
      if (availableMods.size() % (3 * TOP_ELEMENT_COUNT) == 0) {
        displayMods(availableMods);
      }
    });
  }

  private void displayMods(ObservableSet<ModInfoBean> modInfoBeans) {
    List<ModInfoBean> mods = new ArrayList<>(modInfoBeans);
    populateMods(mods, ModInfoBean.DOWNLOADS_COMPARATOR, popularModsPane);
    populateMods(mods, ModInfoBean.PUBLISH_DATE_COMPARATOR, newestModsPane);

    List<ModInfoBean> uiMods = mods.stream().filter(ModInfoBean::getUiOnly).collect(Collectors.toList());
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
    modService.requestMods();
  }

  @FXML
  void onShowModDetail(ModInfoBean mod) {
    modDetailController.setMod(mod);
    modDetailController.getRoot().setVisible(true);
  }
}
