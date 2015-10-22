package com.faforever.client.parsecom;

import com.faforever.client.util.JavaFxUtil;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ParseCloudAccessor implements CloudAccessor {

  public static class ParseUrl extends GenericUrl {

    private static final String BASE = "https://api.parse.com/1/";

    public ParseUrl(String encodedUrl) {
      super(encodedUrl);
    }

    public static ParseUrl logIn(String username, String password) {
      return new ParseUrl(BASE + "login?username=" + username + "&password=" + password);
    }

    public static ParseUrl callFunction(String function) {
      return new ParseUrl(BASE + "functions/" + function);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final long LOGIN_TIMEOUT = 10000;
  private static final String X_PARSE_APPLICATION_ID = "X-Parse-Application-Id";
  private static final String X_PARSE_REST_API_KEY = "X-Parse-REST-API-Key";
  private static final String X_PARSE_SESSION_TOKEN = "X-Parse-Session-Token";
  private final CountDownLatch loginLatch;
  @Resource
  Environment environment;
  @Resource
  ExecutorService executorService;
  @Resource
  HttpTransport httpTransport;
  @Resource
  JsonFactory jsonFactory;
  private String parseAppId;
  private String parseApiId;
  private String sessionToken;
  private HttpRequestFactory requestFactory;

  public ParseCloudAccessor() {
    loginLatch = new CountDownLatch(1);
  }

  @PostConstruct
  public void postConstruct() {
    parseAppId = environment.getProperty("parse.appId");
    parseApiId = environment.getProperty("parse.apiId");
    requestFactory = httpTransport.createRequestFactory(request -> request.setParser(new JsonObjectParser(jsonFactory)));
  }

  @Override
  public CompletableFuture<String> signUpOrLogIn(String username, String password, int uid) {
    HashMap<String, Object> params = new HashMap<>();
    params.put("username", username);
    params.put("password", password);
    params.put("uid", uid);

    return this.<String>callCloudFunction("signUpIfNecessary", params, false).thenApply(result -> {
      loginLatch.countDown();
      try {
        login(username, password);
        logger.info("Logged in to parse services");
        return result;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public CompletableFuture<String> getPlayerIdForUsername(String username) {
    HashMap<String, Object> params = new HashMap<>();
    params.put("username", username);

    return CompletableFuture.completedFuture("1");
  }

  @Override
  public CompletableFuture<String> setPlayerId(String playerId) {
    HashMap<String, Object> params = new HashMap<>();
    params.put("playerId", playerId);

    return callCloudFunction("setPlayerId", params, true);
  }

  @SuppressWarnings("unchecked")
  private <T> CompletableFuture<T> callCloudFunction(String function, HashMap<String, Object> params, boolean needsLogin) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        logger.debug("Calling cloud function '{}'", function);

        GenericUrl url = ParseUrl.callFunction(function);

        HttpRequest httpRequest = requestFactory.buildPostRequest(url, new UrlEncodedContent(params));

        httpRequest.getHeaders()
            .set(X_PARSE_APPLICATION_ID, parseAppId)
            .set(X_PARSE_REST_API_KEY, parseApiId);

        if (needsLogin) {
          awaitLogin();
          httpRequest.getHeaders().set(X_PARSE_SESSION_TOKEN, sessionToken);
        }

        HttpResponse response = httpRequest.execute();
        try {
          ParseResult parseResult = response.parseAs(ParseResult.class);
          logger.debug("Function '{}' completed with result: {}", function, parseResult.result);
          return (T) parseResult.result;
        } finally {
          response.disconnect();
        }
      } catch (IOException | InterruptedException e) {
        logger.warn("Call to function '" + function + "' failed", e);
        throw new RuntimeException(e);
      }
    }, executorService);
  }

  private void login(String username, String password) throws IOException {
    GenericUrl url = ParseUrl.logIn(username, password);

    HttpRequest httpRequest = requestFactory.buildGetRequest(url);
    httpRequest.getHeaders()
        .set(X_PARSE_APPLICATION_ID, parseAppId)
        .set(X_PARSE_REST_API_KEY, parseApiId);

    HttpResponse response = httpRequest.execute();
    try {
      ParseCloudAccessor.this.sessionToken = response.parseAs(ParseUser.class).sessionToken;
    } finally {
      response.disconnect();
    }
  }

  private void awaitLogin() throws InterruptedException {
    JavaFxUtil.assertBackgroundThread();
    if (!loginLatch.await(LOGIN_TIMEOUT, TimeUnit.MILLISECONDS)) {
      throw new RuntimeException("Login timed out");
    }
  }
}
