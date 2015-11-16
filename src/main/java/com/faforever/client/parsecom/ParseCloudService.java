package com.faforever.client.parsecom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

public class ParseCloudService implements CloudService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  Environment environment;

  @Resource
  ThreadPoolExecutor threadPoolExecutor;

  @PostConstruct
  public void postConstruct() {
  }

  @Override
  public CompletableFuture<String> signUpOrLogIn(String username, String password, String email, int uid) {
    return CompletableFuture.completedFuture(null);
  }
}
