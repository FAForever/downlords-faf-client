package com.faforever.client.main;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class StartTabChooseControllerTest extends UITest {

  @Mock
  private I18n i18n;
  @InjectMocks
  private StartTabChooseController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/start_tab_choose.fxml", clazz -> instance);
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void testGetSelection() {
    instance.tabItemChoiceBox.getSelectionModel().select(NavigationItem.MAP);
    assertThat(instance.getSelected(), is(NavigationItem.MAP));
  }
}