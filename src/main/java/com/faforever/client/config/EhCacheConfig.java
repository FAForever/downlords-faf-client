package com.faforever.client.config;

import net.sf.ehcache.config.CacheConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class EhCacheConfig implements CachingConfigurer {

  static final String CACHE_POLICY = "LRU";

  @Bean(destroyMethod = "shutdown")
  public net.sf.ehcache.CacheManager ehCacheManager() {
    net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
    config.addCache(cacheConfig(CacheKeys.LARGE_MAP_PREVIEW, 30, CacheConfiguration.DEFAULT_TTL));
    config.addCache(cacheConfig(CacheKeys.SMALL_MAP_PREVIEW, 30, CacheConfiguration.DEFAULT_TTL));
    config.addCache(cacheConfig(CacheKeys.COUNTRY_FLAGS, 100, CacheConfiguration.DEFAULT_TTL));
    config.addCache(cacheConfig(CacheKeys.AVATARS, 30, CacheConfiguration.DEFAULT_TTL));
    config.addCache(cacheConfig(CacheKeys.URL_PREVIEW, 10, 30_000));
    return net.sf.ehcache.CacheManager.newInstance(config);
  }

  @Bean
  @Override
  public CacheManager cacheManager() {
    return new EhCacheCacheManager(ehCacheManager());
  }

  @Bean
  @Override
  public KeyGenerator keyGenerator() {
    return new SimpleKeyGenerator();
  }

  private CacheConfiguration cacheConfig(String name, long maxEntries, long timeToIdle) {
    CacheConfiguration config = new CacheConfiguration();
    config.setName(name);
    config.setMaxEntriesLocalHeap(maxEntries);
    config.setTimeToIdleSeconds(timeToIdle);
    config.setMemoryStoreEvictionPolicy(CACHE_POLICY);
    return config;
  }
}
