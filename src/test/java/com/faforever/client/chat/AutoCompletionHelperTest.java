package com.faforever.client.chat;

import com.faforever.client.player.PlayerService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.FXCollections;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AutoCompletionHelperTest extends AbstractPlainJavaFxTest {

  private static final long TIMEOUT = 5000;

  @Mock
  private PlayerService playerService;

  private AutoCompletionHelper instance;
  private TextInputControl textInputControl;

  @Before
  public void setUp() throws Exception {
    instance = new AutoCompletionHelper(playerService);

    textInputControl = new TextField();
    instance.bindTo(textInputControl);
  }

  @Test
  public void testAutoCompleteWithEmptyText() throws Exception {
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    simulate(keyEvent);

    assertThat(textInputControl.getText(), Matchers.isEmptyString());
  }

  @NotNull
  private KeyEvent keyEvent(KeyCode keyCode) {
    return keyEvent(keyCode, emptyList());
  }

  private void simulate(KeyEvent keyEvent) {
    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () -> textInputControl.getEventDispatcher().dispatchEvent(keyEvent, null));
  }

  @NotNull
  private KeyEvent keyEvent(KeyCode keyCode, Collection<KeyCode> modifiers) {
    return new KeyEvent(null, null, KeyEvent.KEY_PRESSED, "\u0000", "", keyCode,
        modifiers.contains(KeyCode.SHIFT),
        modifiers.contains(KeyCode.CONTROL), modifiers.contains(KeyCode.ALT),
        modifiers.contains(KeyCode.META));
  }

  @Test
  public void unbindTest() {
    assertTrue(instance.isBound());
    instance.unbind();
    assertFalse(instance.isBound());
  }

  @Test
  public void testAutoCompleteDoesntCompleteWhenTheresNoWordBeforeCaret() throws Exception {
    textInputControl.setText("j");
    textInputControl.positionCaret(0);

    simulate(keyEvent(KeyCode.TAB));

    assertThat(textInputControl.getText(), is("j"));
    verify(playerService, never()).getPlayerNames();
  }

  @Test
  public void testAutoCompleteCompletesToFirstMatchCaseInsensitive() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("DummyUser", "Junit"));
    textInputControl.setText("j");
    textInputControl.positionCaret(1);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    simulate(keyEvent);

    assertThat(textInputControl.getText(), is("Junit"));
  }

  @Test
  public void testAutoCompleteCompletesToFirstMatchCaseInsensitiveRepeated() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("DummyUser", "Junit"));
    textInputControl.setText("j");
    textInputControl.positionCaret(1);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    simulate(keyEvent);
    simulate(keyEvent);
    assertThat(textInputControl.getText(), is("Junit"));
  }

  @Test
  public void testAutoCompleteCycles() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("JayUnit", "Junit"));
    textInputControl.setText("j");
    textInputControl.positionCaret(1);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    simulate(keyEvent);
    assertThat(textInputControl.getText(), is("JayUnit"));

    simulate(keyEvent);
    assertThat(textInputControl.getText(), is("Junit"));

    simulate(keyEvent);
    assertThat(textInputControl.getText(), is("JayUnit"));
  }

  @Test
  public void testAutoCompleteSortedByName() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("JBunit", "JAyUnit"));
    textInputControl.setText("j");
    textInputControl.positionCaret(1);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    simulate(keyEvent);

    assertThat(textInputControl.getText(), is("JAyUnit"));
  }

  @Test
  public void testAutoCompleteCaretMovedAway() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("JUnit", "Downlord"));
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    // Start auto completion on "JB"
    textInputControl.setText("JU Do");
    textInputControl.positionCaret(2);
    simulate(keyEvent);

    // Then auto complete on "Do"
    textInputControl.positionCaret(textInputControl.getText().length());
    simulate(keyEvent);

    assertThat(textInputControl.getText(), is("JUnit Downlord"));
  }
}
