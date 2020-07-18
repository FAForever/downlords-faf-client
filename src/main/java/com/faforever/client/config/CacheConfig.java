package com.faforever.client.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static com.faforever.client.config.CacheNames.ACHIEVEMENTS;
import static com.faforever.client.config.CacheNames.ACHIEVEMENT_IMAGES;
import static com.faforever.client.config.CacheNames.AVAILABLE_AVATARS;
import static com.faforever.client.config.CacheNames.AVATARS;
import static com.faforever.client.config.CacheNames.CLAN;
import static com.faforever.client.config.CacheNames.COOP_LEADERBOARD;
import static com.faforever.client.config.CacheNames.COOP_MAPS;
import static com.faforever.client.config.CacheNames.COUNTRY_FLAGS;
import static com.faforever.client.config.CacheNames.FEATURED_MODS;
import static com.faforever.client.config.CacheNames.FEATURED_MOD_FILES;
import static com.faforever.client.config.CacheNames.GLOBAL_LEADERBOARD;
import static com.faforever.client.config.CacheNames.LADDER_1V1_LEADERBOARD;
import static com.faforever.client.config.CacheNames.MAPS;
import static com.faforever.client.config.CacheNames.MAP_GENERATOR;
import static com.faforever.client.config.CacheNames.MAP_PREVIEW;
import static com.faforever.client.config.CacheNames.MODS;
import static com.faforever.client.config.CacheNames.MOD_THUMBNAIL;
import static com.faforever.client.config.CacheNames.NEWS;
import static com.faforever.client.config.CacheNames.RATING_HISTORY;
import static com.faforever.client.config.CacheNames.STATISTICS;
import static com.faforever.client.config.CacheNames.THEME_IMAGES;
import static com.faforever.client.config.CacheNames.URL_PREVIEW;
import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {

  @Bean
  @Override
  public CacheManager cacheManager() {
    SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
    simpleCacheManager.setCaches(Arrays.asList(
        new CaffeineCache(STATISTICS, newBuilder().maximumSize(10).expireAfterWrite(20, MINUTES).build()),
        new CaffeineCache(ACHIEVEMENTS, newBuilder().expireAfterWrite(10, MINUTES).build()),
        new CaffeineCache(MODS, newBuilder().expireAfterWrite(10, MINUTES).build()),
        new CaffeineCache(MAPS, newBuilder().expireAfterWrite(10, MINUTES).build()),
        new CaffeineCache(MAP_GENERATOR, newBuilder().expireAfterWrite(10, MINUTES).build()),
        new CaffeineCache(GLOBAL_LEADERBOARD, newBuilder().maximumSize(1).expireAfterAccess(5, MINUTES).build()),
        new CaffeineCache(LADDER_1V1_LEADERBOARD, newBuilder().maximumSize(1).expireAfterAccess(5, MINUTES).build()),
        new CaffeineCache(AVAILABLE_AVATARS, newBuilder().expireAfterAccess(10, MINUTES).build()),
        new CaffeineCache(COOP_MAPS, newBuilder().expireAfterAccess(10, MINUTES).build()),
        new CaffeineCache(NEWS, newBuilder().expireAfterWrite(5, MINUTES).build()),
        new CaffeineCache(RATING_HISTORY, newBuilder().expireAfterWrite(1, MINUTES).build()),
        new CaffeineCache(COOP_LEADERBOARD, newBuilder().expireAfterWrite(1, MINUTES).build()),
        new CaffeineCache(CLAN, newBuilder().expireAfterWrite(1, HOURS).build()),
        new CaffeineCache(FEATURED_MODS, newBuilder().build()),
        new CaffeineCache(FEATURED_MOD_FILES, newBuilder().expireAfterWrite(10, MINUTES).build()),

        // Images should only be cached as long as they are in use. This avoids loading an image multiple times, while
        // at the same time it doesn't prevent unused images from being garbage collected.
        new CaffeineCache(ACHIEVEMENT_IMAGES, newBuilder().weakValues().build()),
        new CaffeineCache(AVATARS, newBuilder().weakValues().build()),
        new CaffeineCache(URL_PREVIEW, newBuilder().weakValues().expireAfterAccess(30, MINUTES).build()),
        new CaffeineCache(MAP_PREVIEW, newBuilder().weakValues().build()),
        new CaffeineCache(COUNTRY_FLAGS, newBuilder().weakValues().build()),
        new CaffeineCache(THEME_IMAGES, newBuilder().weakValues().build()),
        new CaffeineCache(MOD_THUMBNAIL, newBuilder().weakValues().build()
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
    return new SimpleCacheErrorHandler();
  }
}
