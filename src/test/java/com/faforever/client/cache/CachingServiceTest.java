package com.faforever.client.cache;

import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.event.LoggedOutEvent;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachingServiceTest extends ServiceTest {
  @InjectMocks
  private CachingService instance;

  @Mock
  private EventBus eventBus;
  @Mock
  private CacheManager cacheManager;

  @Test
  public void testOnLogout() throws Exception {
    instance.afterPropertiesSet();
    verify(eventBus).register(any());

    Cache cache = mock(Cache.class);
    when(cacheManager.getCacheNames()).thenReturn(List.of("test"));
    when(cacheManager.getCache("test")).thenReturn(cache);

    instance.onLogout(new LoggedOutEvent());

    verify(cache).clear();
  }
}
