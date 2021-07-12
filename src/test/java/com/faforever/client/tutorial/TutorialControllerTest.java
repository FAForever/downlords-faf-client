package com.faforever.client.tutorial;

import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TutorialControllerTest extends UITest {
  private TutorialController instance;
  @Mock
  private UiService uiService;
  @Mock
  private TutorialService tutorialService;
  @Mock
  private TutorialDetailController tutorialDetailController;
  @Mock
  private TutorialCategoryListItemController tutorialsCategoryListItemController;
  @Mock
  private TutorialListItemController tutorialListItemController;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new TutorialController(tutorialService, uiService);
    loadFxml("theme/tutorial.fxml", clazz -> {
      if (TutorialDetailController.class.isAssignableFrom(clazz)) {
        return tutorialDetailController;
      }
      return instance;
    });
    TutorialCategory tutorialCategory = new TutorialCategory();
    tutorialCategory.setCategory("test category");
    Tutorial tutorial = new Tutorial();
    tutorial.setImageUrl("http://example.com/example.png");
    tutorial.setDescription("Test description");
    tutorial.setTitle("Test Title");
    tutorial.setId(1);
    tutorial.setOrdinal(1);
    tutorialCategory.getTutorials().add(tutorial);
    when(tutorialService.getTutorialCategories()).thenReturn(CompletableFuture.completedFuture(Collections.singletonList(tutorialCategory)));
    when(tutorialListItemController.getRoot()).thenReturn(new Pane());
    when(tutorialsCategoryListItemController.getRoot()).thenReturn(new Pane());
    when(uiService.loadFxml("theme/tutorial_category_list_item.fxml")).thenReturn(tutorialsCategoryListItemController);
    when(uiService.loadFxml("theme/tutorial_list_item.fxml")).thenReturn(tutorialListItemController);
  }

  @Test
  public void everythingLoadedSuccessfully() {
    instance.onDisplay(new NavigateEvent(NavigationItem.TUTORIALS));
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.tutorialPane.isVisible(), is(true));
    verify(tutorialService).getTutorialCategories();
  }
}