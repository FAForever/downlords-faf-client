package com.faforever.client.cache;

import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.LoginService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachingServiceTest extends ServiceTest {
  private final BooleanProperty loggedIn = new SimpleBooleanProperty();

  @InjectMocks
  private CachingService instance;

  @Mock
  private LoginService loginService;
  @Mock
  private CacheManager cacheManager;

  @Test
  public void testOnLoginStatusChange() throws Exception {
    when(loginService.loggedInProperty()).thenReturn(loggedIn);

    instance.afterPropertiesSet();

    Cache cache = mock(Cache.class);
    when(cacheManager.getCacheNames()).thenReturn(List.of("test"));
    when(cacheManager.getCache("test")).thenReturn(cache);

    loggedIn.set(true);
    loggedIn.set(false);

    verify(cache).clear();
  }
}
