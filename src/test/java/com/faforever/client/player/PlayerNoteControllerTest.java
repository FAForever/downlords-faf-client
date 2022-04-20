package com.faforever.client.player;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.test.UITest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.faforever.client.player.PlayerNoteController.CHARACTER_LIMIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

public class PlayerNoteControllerTest extends UITest {

  @Mock
  private PlayerService playerService;

  @InjectMocks
  private PlayerNoteController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/player_note.fxml", clazz -> instance);
    runOnFxThreadAndWait(() -> instance.initialize());
  }

  @Test
  public void testSetPlayer() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().note("junit").get();
    runOnFxThreadAndWait(() -> instance.setPlayer(player));

    assertEquals("junit", instance.textArea.getText());
  }

  @Test
  public void testCharacterLimit() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().note(RandomStringUtils.randomAlphanumeric(CHARACTER_LIMIT)).get();
    runOnFxThreadAndWait(() -> instance.setPlayer(player));
    assertEquals(CHARACTER_LIMIT, instance.textArea.getLength());

    runOnFxThreadAndWait(() -> instance.textArea.appendText("1"));
    assertEquals(CHARACTER_LIMIT, instance.textArea.getLength());
  }

  @Test
  public void testNoMultipleSeparators() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().note("junit\n").get();
    runOnFxThreadAndWait(() -> instance.setPlayer(player));

    assertEquals("junit\n", instance.textArea.getText());

    runOnFxThreadAndWait(() -> instance.textArea.appendText("\n"));
    assertEquals("junit\n", instance.textArea.getText());
  }

  @Test
  public void testOnOkButtonClicked() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().note("junit").get();
    runOnFxThreadAndWait(() -> {
      instance.setPlayer(player);
      instance.textArea.setText("updated");
      instance.okButtonClicked();
    });

    verify(playerService).updateNote(player, "updated");
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }
}