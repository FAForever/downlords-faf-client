package com.faforever.client.chat;

import com.faforever.client.test.PlatformTest;
import javafx.scene.control.Tab;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class AbstractChatTabControllerTest extends PlatformTest {

  @Mock
  private ChatService chatService;

  private AbstractChatTabController instance;

  @BeforeEach
  public void setup() throws Exception {
    when(chatService.getCurrentUsername()).thenReturn("junit");

    fxApplicationThreadExecutor.executeAndWait(() -> instance = new AbstractChatTabController(chatService) {
      private final Tab root = new Tab();

      @Override
      public Tab getRoot() {
        return root;
      }
    });

    fxApplicationThreadExecutor.executeAndWait(() -> reinitialize(instance));
  }
}
