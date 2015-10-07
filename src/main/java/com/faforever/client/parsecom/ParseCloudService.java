package com.faforever.client.parsecom;

import org.parse4j.Parse;
import org.parse4j.ParseCloud;
import org.parse4j.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
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
    Parse.initialize(environment.getProperty("parse.appId"), environment.getProperty("parse.apiId"));
  }

  @Override
  public CompletableFuture<String> signUpOrLogIn(String username, String password) {
    HashMap<String, Object> params = new HashMap<>();
    params.put("username", username);
    params.put("password", password);

    return callCloudFunction("signUpIfNecessary", params)
        .exceptionally(throwable -> {
          callCloudFunction("logIn", params);
          return null;
        });
  }

  private CompletableFuture<String> callCloudFunction(String function, HashMap<String, Object> params) {
    CompletableFuture<String> future = new CompletableFuture<>();

    threadPoolExecutor.submit(() -> {
      try {
        logger.debug("Calling cloud function '{}' with params {}", function, params);
        String result = ParseCloud.callFunction(function, params);
        logger.debug("Function '{}' completed with result: {}", function, result);
        future.complete(result);
      } catch (ParseException e) {
        logger.warn("Function '" + function + "' failed", e);
        future.completeExceptionally(e);
      }
    });

    return future;
  }
}
