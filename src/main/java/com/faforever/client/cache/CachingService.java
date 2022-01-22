package com.faforever.client.cache;

import com.faforever.client.user.event.LoggedOutEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class CachingService implements InitializingBean {
  private final CacheManager cacheManager;
  private final EventBus eventBus;

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
  }

  private void evictAllCaches() {
    cacheManager.getCacheNames().stream()
        .map(cacheManager::getCache)
        .filter(Objects::nonNull)
        .forEach(Cache::clear);
  }

  @Subscribe
  public void onLogout(LoggedOutEvent event) {
    evictAllCaches();
  }
}
