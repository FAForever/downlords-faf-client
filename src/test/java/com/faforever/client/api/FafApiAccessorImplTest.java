package com.faforever.client.api;

public class FafApiAccessorImplTest {
//
//  @Rule
//  public TemporaryFolder preferencesDirectory = new TemporaryFolder();
//
//  private FafApiAccessorImpl instance;
//
//  @Mock
//  private PreferencesService preferencesService;
//  @Mock
//  private UserService userService;
//  @Mock
//  private EventBus eventBus;
//
//  @Before
//  public void setUp() throws Exception {
//    MockitoAnnotations.initMocks(this);
//
//    ClientProperties clientProperties = new ClientProperties();
//    clientProperties.getApi()
//        .setBaseUrl("http://api.example.com");
//
//    instance = new FafApiAccessorImpl(clientProperties, eventBus);
//
//    when(preferencesService.getPreferencesDirectory()).thenReturn(preferencesDirectory.getRoot().toPath());
//    when(userService.getUserId()).thenReturn(123);
//    when(userService.getUsername()).thenReturn("junit");
//    when(userService.getPassword()).thenReturn("42");
//
//    instance.postConstruct();
//  }
//
//  @Test
//  public void testGetPlayerAchievements() throws Exception {
//    mockResponse("{'data': [" +
//        " {" +
//        "   'id': '1'," +
//        "   'attributes': {'achievement_id': '1-2-3'}" +
//        " }," +
//        " {" +
//        "   'id': '2'," +
//        "   'attributes': {'achievement_id': '2-3-4'}" +
//        " }" +
//        "]}");
//
//    PlayerAchievement playerAchievement1 = new PlayerAchievement();
//    playerAchievement1.setId("1");
//    playerAchievement1.setAchievementId("1-2-3");
//    PlayerAchievement playerAchievement2 = new PlayerAchievement();
//    playerAchievement2.setId("2");
//    playerAchievement2.setAchievementId("2-3-4");
//    List<PlayerAchievement> result = Arrays.asList(playerAchievement1, playerAchievement2);
//
//    assertThat(instance.getPlayerAchievements(123), is(result));
//    verify(restOperations).getForObject("http://api.example.com/players/123/achievements", String.class);
//  }
//
//  @Test
//  public void testGetAchievementDefinitions() throws Exception {
//    mockResponse("{'data': [" +
//        " {" +
//        "   'id': '1-2-3'," +
//        "   'attributes': {}" +
//        " }," +
//        " {" +
//        "   'id': '2-3-4'," +
//        "   'attributes': {}" +
//        " }" +
//        "]}");
//
//    AchievementDefinition achievementDefinition1 = new AchievementDefinition();
//    achievementDefinition1.setId("1-2-3");
//    AchievementDefinition achievementDefinition2 = new AchievementDefinition();
//    achievementDefinition2.setId("2-3-4");
//    List<AchievementDefinition> result = Arrays.asList(achievementDefinition1, achievementDefinition2);
//
//    assertThat(instance.getAchievementDefinitions(), is(result));
//    verify(restOperations).getForObject("http://api.example.com/achievements?sort=order", String.class);
//  }
//
//  @Test
//  public void testGetAchievementDefinition() throws Exception {
//    AchievementDefinition achievementDefinition = new AchievementDefinition();
//    achievementDefinition.setId("1-2-3");
//
//    mockResponse("{'data': " +
//        " {" +
//        "   'id': '1-2-3'," +
//        "   'attributes': {}" +
//        " }" +
//        "}");
//
//    assertThat(instance.getAchievementDefinition("123"), is(achievementDefinition));
//    verify(restOperations).getForObject("http://api.example.com/achievements/123", String.class);
//  }
//
//  @Test
//  public void testGetPlayerEvents() throws Exception {
//    mockResponse("{'data': [" +
//        " {" +
//        "   'id': '1'," +
//        "   'attributes': {'count': 11, 'event_id': '1-1-1' }" +
//        " }," +
//        " {" +
//        "   'id': '2'," +
//        "   'attributes': {'count': 22, 'event_id': '2-2-2' }" +
//        " }" +
//        "]}");
//
//    PlayerEvent playerEvent1 = new PlayerEvent();
//    playerEvent1.setId("1");
//    playerEvent1.setEventId("1-1-1");
//    playerEvent1.setCount(11);
//    PlayerEvent playerEvent2 = new PlayerEvent();
//    playerEvent2.setId("2");
//    playerEvent2.setEventId("2-2-2");
//    playerEvent2.setCount(22);
//
//    List<PlayerEvent> result = Arrays.asList(playerEvent1, playerEvent2);
//
//    assertThat(instance.getPlayerEvents(123), is(result));
//    verify(restOperations).getForObject("http://api.example.com/players/123/events", String.class);
//  }
//
//  @Test
//  public void testGetMods() throws Exception {
//    mockResponse("{'data': [" +
//            " {" +
//            "   'id': '1'," +
//            "   'attributes': {" +
//            "     'create_time': '2011-12-03T10:15:30'," +
//            "     'version': '1'," +
//            "     'download_url': 'http://example.com/mod1.zip'" +
//            "   }" +
//            " }," +
//            " {" +
//            "   'id': '2'," +
//            "   'attributes': {" +
//            "     'create_time': '2011-12-03T10:15:30'," +
//            "     'version': '1'," +
//            "     'download_url': 'http://example.com/mod2.zip'" +
//            "   }" +
//            " }" +
//            "]}",
//        "{'data': []}");
//
//    List<Mod> result = Arrays.asList(
//        ModInfoBeanBuilder.create().defaultValues().uid("1").get(),
//        ModInfoBeanBuilder.create().defaultValues().uid("2").get()
//    );
//
//    assertThat(instance.getMods(), equalTo(result));
//    verify(restOperations).getForObject("http://api.example.com/mods", String.class);
//  }
//
//  @Test
//  public void testGetRanked1v1Entries() throws Exception {
//    mockResponse("{'data': [" +
//            " {" +
//            "   'id': '1'," +
//            "   'attributes': {" +
//            "     'login': 'user1'," +
//            "     'num_games': 5" +
//            "   }" +
//            " }," +
//            " {" +
//            "   'id': '2'," +
//            "   'attributes': {" +
//            "     'login': 'user2'," +
//            "     'num_games': 3" +
//            "   }" +
//            " }" +
//            "]}",
//        "{'data': []}");
//
//    List<Ranked1v1EntryBean> result = Arrays.asList(
//        Ranked1v1EntryBeanBuilder.create().defaultValues().username("user1").get(),
//        Ranked1v1EntryBeanBuilder.create().defaultValues().username("user2").get()
//    );
//
//    assertThat(instance.getLeaderboardEntries(RatingType.LADDER_1V1), equalTo(result));
//    verify(restOperations).getForObject("http://api.example.com/leaderboards/1v1", String.class);
//  }
//
//  @Test
//  public void testGetRanked1v1Stats() throws Exception {
//    mockResponse("{'data': [" +
//            " {" +
//            "   'id': '/leaderboards/1v1/stats'," +
//            "   'attributes': {" +
//            "     '100': 1," +
//            "     '1200': 5," +
//            "     '1400': 5" +
//            "   }" +
//            " }" +
//            "]}",
//        "{'data': []}");
//
//    Ranked1v1Stats ranked1v1Stats = new Ranked1v1Stats();
//    ranked1v1Stats.setId("/leaderboards/1v1/stats");
//
//    assertThat(instance.getRanked1v1Stats(), equalTo(ranked1v1Stats));
//    verify(restOperations).getForObject("http://api.example.com/leaderboards/1v1/stats", String.class);
//  }
//
//  @Test
//  public void testGetRanked1v1EntryForPlayer() throws Exception {
//    mockResponse("{'data': [" +
//            " {" +
//            "   'id': '2'," +
//            "   'attributes': {" +
//            "     'login': 'user1'," +
//            "     'num_games': 3" +
//            "   }" +
//            " }" +
//            "]}",
//        "{'data': []}");
//
//    Ranked1v1EntryBean entry = Ranked1v1EntryBeanBuilder.create().defaultValues().username("user1").get();
//
//    assertThat(instance.getRanked1v1EntryForPlayer(123), equalTo(entry));
//    verify(restOperations).getForObject("http://api.example.com/leaderboards/1v1/123", String.class);
//  }
//
//  @Test
//  public void testGetRatingHistoryGlobal() throws Exception {
//    mockResponse("{" +
//            "  'data': {" +
//            "    'attributes': {" +
//            "      'history': {" +
//            "        '1469921413': [1026.62, 49.4094]," +
//            "        '1469989967': [1024.01, 49.4545]," +
//            "        '1470842200': [1020.65, 50.1963]" +
//            "      }" +
//            "    }," +
//            "    'id': '21447'," +
//            "    'type': 'leaderboard_history'" +
//            "  }" +
//            "}",
//        "{'data': []}");
//
//    History ratingHistory = instance.getRatingHistory(RatingType.GLOBAL, 123);
//
//    verify(restOperations).getForObject("http://api.example.com/players/123/ratings/global/history", String.class);
//    assertThat(ratingHistory.getData().values(), hasSize(3));
//    assertThat(ratingHistory.getData().get("1469921413").get(0), is(1026.62f));
//    assertThat(ratingHistory.getData().get("1469921413").get(1), is(49.4094f));
//  }
//
//  @Test
//  public void testGetRatingHistory1v1() throws Exception {
//    mockResponse("{" +
//            "  'data': {" +
//            "    'attributes': {" +
//            "      'history': {" +
//            "        '1469921413': [1026.62, 49.4094]," +
//            "        '1469989967': [1024.01, 49.4545]," +
//            "        '1470842200': [1020.65, 50.1963]" +
//            "      }" +
//            "    }," +
//            "    'id': '21447'," +
//            "    'type': 'leaderboard_history'" +
//            "  }" +
//            "}",
//        "{'data': []}");
//
//    History ratingHistory = instance.getRatingHistory(RatingType.LADDER_1V1, 123);
//
//    verify(restOperations).getForObject("http://api.example.com/players/123/ratings/1v1/history", String.class);
//    assertThat(ratingHistory.getData().values(), hasSize(3));
//    assertThat(ratingHistory.getData().get("1469921413").get(0), is(1026.62f));
//    assertThat(ratingHistory.getData().get("1469921413").get(1), is(49.4094f));
//  }
//
//  @Test
//  public void testUploadMod() throws Exception {
//    Path file = Files.createTempFile("foo", null);
//
//    // FIXME filename
//    instance.uploadMod(file, (written, total) -> {
//    });
//
//    verify(restOperations).getForObject("http://api.example.com/mods/upload", String.class);
//  }
//
//  @Test
//  public void testChangePassword() throws Exception {
//    instance.changePassword("junit", "currentPasswordHash", "newPasswordHash");
//
//    verify(restOperations).getForObject("http://api.example.com/users/change_password", String.class);
//  }
//
//  @Test
//  public void testGetCoopMissions() throws Exception {
//    mockResponse("{'data': [" +
//            " {" +
//            "   'id': '111'," +
//            "   'attributes': {" +
//            "     'name': 'Sample Mission'," +
//            "     'description': 'Sample description'," +
//            "     'category': 'UEF'," +
//            "     'download_url': 'http://content.example.com/mission.zip'," +
//            "     'thumbnail_url_small': 'http://content.example.com/small.png'," +
//            "     'thumbnail_url_large': 'http://content.example.com/large.png'," +
//            "     'version': 5" +
//            "   }" +
//            " }" +
//            "]}",
//        "{'data': []}");
//
//    List<CoopMission> coopMissions = instance.getCoopMissions();
//    CoopMission result = coopMissions.get(0);
//
//    assertThat(result.getName(), is("Sample Mission"));
//    assertThat(result.getDescription(), is("Sample description"));
//    assertThat(result.getCategory(), is(CoopCategory.UEF));
//    assertThat(result.getVersion(), is(5));
//    assertThat(result.getDownloadUrl(), is("http://content.example.com/mission.zip"));
//    assertThat(result.getThumbnailUrlSmall(), is("http://content.example.com/small.png"));
//    assertThat(result.getThumbnailUrlLarge(), is("http://content.example.com/large.png"));
//
//    verify(restOperations).getForObject("http://http://api.example.com/coop/missions", String.class);
//  }
//
//  private void mockResponse(String... responses) throws IOException {
//    OngoingStubbing<String> ongoingStubbing = when(restOperations.getForObject(any(), eq(String.class)));
//    for (String string : responses) {
//      ongoingStubbing = ongoingStubbing.thenReturn(string);
//    }
//  }
}
