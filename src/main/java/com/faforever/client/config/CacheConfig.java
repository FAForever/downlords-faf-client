package com.faforever.client.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static com.faforever.client.config.CacheNames.ACHIEVEMENTS;
import static com.faforever.client.config.CacheNames.ACHIEVEMENT_IMAGES;
import static com.faforever.client.config.CacheNames.AVAILABLE_AVATARS;
import static com.faforever.client.config.CacheNames.AVATARS;
import static com.faforever.client.config.CacheNames.COUNTRY_FLAGS;
import static com.faforever.client.config.CacheNames.FEATURED_MODS;
import static com.faforever.client.config.CacheNames.LARGE_MAP_PREVIEW;
import static com.faforever.client.config.CacheNames.LEADERBOARD;
import static com.faforever.client.config.CacheNames.MAPS;
import static com.faforever.client.config.CacheNames.MODS;
import static com.faforever.client.config.CacheNames.MOD_THUMBNAIL;
import static com.faforever.client.config.CacheNames.NEWS;
import static com.faforever.client.config.CacheNames.RATING_HISTORY;
import static com.faforever.client.config.CacheNames.SMALL_MAP_PREVIEW;
import static com.faforever.client.config.CacheNames.STATISTICS;
import static com.faforever.client.config.CacheNames.THEME_IMAGES;
import static com.faforever.client.config.CacheNames.URL_PREVIEW;
import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

  @Bean
  @Override
  public CacheManager cacheManager() {
    SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
    simpleCacheManager.setCaches(Arrays.asList(
        new GuavaCache(STATISTICS, newBuilder().maximumSize(10).expireAfterWrite(20, MINUTES).build()),
        new GuavaCache(ACHIEVEMENTS, newBuilder().expireAfterWrite(10, MINUTES).build()),
        new GuavaCache(MODS, newBuilder().expireAfterWrite(10, MINUTES).build()),
        new GuavaCache(MAPS, newBuilder().expireAfterWrite(10, MINUTES).build()),
        new GuavaCache(LEADERBOARD, newBuilder().maximumSize(1).expireAfterAccess(1, MINUTES).build()),
        new GuavaCache(AVAILABLE_AVATARS, newBuilder().expireAfterAccess(30, SECONDS).build()),
        new GuavaCache(NEWS, newBuilder().expireAfterWrite(1, MINUTES).build()),
        new GuavaCache(RATING_HISTORY, newBuilder().expireAfterWrite(1, MINUTES).build()),
        new GuavaCache(FEATURED_MODS, newBuilder().build()),

        // Images should only be cached as long as they are in use. This avoids loading an image multiple times, while
        // at the same time it doesn't prevent unused images from being garbage collected.
        new GuavaCache(ACHIEVEMENT_IMAGES, newBuilder().weakValues().build()),
        new GuavaCache(AVATARS, newBuilder().weakValues().build()),
        new GuavaCache(URL_PREVIEW, newBuilder().weakValues().expireAfterAccess(30, MINUTES).build()),
        new GuavaCache(LARGE_MAP_PREVIEW, newBuilder().weakValues().build()),
        new GuavaCache(SMALL_MAP_PREVIEW, newBuilder().weakValues().build()),
        new GuavaCache(COUNTRY_FLAGS, newBuilder().weakValues().build()),
        new GuavaCache(THEME_IMAGES, newBuilder().weakValues().build()),
        new GuavaCache(MOD_THUMBNAIL, newBuilder().weakValues().build()
        )));
    return simpleCacheManager;
  }

  @Override
  public CacheResolver cacheResolver() {
    return null;
  }

  @Bean
  @Override
  public KeyGenerator keyGenerator() {
    return new SimpleKeyGenerator();
  }

  @Override
  public CacheErrorHandler errorHandler() {
    return null;
  }
}
