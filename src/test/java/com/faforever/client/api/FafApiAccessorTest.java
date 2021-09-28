package com.faforever.client.api;

import com.faforever.client.builders.ReplayReviewBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.JsonApiConfig;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.ReviewMapper;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.commons.api.dto.ApiException;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.api.elide.ElideNavigator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import com.google.common.eventbus.EventBus;
import io.netty.resolver.DefaultAddressResolverGroup;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.testfx.util.WaitForAsyncUtils;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FafApiAccessorTest extends ServiceTest {

  private FafApiAccessor instance;

  @Mock
  private EventBus eventBus;
  @Mock
  private OAuthTokenFilter oAuthTokenFilter;
  @TempDir
  public Path tempDirectory;

  private ResourceConverter resourceConverter;
  private MockWebServer mockApi;
  private final ReviewMapper reviewMapper = Mappers.getMapper(ReviewMapper.class);

  @AfterEach
  public void killServer() throws IOException {
    mockApi.shutdown();
  }

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(reviewMapper);
    ObjectMapper objectMapper = new ObjectMapper();
    resourceConverter = new JsonApiConfig().resourceConverter(objectMapper);
    JsonApiReader jsonApiReader = new JsonApiReader(resourceConverter);
    JsonApiWriter jsonApiWriter = new JsonApiWriter(resourceConverter);
    HttpClient httpClient = HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE);
    ReactorClientHttpConnector clientHttpConnector = new ReactorClientHttpConnector(httpClient);
    WebClient.Builder webClientBuilder = WebClient.builder()
        .clientConnector(clientHttpConnector)
        .codecs(clientCodecConfigurer -> {
          clientCodecConfigurer.customCodecs().register(jsonApiReader);
          clientCodecConfigurer.customCodecs().register(jsonApiWriter);
        });

    mockApi = new MockWebServer();
    mockApi.start();

    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getApi().setMaxPageSize(100);
    clientProperties.getApi().setBaseUrl(String.format("http://localhost:%s", mockApi.getPort()));
    instance = new FafApiAccessor(eventBus, clientProperties, oAuthTokenFilter, webClientBuilder);
    instance.afterPropertiesSet();
    instance.authorize();
  }

  private void prepareJsonApiResponse(Object object) throws Exception {
    byte[] serializedObject;
    if (object instanceof Iterable) {
      serializedObject = resourceConverter.writeDocumentCollection(new JSONAPIDocument<Iterable<?>>((Iterable<?>) object));
    } else if (object instanceof JSONAPIDocument) {
      serializedObject = resourceConverter.writeDocument((JSONAPIDocument<?>) object);
    } else {
      serializedObject = resourceConverter.writeDocument(new JSONAPIDocument<>(object));
    }
    mockApi.enqueue(new MockResponse()
        .setBody(new String(serializedObject))
        .addHeader("Content-Type", "application/vnd.api+json;charset=utf-8"));
  }

  private void prepareJsonApiBadRequest(List<Error> errors) throws Exception {
    byte[] serializedObject = resourceConverter.writeDocument(JSONAPIDocument.createErrorDocument(errors));
    mockApi.enqueue(new MockResponse()
        .setResponseCode(400)
        .setBody(new String(serializedObject))
        .addHeader("Content-Type", "application/vnd.api+json;charset=utf-8"));
  }

  private void prepareErrorResponse(int statusCode) throws Exception {
    mockApi.enqueue(new MockResponse().setResponseCode(statusCode));
  }

  private void prepareVoidResponse() throws Exception {
    mockApi.enqueue(new MockResponse());
  }

  @Test
  public void testGetMaxPageSize() {
    assertEquals(100, instance.getMaxPageSize());
  }

  @Test
  public void testSessionExpired() {
    instance.onSessionExpiredEvent(new SessionExpiredEvent());
    RuntimeException exception = assertThrows(RuntimeException.class, () -> WaitForAsyncUtils.waitForAsync(1000, () -> instance.getMe()));
    assertEquals(TimeoutException.class, exception.getCause().getClass());
  }

  @Test
  public void testLoggedOut() {
    instance.onLoggedOutEvent(new LoggedOutEvent());
    RuntimeException exception = assertThrows(RuntimeException.class, () -> WaitForAsyncUtils.waitForAsync(1000, () -> instance.getMe()));
    assertEquals(TimeoutException.class, exception.getCause().getClass());
  }

  @Test
  public void testUploadFile() throws Exception {
    Path tempFile = Files.createFile(tempDirectory.resolve("temp"));
    prepareVoidResponse();
    StepVerifier.create(instance.uploadFile("/", tempFile, (written, total) -> {
    }, Map.of())).verifyComplete();
  }

  @Test
  public void testPost() throws Exception {
    ReplayReviewBean reviewBean = ReplayReviewBeanBuilder.create().defaultValues().id(0).get();
    GameReview review = reviewMapper.map(reviewBean, new CycleAvoidingMappingContext());

    prepareJsonApiResponse(review);
    StepVerifier.create(instance.post(ElideNavigator.of(GameReview.class).collection(), review))
        .expectNext(review)
        .verifyComplete();
  }

  @Test
  public void testPatch() throws Exception {
    ReplayReviewBean reviewBean = ReplayReviewBeanBuilder.create().defaultValues().id(0).get();
    GameReview review = reviewMapper.map(reviewBean, new CycleAvoidingMappingContext());

    prepareVoidResponse();
    StepVerifier.create(instance.patch(ElideNavigator.of(review), review))
        .verifyComplete();
  }

  @Test
  public void testDelete() throws Exception {
    ReplayReviewBean reviewBean = ReplayReviewBeanBuilder.create().defaultValues().id(0).get();
    GameReview review = reviewMapper.map(reviewBean, new CycleAvoidingMappingContext());

    prepareVoidResponse();
    StepVerifier.create(instance.delete(ElideNavigator.of(review)))
        .verifyComplete();
  }

  @Test
  public void testGetOne() throws Exception {
    ReplayReviewBean reviewBean = ReplayReviewBeanBuilder.create().defaultValues().id(0).get();
    GameReview review = reviewMapper.map(reviewBean, new CycleAvoidingMappingContext());

    prepareJsonApiResponse(review);
    StepVerifier.create(instance.getOne(ElideNavigator.of(review)))
        .expectNext(review)
        .verifyComplete();
  }

  @Test
  public void testGetMe() throws Exception {
    MeResult meResult = new MeResult().setId("0");
    prepareJsonApiResponse(meResult);

    StepVerifier.create(instance.getMe())
        .expectNext(meResult)
        .verifyComplete();
  }

  @Test
  public void testGetManyNoNavigator() throws Exception {
    ReplayReviewBean reviewBean = ReplayReviewBeanBuilder.create().defaultValues().id(0).get();
    GameReview review = reviewMapper.map(reviewBean, new CycleAvoidingMappingContext());

    prepareJsonApiResponse(List.of(review));
    StepVerifier.create(instance.getMany(GameReview.class, "/data/gameReview", 1, Map.of("param", "test")))
        .expectNext(review)
        .verifyComplete();
    HttpUrl requestedUrl = mockApi.takeRequest().getRequestUrl();

    assertEquals("1", requestedUrl.queryParameter("page[size]"));
    assertEquals("1", requestedUrl.queryParameter("page[number]"));
    assertEquals("test", requestedUrl.queryParameter("param"));
  }

  @Test
  public void testGetManyNavigatorEnrichment() throws Exception {
    FafApiAccessor.FILTERS.forEach((clazz, filters) -> {
      try {
        prepareJsonApiResponse(List.of());

        StepVerifier.create(instance.getMany(ElideNavigator.of(clazz).collection()))
            .verifyComplete();

        HttpUrl requestedUrl = mockApi.takeRequest().getRequestUrl();
        assertThat(requestedUrl.queryParameter("filter"), containsString((String) new QBuilder().and(filters).query(new RSQLVisitor())));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    FafApiAccessor.INCLUDES.forEach((clazz, includes) -> {
      try {
        prepareJsonApiResponse(List.of());

        StepVerifier.create(instance.getMany(ElideNavigator.of(clazz).collection()))
            .verifyComplete();

        HttpUrl requestedUrl = mockApi.takeRequest().getRequestUrl();

        includes.forEach(include -> assertThat(requestedUrl.queryParameter("include"), containsString(include)));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testGetManyWithPageCountNavigatorEnrichment() throws Exception {
    FafApiAccessor.FILTERS.forEach((clazz, filters) -> {
      try {
        prepareJsonApiResponse(List.of());

        StepVerifier.create(instance.getManyWithPageCount(ElideNavigator.of(clazz).collection()))
            .expectNextCount(1)
            .verifyComplete();

        HttpUrl requestedUrl = mockApi.takeRequest().getRequestUrl();
        assertThat(requestedUrl.queryParameter("filter"), containsString((String) new QBuilder().and(filters).query(new RSQLVisitor())));
        assertThat(requestedUrl.query(), containsString("page[totals]"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    FafApiAccessor.INCLUDES.forEach((clazz, includes) -> {
      try {
        prepareJsonApiResponse(List.of());

        StepVerifier.create(instance.getManyWithPageCount(ElideNavigator.of(clazz).collection()))
            .expectNextCount(1)
            .verifyComplete();

        HttpUrl requestedUrl = mockApi.takeRequest().getRequestUrl();

        includes.forEach(include -> assertThat(requestedUrl.queryParameter("include"), containsString(include)));
        assertThat(requestedUrl.query(), containsString("page[totals]"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testGetManyWithCustomFilter() throws Exception {
    prepareJsonApiResponse(List.of());

    StepVerifier.create(instance.getMany(ElideNavigator.of(Game.class).collection(), "MyVeryOwnFilter"))
        .verifyComplete();

    HttpUrl requestedUrl = mockApi.takeRequest().getRequestUrl();
    assertThat(requestedUrl.queryParameter("filter"), containsString("MyVeryOwnFilter"));

    prepareJsonApiResponse(List.of());

    StepVerifier.create(instance.getMany(ElideNavigator.of(Game.class).collection().setFilter(qBuilder().intNum("test").eq(1)), "MyVeryOwnFilter"))
        .verifyComplete();

    requestedUrl = mockApi.takeRequest().getRequestUrl();
    assertThat(requestedUrl.queryParameter("filter"), containsString("MyVeryOwnFilter"));
  }

  @Test
  public void testGetManyWithPageTotalWithCustomFilter() throws Exception {
    prepareJsonApiResponse(List.of());

    StepVerifier.create(instance.getManyWithPageCount(ElideNavigator.of(Game.class).collection(), "MyVeryOwnFilter"))
        .expectNextCount(1)
        .verifyComplete();

    HttpUrl requestedUrl = mockApi.takeRequest().getRequestUrl();
    assertThat(requestedUrl.queryParameter("filter"), containsString("MyVeryOwnFilter"));
  }

  @Test
  public void testGetManyBadRequest() throws Exception {
    Error error = new Error();
    error.setId("0");
    error.setStatus("test");
    error.setCode("400");
    prepareJsonApiBadRequest(List.of(error));

    StepVerifier.create(instance.getManyWithPageCount(ElideNavigator.of(Game.class).collection()))
        .verifyError(ApiException.class);
  }

  @Test
  public void test4xxError() throws Exception {
    prepareErrorResponse(404);

    StepVerifier.create(instance.getManyWithPageCount(ElideNavigator.of(Game.class).collection()))
        .verifyError();

    prepareErrorResponse(404);

    StepVerifier.create(instance.getMany(ElideNavigator.of(Game.class).collection()))
        .verifyError();
  }

  @Test
  public void test5xxError() throws Exception {
    prepareErrorResponse(500);

    StepVerifier.create(instance.getManyWithPageCount(ElideNavigator.of(Game.class).collection()))
        .verifyError();

    prepareErrorResponse(500);

    StepVerifier.create(instance.getMany(ElideNavigator.of(Game.class).collection()))
        .verifyError();
  }

  @Test
  public void testUnknownError() throws Exception {
    prepareErrorResponse(600);

    StepVerifier.create(instance.getManyWithPageCount(ElideNavigator.of(Game.class).collection()))
        .verifyError();

    prepareErrorResponse(600);

    StepVerifier.create(instance.getMany(ElideNavigator.of(Game.class).collection()))
        .verifyError();
  }
}
