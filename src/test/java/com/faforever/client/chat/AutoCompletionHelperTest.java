package com.faforever.client.chat;

import com.faforever.client.test.UITest;
import javafx.collections.FXCollections;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Collection;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AutoCompletionHelperTest extends UITest {

  private static final long TIMEOUT = 5000;

  @Mock
  private Function<String, Collection<String>> completionProposalGeneratorMock;

  private AutoCompletionHelper instance;
  private TextInputControl textInputControl;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new AutoCompletionHelper(completionProposalGeneratorMock);

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
    verify(completionProposalGeneratorMock, never()).apply(any());
  }

  @Test
  public void testAutoCompleteCompletesToFirstMatchCaseInsensitive() throws Exception {
    when(completionProposalGeneratorMock.apply(any())).thenReturn(FXCollections.observableSet("Junit"));
    textInputControl.setText("j");
    textInputControl.positionCaret(1);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    simulate(keyEvent);

    assertThat(textInputControl.getText(), is("Junit"));
  }

  @Test
  public void testAutoCompleteCompletesToFirstMatchCaseInsensitiveRepeated() throws Exception {
    when(completionProposalGeneratorMock.apply(any())).thenReturn(FXCollections.observableSet("DummyUser", "Junit"));
    textInputControl.setText("j");
    textInputControl.positionCaret(1);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    simulate(keyEvent);
    simulate(keyEvent);
    assertThat(textInputControl.getText(), is("Junit"));
  }

  @Test
  public void testAutoCompleteCycles() throws Exception {
    when(completionProposalGeneratorMock.apply(any())).thenReturn(FXCollections.observableSet("JayUnit", "Junit"));
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
  public void testAutoCompleteCaretMovedAway() throws Exception {
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    // Start auto completion on "JU"
    when(completionProposalGeneratorMock.apply(any())).thenReturn(FXCollections.observableSet("JUnit"));
    textInputControl.setText("JU Do");
    textInputControl.positionCaret(2);
    simulate(keyEvent);

    // Then auto complete on "Do"
    when(completionProposalGeneratorMock.apply(any())).thenReturn(FXCollections.observableSet("Downlord"));
    textInputControl.positionCaret(textInputControl.getText().length());
    simulate(keyEvent);

    assertThat(textInputControl.getText(), is("JUnit Downlord"));
  }
}
