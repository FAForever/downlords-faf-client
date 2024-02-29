package com.faforever.client.tutorial;

import com.faforever.client.domain.TutorialCategoryBean;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.Pane;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TutorialControllerTest extends PlatformTest {
  @InjectMocks
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
    loadFxml("theme/tutorial.fxml", clazz -> {
      if (TutorialDetailController.class.isAssignableFrom(clazz)) {
        return tutorialDetailController;
      }
      return instance;
    });
    TutorialCategoryBean tutorialCategory = Instancio.create(TutorialCategoryBean.class);
    when(tutorialService.getTutorialCategories()).thenReturn(Flux.just(tutorialCategory));
    when(tutorialListItemController.getRoot()).thenReturn(new Pane());
    when(tutorialsCategoryListItemController.getRoot()).thenReturn(new Pane());
    when(uiService.loadFxml("theme/tutorial_category_list_item.fxml")).thenReturn(tutorialsCategoryListItemController);
    when(uiService.loadFxml("theme/tutorial_list_item.fxml")).thenReturn(tutorialListItemController);
  }

  @Test
  public void everythingLoadedSuccessfully() {
    instance.onNavigate(new NavigateEvent(NavigationItem.TUTORIALS));
    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.tutorialPane.isVisible(), is(true));
    verify(tutorialService).getTutorialCategories();
  }
}