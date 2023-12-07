package com.faforever.client.cache;

import com.faforever.client.user.LoginService;
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
  private final LoginService loginService;

  @Override
  public void afterPropertiesSet() throws Exception {
    loginService.loggedInProperty().subscribe(this::evictAllCaches);
  }

  private void evictAllCaches() {
    cacheManager.getCacheNames().stream()
        .map(cacheManager::getCache)
        .filter(Objects::nonNull)
        .forEach(Cache::clear);
  }
}
