package com.faforever.client.player;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.social.SocialService;
import com.faforever.client.test.PlatformTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.faforever.client.player.PlayerNoteController.CHARACTER_LIMIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

public class PlayerNoteControllerTest extends PlatformTest {

  @Mock
  private PlayerService playerService;
  @Mock
  private SocialService socialService;

  @InjectMocks
  private PlayerNoteController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/player_note.fxml", clazz -> instance);
    runOnFxThreadAndWait(() -> reinitialize(instance));
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
  public void testOnOkButtonClicked() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().note("junit").get();
    runOnFxThreadAndWait(() -> {
      instance.setPlayer(player);
      instance.textArea.setText("updated");
      instance.okButtonClicked();
    });

    verify(socialService).updateNote(player, "updated");
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }
}