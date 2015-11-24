package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.JavaFxUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class ModVaultController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TOP_ELEMENT_COUNT = 7;
  private static final int MAX_SUGGESTIONS = 10;

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
  Pane mostLikedMods;
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
  @Resource
  Directory directory;
  @Resource
  ExecutorService executorService;
  @Resource
  Analyzer analyzer;
  private boolean initialized;

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

    searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.isEmpty()) {
        return;
      }

      modService.searchMod(newValue);
    });
  }

  public void setUpIfNecessary() {
    if (initialized) {
      return;
    }
    initialized = true;

    displayShowroomMods();

    try {
      AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(directory, analyzer);
      ModInfoBeanIterator iterator = new ModInfoBeanIterator(modService.getAvailableMods().get().iterator());
      suggester.build(iterator);

      JavaFxUtil.makeSuggestionField(searchTextField, string -> {
        try {
          List<Lookup.LookupResult> results = suggester.lookup(string, MAX_SUGGESTIONS, true, false);

          return results.stream()
              .sorted()
              .map(result -> {
                ModInfoBean modInfoBean = iterator.deserialize(result.payload.bytes);
                String name = modInfoBean.getName();
                CustomMenuItem customMenuItem = new CustomMenuItem(new Label(name), true) {
                  @Override
                  public int hashCode() {
                    return ((Label) getContent()).getText().hashCode();
                  }

                  @Override
                  public boolean equals(Object obj) {
                    return ((Label) getContent()).getText().equals(((Label) ((CustomMenuItem) obj).getContent()).getText());
                  }
                };
                customMenuItem.setUserData(name);
                return customMenuItem;
              })
              .collect(Collectors.toSet());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }, customMenuItem -> (String) customMenuItem.getUserData());
    } catch (InterruptedException | ExecutionException | IOException e) {
      logger.warn("Search could not be executed", e);
    }
  }

  private void displayShowroomMods() {
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);

    modService.getMostDownloadedMods(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateMods(modInfoBeans, popularModsPane))
        .exceptionally(throwable -> {
          logger.warn("Could not populate mods", throwable);
          return null;
        });
    modService.getMostLikedMods(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateMods(modInfoBeans, mostLikedMods))
        .exceptionally(throwable -> {
          logger.warn("Could not populate mods", throwable);
          return null;
        });
    modService.getNewestMods(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateMods(modInfoBeans, newestModsPane))
        .exceptionally(throwable -> {
          logger.warn("Could not populate mods", throwable);
          return null;
        });
    modService.getMostLikedUiMods(TOP_ELEMENT_COUNT).thenAccept(modInfoBeans -> populateMods(modInfoBeans, recommendedUiModsPane))
        .exceptionally(throwable -> {
          logger.warn("Could not populate mods", throwable);
          return null;
        });
  }

  private void populateMods(List<ModInfoBean> modInfoBeans, Pane pane) {
    ObservableList<Node> children = pane.getChildren();
    Platform.runLater(() -> {
      children.clear();
      for (ModInfoBean mod : modInfoBeans) {
        ModTileController controller = applicationContext.getBean(ModTileController.class);
        controller.setMod(mod);
        controller.setOnOpenDetailListener(this::onShowModDetail);
        children.add(controller.getRoot());
      }
    });
  }

  @FXML
  void onShowModDetail(ModInfoBean mod) {
    modDetailController.setMod(mod);
    modDetailController.getRoot().setVisible(true);
    modDetailController.getRoot().requestFocus();
  }

  @FXML
  void onSearchModButtonClicked() {
    if (searchTextField.getText().isEmpty()) {
      onResetButtonClicked();
      return;
    }

    List<ModInfoBean> modInfoBeans = modService.searchMod(searchTextField.getText());
    displaySearchResult(modInfoBeans);
  }

  @FXML
  void onResetButtonClicked() {
    searchTextField.clear();
    showroomGroup.setVisible(true);
    searchResultGroup.setVisible(false);
  }

  private void displaySearchResult(List<ModInfoBean> modInfoBeans) {
    showroomGroup.setVisible(false);
    searchResultGroup.setVisible(true);

    populateMods(modInfoBeans, searchResultPane);
  }
}
