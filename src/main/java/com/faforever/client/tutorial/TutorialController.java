package com.faforever.client.tutorial;

import com.faforever.client.domain.TutorialBean;
import com.faforever.client.domain.TutorialCategoryBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class TutorialController extends NodeController<Node> {
  private final TutorialService tutorialService;
  private final UiService uiService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public StackPane rootPane;
  public VBox tutorialOverviewPane;
  public TutorialDetailController tutorialDetailController;
  public HBox tutorialPane;
  public VBox loadingSpinner;
  public VBox nothingToShow;
  private final List<TutorialListItemController> tutorialListItemControllers = new ArrayList<>();

  @Override
  public void onNavigate(NavigateEvent navigateEvent) {
    super.onNavigate(navigateEvent);
    setLoading(true);
    setNothingToShow(false);
    tutorialListItemControllers.clear();
    tutorialOverviewPane.getChildren().clear();
    tutorialService.getTutorialCategories()
                   .collectList()
                   .publishOn(fxApplicationThreadExecutor.asScheduler())
                   .subscribe(this::displayTutorials,
                              throwable -> log.error("Tutorials could not be loaded", throwable));
  }

  private void setLoading(boolean loading) {
    tutorialPane.setVisible(!loading);
    loadingSpinner.setVisible(loading);
  }

  private void displayTutorials(List<TutorialCategoryBean> categories) {
      if (categories.isEmpty()) {
        setLoading(false);
        setNothingToShow(true);
        return;
      }
      categories.forEach(tutorialCategory -> {
        addCategory(tutorialCategory);
        addTutorials(tutorialCategory.tutorials());
      });

      if (tutorialDetailController.getTutorial() == null && !tutorialListItemControllers.isEmpty()) {
        onTutorialClicked(tutorialListItemControllers.getFirst());
      }
      setLoading(false);
      setNothingToShow(false);
  }

  private void setNothingToShow(boolean activate) {
    nothingToShow.setVisible(activate);
  }

  private void addTutorials(List<TutorialBean> tutorials) {
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

  private void addCategory(TutorialCategoryBean category) {
    TutorialCategoryListItemController tutorialCategoryListItemController = uiService.loadFxml("theme/tutorial_category_list_item.fxml");
    tutorialCategoryListItemController.setCategory(category);
    tutorialOverviewPane.getChildren().add(tutorialCategoryListItemController.getRoot());
  }

  @Override
  public Node getRoot() {
    return rootPane;
  }

}
