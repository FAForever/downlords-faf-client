package com.faforever.client.remote;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.dto.GameReview;
import com.faforever.client.api.dto.MapVersionReview;
import com.faforever.client.api.dto.ModVersionReview;
import com.faforever.client.api.dto.Player;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.event.AvatarChangedEvent;
import com.faforever.client.vault.review.Review;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FafServiceImplTest {

  private FafServiceImpl instance;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private EventBus eventBus;
  @Mock
  private FafApiAccessor fafApiAccessor;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    instance = new FafServiceImpl(fafServerAccessor, fafApiAccessor, eventBus);
  }

  @Test
  public void selectAvatar() throws Exception {
    URL url = new URL("http://example.com");
    instance.selectAvatar(new AvatarBean(url, "Description"));

    ArgumentCaptor<AvatarChangedEvent> eventCaptor = ArgumentCaptor.forClass(AvatarChangedEvent.class);
    verify(eventBus).post(eventCaptor.capture());

    AvatarBean avatar = eventCaptor.getValue().getAvatar();
    assertThat(avatar, not(nullValue()));
    assertThat(avatar.getUrl(), is(url));
    assertThat(avatar.getDescription(), is("Description"));

    verify(fafServerAccessor).selectAvatar(url);
  }

  @Test
  public void createGameReview() throws Exception {
    Review review = createReview(null, "something", 3, 42);

    when(fafApiAccessor.createGameReview(any()))
        .thenReturn((GameReview) new GameReview().setPlayer(player()).setId("1").setScore((byte) 1));

    instance.saveGameReview(review, 5);
    verify(fafApiAccessor).createGameReview(any());
  }

  @Test
  public void createMapVersionReview() throws Exception {
    Review review = createReview(null, "something", 3, 42);

    when(fafApiAccessor.createMapVersionReview(any()))
        .thenReturn((MapVersionReview) new MapVersionReview().setPlayer(player()).setId("1").setScore((byte) 1));

    instance.saveMapVersionReview(review, "5");
    verify(fafApiAccessor).createMapVersionReview(any());
  }

  @Test
  public void createModVersion() throws Exception {
    Review review = createReview(null, "something", 3, 42);

    when(fafApiAccessor.createModVersionReview(any()))
        .thenReturn((ModVersionReview) new ModVersionReview().setPlayer(player()).setId("1").setScore((byte) 1));

    instance.saveModVersionReview(review, 5);
    verify(fafApiAccessor).createModVersionReview(any());
  }

  private Review createReview(Integer id, String text, int rating, Integer playerId) {
    Review review = new Review();
    review.setId(id);
    review.setText(text);
    review.setScore(rating);
    Optional.ofNullable(playerId)
        .map(String::valueOf)
        .ifPresent(s -> {
          com.faforever.client.player.Player player = new com.faforever.client.player.Player(s);
          player.setId(playerId);
          review.setPlayer(player);
        });

    return review;
  }

  private Player player() {
    return new Player().setId("1");
  }
}
