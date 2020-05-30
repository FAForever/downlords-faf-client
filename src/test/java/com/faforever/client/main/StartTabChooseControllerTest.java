package com.faforever.client.main;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StartTabChooseControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private I18n i18n;
  private StartTabChooseController instance;

  @Before
  public void setUp() throws Exception {
    instance = new StartTabChooseController(i18n);
    loadFxml("theme/start_tab_choose.fxml", clazz -> instance);
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void testGetSelection() {
    instance.tabItemChoiceBox.getSelectionModel().select(NavigationItem.VAULT);
    assertThat(instance.getSelected(), is(NavigationItem.VAULT));
  }
}