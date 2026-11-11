package com.faforever.client.fa;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.update.ClientConfiguration.ServerEndpoints;
import com.faforever.client.update.ClientConfiguration.UrlEndpoint;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DesyncReportsTest extends ServiceTest {

  private static final String PRODUCT_NAME = RebuiltEngineExecutable.PRODUCT_NAME;

  @TempDir
  public Path binDirectory;

  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private DataPrefs dataPrefs;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;

  private ForgedAlliancePrefs forgedAlliancePrefs;
  private ClientProperties clientProperties;
  private DesyncReports instance;

  @BeforeEach
  public void setUp() {
    forgedAlliancePrefs = new ForgedAlliancePrefs();
    clientProperties = new ClientProperties();
    lenient().when(dataPrefs.getBinDirectory()).thenReturn(binDirectory);
    lenient().when(i18n.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    instance = new DesyncReports(fafApiAccessor, forgedAlliancePrefs, dataPrefs, notificationService, i18n,
                                 clientProperties);
  }

  private void agreeWithToken(String token) {
    forgedAlliancePrefs.setAskedSendDesyncReports(true);
    forgedAlliancePrefs.setSendDesyncReports(true);
    when(fafApiAccessor.postJsonForObject(eq("/ice/session/token"), any(), eq(DesyncReports.TokenResponse.class)))
        .thenReturn(Mono.just(new DesyncReports.TokenResponse(token)));
  }

  @Test
  public void testPreparedArgumentsAreReusedForTheSameGame() throws Exception {
    useExe(PRODUCT_NAME);
    agreeWithToken("t");

    List<String> prepared = instance.prepare(42).get();
    assertEquals(List.of("/telemetry", "/telemetrytoken", "t"), prepared);
    // The launch, and a return to the same game after a crash, take that result without asking icebreaker again.
    assertEquals(prepared, instance.launchArguments(42));
    assertEquals(prepared, instance.launchArguments(42));
    verify(fafApiAccessor).postJsonForObject(eq("/ice/session/token"), eq(Map.of("gameId", 42)),
                                             eq(DesyncReports.TokenResponse.class));
  }

  @Test
  public void testTheServersEndpointsArePassed() throws Exception {
    useExe(PRODUCT_NAME);
    agreeWithToken("t");
    ServerEndpoints server = new ServerEndpoints();
    server.setName("Test server");
    UrlEndpoint records = new UrlEndpoint();
    records.setUrl("https://reports.example/v1/logs");
    UrlEndpoint dumps = new UrlEndpoint();
    dumps.setUrl("https://reports.example/dumps");
    server.setDesyncRecords(records);
    server.setDesyncDumps(dumps);
    clientProperties.updateFromEndpoint(server);

    assertEquals(List.of("/telemetry", "/telemetrytoken", "t", "/telemetryendpoint", "https://reports.example/v1/logs",
                         "/telemetrydumpendpoint", "https://reports.example/dumps"), instance.launchArguments(42));

    // Another server without them: the game's own default again.
    ServerEndpoints other = new ServerEndpoints();
    other.setName("Main server");
    clientProperties.updateFromEndpoint(other);
    assertEquals(List.of("/telemetry", "/telemetrytoken", "t"), instance.launchArguments(43));
  }

  private void useExe(String productName) throws Exception {
    TestExecutables.write(binDirectory.resolve("ForgedAlliance.exe"), true, productName);
  }

  @Test
  public void testConsentWithdrawnAfterPreparingSendsNothing() throws Exception {
    useExe(PRODUCT_NAME);
    agreeWithToken("t");
    assertEquals(List.of("/telemetry", "/telemetrytoken", "t"), instance.prepare(42).get());

    forgedAlliancePrefs.setSendDesyncReports(false);

    // A return to the same game after a crash, with the setting now off.
    assertEquals(List.of(), instance.launchArguments(42));
  }

  @Test
  public void testATokenRequestPastTheDeadlineSendsNothing() throws Exception {
    useExe(PRODUCT_NAME);
    forgedAlliancePrefs.setAskedSendDesyncReports(true);
    forgedAlliancePrefs.setSendDesyncReports(true);
    instance.deadlineMillis = 200;
    // As when the request first waits for the client's sign-in.
    when(fafApiAccessor.postJsonForObject(eq("/ice/session/token"), any(), eq(DesyncReports.TokenResponse.class)))
        .thenAnswer(invocation -> {
          Thread.sleep(3000);
          return Mono.just(new DesyncReports.TokenResponse("late"));
        });

    long start = System.nanoTime();
    assertEquals(List.of(), instance.prepare(42).get(2, java.util.concurrent.TimeUnit.SECONDS));
    assertTrue(System.nanoTime() - start < 2_000_000_000L);
  }

  @Test
  public void testAFailureWhilePreparingSendsNothing() throws Exception {
    when(dataPrefs.getBinDirectory()).thenThrow(new IllegalStateException("no data folder"));

    assertEquals(List.of(), instance.prepare(42).get());
  }

  @Test
  public void testNoAnswerFromIcebreakerSendsNothing() throws Exception {
    useExe(PRODUCT_NAME);
    forgedAlliancePrefs.setAskedSendDesyncReports(true);
    forgedAlliancePrefs.setSendDesyncReports(true);
    when(fafApiAccessor.postJsonForObject(eq("/ice/session/token"), any(), eq(DesyncReports.TokenResponse.class)))
        .thenReturn(Mono.empty());

    assertEquals(List.of(), instance.launchArguments(42));
  }

  @Test
  public void testABlankTokenSendsNothing() throws Exception {
    useExe(PRODUCT_NAME);
    agreeWithToken(" ");

    assertEquals(List.of(), instance.launchArguments(42));
  }

  @Test
  public void testBlankEndpointsLeaveTheGamesDefault() throws Exception {
    useExe(PRODUCT_NAME);
    agreeWithToken("t");
    ServerEndpoints server = new ServerEndpoints();
    server.setName("Test server");
    UrlEndpoint records = new UrlEndpoint();
    records.setUrl(" ");
    UrlEndpoint dumps = new UrlEndpoint();
    dumps.setUrl("");
    server.setDesyncRecords(records);
    server.setDesyncDumps(dumps);
    clientProperties.updateFromEndpoint(server);

    assertEquals(List.of("/telemetry", "/telemetrytoken", "t"), instance.launchArguments(42));
  }

  @Test
  public void testTheQuestionsAnswersAreStored() throws Exception {
    useExe(PRODUCT_NAME);
    instance.launchArguments(42);
    ArgumentCaptor<PersistentNotification> asked = ArgumentCaptor.forClass(PersistentNotification.class);
    verify(notificationService).addNotification(asked.capture());

    asked.getValue().actions().get(0).run();
    assertTrue(forgedAlliancePrefs.isSendDesyncReports());
    asked.getValue().actions().get(1).run();
    assertFalse(forgedAlliancePrefs.isSendDesyncReports());
  }

  @Test
  public void testOriginalExeGetsNothingAndNobodyIsAsked() throws Exception {
    useExe("Supreme Commander Forged Alliance");
    forgedAlliancePrefs.setAskedSendDesyncReports(true);
    forgedAlliancePrefs.setSendDesyncReports(true);

    assertEquals(List.of(), instance.launchArguments(42));
    verify(fafApiAccessor, never()).postJsonForObject(anyString(), any(), any());
  }

  @Test
  public void testFirstRebuiltEngineGameAsksOnceAndSendsNothing() throws Exception {
    useExe(PRODUCT_NAME);

    assertEquals(List.of(), instance.launchArguments(42));
    assertTrue(forgedAlliancePrefs.isAskedSendDesyncReports());
    ArgumentCaptor<PersistentNotification> asked = ArgumentCaptor.forClass(PersistentNotification.class);
    verify(notificationService).addNotification(asked.capture());
    assertEquals("game.desyncReports.ask", asked.getValue().text());

    // The second game doesn't ask again; the player hasn't agreed, so it sends nothing.
    assertEquals(List.of(), instance.launchArguments(43));
    verify(notificationService).addNotification(any(PersistentNotification.class));
    verify(fafApiAccessor, never()).postJsonForObject(anyString(), any(), any());
  }

  @Test
  public void testAgreedPlayerGetsConsentAndTheGamesToken() throws Exception {
    useExe(PRODUCT_NAME);
    forgedAlliancePrefs.setAskedSendDesyncReports(true);
    forgedAlliancePrefs.setSendDesyncReports(true);
    when(fafApiAccessor.postJsonForObject(eq("/ice/session/token"), eq(Map.of("gameId", 42)),
                                          eq(DesyncReports.TokenResponse.class)))
        .thenReturn(Mono.just(new DesyncReports.TokenResponse("header.payload.signature")));

    assertEquals(List.of("/telemetry", "/telemetrytoken", "header.payload.signature"), instance.launchArguments(42));
  }

  @Test
  public void testNoTokenMeansNoReports() throws Exception {
    useExe(PRODUCT_NAME);
    forgedAlliancePrefs.setAskedSendDesyncReports(true);
    forgedAlliancePrefs.setSendDesyncReports(true);
    when(fafApiAccessor.postJsonForObject(anyString(), any(), eq(DesyncReports.TokenResponse.class)))
        .thenReturn(Mono.error(new IllegalStateException("icebreaker is down")));

    assertEquals(List.of(), instance.launchArguments(42));
  }

  @Test
  public void testDeclinedPlayerSendsNothing() throws Exception {
    useExe(PRODUCT_NAME);
    forgedAlliancePrefs.setAskedSendDesyncReports(true);
    forgedAlliancePrefs.setSendDesyncReports(false);

    assertEquals(List.of(), instance.launchArguments(42));
    verify(fafApiAccessor, never()).postJsonForObject(anyString(), any(), any());
  }

  @Test
  public void testTheLoggedCommandHidesTheToken() {
    assertEquals(List.of("game.exe", "/telemetry", "/telemetrytoken", "<token>", "/nativenet"),
                 DesyncReports.withTokenHidden(List.of("game.exe", "/telemetry", "/telemetrytoken", "secret", "/nativenet")));
  }
}
