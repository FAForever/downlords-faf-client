package com.faforever.client.mod;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ModVaultController {

  private static final int TOP_ELEMENT_COUNT = 7;

  @FXML
  FlowPane recommendedModsPane;
  @FXML
  FlowPane newestModsPane;
  @FXML
  FlowPane popularModsPane;
  @FXML
  Node modVaultRoot;

  @Resource
  ModService modService;

  @Resource
  ApplicationContext applicationContext;

  public Node getRoot() {
    return modVaultRoot;
  }

  @PostConstruct
  void postConstruct() {
    modService.getAvailableMods().addListener(new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        ObservableSet<ModInfoBean> availableMods = modService.getAvailableMods();
        // TODO after writing this code, I had to take a shower. If you read this, please help me getting a proper server API
        if (availableMods.size() % 25 == 0) {
          displayMods(availableMods);
        }
      }
    });
  }

  private void displayMods(ObservableSet<ModInfoBean> modInfoBeans) {
    List<ModInfoBean> mods = new ArrayList<>(modInfoBeans);
    populateMods(mods, ModInfoBean.DOWNLOADS_COMPARATOR, popularModsPane);
    populateMods(mods, ModInfoBean.PUBLISH_DATE_COMPARATOR, newestModsPane);
    populateMods(mods, ModInfoBean.LIKES_COMPARATOR, recommendedModsPane);
  }

  private void populateMods(List<ModInfoBean> modInfoBeans, Comparator<? super ModInfoBean> comparator, FlowPane popularModsPane) {
    ObservableList<Node> children = popularModsPane.getChildren();
    List<ModInfoBean> mods = getTopElements(modInfoBeans, comparator);
    Platform.runLater(() -> {
      children.clear();
      for (ModInfoBean mod : mods) {
        ModTileController controller = applicationContext.getBean(ModTileController.class);
        controller.setMod(mod);
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
}
