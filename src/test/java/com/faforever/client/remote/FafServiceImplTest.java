package com.faforever.client.remote;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.event.AvatarChangedEvent;
import com.google.common.eventbus.EventBus;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;
import java.util.concurrent.ThreadPoolExecutor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

public class FafServiceImplTest {

  private FafServiceImpl instance;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private EventBus eventBus;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private ThreadPoolExecutor threadPoolExecutor;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    instance = new FafServiceImpl(fafServerAccessor, fafApiAccessor, threadPoolExecutor, eventBus);
  }

  @Test
  public void selectAvatar() throws Exception {
    URL url = new URL("http://example.com");
    instance.selectAvatar(new AvatarBean(url, "Description"));

    ArgumentCaptor<AvatarChangedEvent> eventCaptor = ArgumentCaptor.forClass(AvatarChangedEvent.class);
    verify(eventBus).post(eventCaptor.capture());

    AvatarBean avatar = eventCaptor.getValue().getAvatar();
    assertThat(avatar, not(CoreMatchers.nullValue()));
    assertThat(avatar.getUrl(), is(url));
    assertThat(avatar.getDescription(), is("Description"));

    verify(fafServerAccessor).selectAvatar(url);
  }
}
