package com.faforever.client.tutorial;

import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.theme.UiService;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TutorialController extends AbstractViewController<Node> {
  private final TutorialService tutorialService;
  private final UiService uiService;
  public StackPane rootPane;
  public VBox tutorialOverviewPane;
  public TutorialDetailController tutorialDetailController;
  public HBox tutorialPane;
  public VBox loadingSpinner;
  public VBox nothingToShow;
  private List<TutorialListItemController> tutorialListItemControllers = new ArrayList<>();

  @Inject
  public TutorialController(TutorialService tutorialService, UiService uiService) {
    this.tutorialService = tutorialService;
    this.uiService = uiService;
  }

  @Override
  public void onDisplay(NavigateEvent navigateEvent) {
    super.onDisplay(navigateEvent);
    setLoading(true);
    setNothingToShow(false);
    tutorialListItemControllers.clear();
    tutorialOverviewPane.getChildren().clear();
    tutorialService.getTutorialCategories()
        .thenAccept(this::displayTutorials)
        .exceptionally(throwable -> {
          log.error("Tutorials could not be loaded", throwable);
          return null;
        });
  }

  private void setLoading(boolean loading) {
    tutorialPane.setVisible(!loading);
    loadingSpinner.setVisible(loading);
  }

  private void displayTutorials(List<TutorialCategory> categories) {
    Platform.runLater(() -> {
      if (categories.isEmpty()) {
        setLoading(false);
        setNothingToShow(true);
        return;
      }
      categories.forEach(tutorialCategory -> {
        addCategory(tutorialCategory);
        addTutorials(tutorialCategory.getTutorials());
      });

      if (tutorialDetailController.getTutorial() == null && !tutorialListItemControllers.isEmpty()) {
        onTutorialClicked(tutorialListItemControllers.get(0));
      }
      setLoading(false);
      setNothingToShow(false);
    });
  }

  private void setNothingToShow(boolean activate) {
    nothingToShow.setVisible(activate);
  }

  private void addTutorials(List<Tutorial> tutorials) {
    tutorials.forEach(tutorial -> {
      TutorialListItemController tutorialListItemController = uiService.loadFxml("theme/tutorial_list_item.fxml");
      tutorialListItemController.setTutorial(tutorial);
      Node root = tutorialListItemController.getRoot();
      tutorialListItemControllers.add(tutorialListItemController);
      tutorialOverviewPane.getChildren().add(root);
      root.setOnMouseClicked(event -> onTutorialClicked(tutorialListItemController));
    });
  }

  private void onTutorialClicked(TutorialListItemController tutorialListItemController) {
    tutorialListItemControllers.forEach(specificTutorialListItemController -> specificTutorialListItemController.getRoot().pseudoClassStateChanged(TutorialListItemController.SELECTED_PSEUDO_CLASS, false));
    tutorialListItemController.getRoot().pseudoClassStateChanged(TutorialListItemController.SELECTED_PSEUDO_CLASS, true);
    tutorialDetailController.setTutorial(tutorialListItemController.getTutorial());
  }

  private void addCategory(TutorialCategory category) {
    TutorialCategoryListItemController tutorialCategoryListItemController = uiService.loadFxml("theme/tutorial_category_list_item.fxml");
    tutorialCategoryListItemController.setCategory(category);
    tutorialOverviewPane.getChildren().add(tutorialCategoryListItemController.getRoot());
  }

  @Override
  public Node getRoot() {
    return rootPane;
  }

}
