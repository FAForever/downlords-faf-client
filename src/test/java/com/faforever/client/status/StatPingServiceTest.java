package com.faforever.client.status;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.test.ServiceTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class StatPingServiceTest extends ServiceTest {

  private StatPingService instance;
  private MockWebServer mockApi;

  @BeforeEach
  void setUp() throws Exception {
    mockApi = new MockWebServer();
    mockApi.start();

    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getStatping().setApiRoot(String.format("http://localhost:%s", mockApi.getPort()));

    instance = new StatPingService(clientProperties, WebClient.builder());
  }

  @AfterEach
  public void tearDown() throws IOException {
    mockApi.shutdown();
  }

  @Test
  void getServices() {
    mockApi.enqueue(new MockResponse()
        .setBody("""
            [
              {
                "avg_response": 28037,
                "check_interval": 60,
                "created_at": "2021-08-26T01:32:30.186659925Z",
                "failures_24_hours": 1151,
                "group_id": 1,
                "id": 1,
                "incidents": [
                  {
                    "id": 1,
                    "title": "Test incident",
                    "description": "Test description.",
                    "service": 1,
                    "created_at": "2021-08-27T11:12:35.357173973Z",
                    "updated_at": "2021-08-27T13:50:18.706074801Z",
                    "updates": [
                      {
                        "id": 1,
                        "message": "We're not yet sure what it is.",
                        "type": "Investigating",
                        "created_at": "2021-08-27T11:13:05.669821757Z",
                        "updated_at": "2021-08-27T13:50:18.706216348Z"
                      }
                    ]
                  }
                ],
                "last_error": "2021-08-29T19:49:25.090685775Z",
                "last_success": "2021-08-27T13:49:36.253862928Z",
                "latency": 26823,
                "messages": [],
                "name": "Lobby",
                "online": false,
                "online_24_hours": 0,
                "online_7_days": 0,
                "order_id": 0,
                "permalink": "lobby",
                "ping_time": 3514,
                "public": true,
                "stats": {
                  "failures": 2592,
                  "hits": 2180,
                  "first_hit": "2021-08-26T01:32:30.318803856Z"
                },
                "status_code": 0,
                "updated_at": "2021-08-27T13:50:18.705341762Z"
              },
              {
                "avg_response": 198004,
                "check_interval": 60,
                "created_at": "2021-08-26T01:32:30.286840197Z",
                "failures_24_hours": 5,
                "group_id": 1,
                "id": 2,
                "incidents": [],
                "last_error": "2021-08-28T22:02:30.727645833Z",
                "last_success": "2021-08-29T19:49:30.49469974Z",
                "latency": 196995,
                "messages": [],
                "name": "API",
                "online": true,
                "online_24_hours": 99.65,
                "online_7_days": 99.91,
                "order_id": 0,
                "permalink": "api",
                "ping_time": 4037,
                "public": true,
                "stats": {
                  "failures": 5,
                  "hits": 5413,
                  "first_hit": "2021-08-26T01:32:30.81848813Z"
                },
                "status_code": 200,
                "updated_at": "2021-08-26T01:32:30.286840197Z"
              }
            ]
                        """)
        .addHeader("Content-Type", "application/json;charset=utf-8"));

    List<Service> result = instance.getServices().collectList().block();
    assertThat(result, hasSize(2));
    assertThat(result.get(0).getName(), is("Lobby"));
    assertThat(result.get(1).getName(), is("API"));
  }

  @Test
  void getMessages() {
    mockApi.enqueue(new MockResponse()
        .setBody("""
            [
              {
                "created_at": "2021-08-29T19:54:56.968067873Z",
                "description": "Test description.",
                "end_on": "2021-08-30T19:54:00Z",
                "id": 6,
                "service": 1,
                "start_on": "2021-08-29T19:54:00Z",
                "title": "Test Title",
                "updated_at": "2021-08-29T19:54:56.968067873Z"
              },
              {
                "created_at": "2021-08-29T19:58:56.968067873Z",
                "description": "Test description 2.",
                "end_on": "2021-08-30T19:54:00Z",
                "id": 6,
                "service": 1,
                "start_on": "2021-08-30T19:54:00Z",
                "title": "Test Title 2",
                "updated_at": "2021-08-31T19:54:56.968067873Z"
              }
            ]
            """)
        .addHeader("Content-Type", "application/json;charset=utf-8"));

    List<Message> result = instance.getMessages().collectList().block();
    assertThat(result, hasSize(2));
    assertThat(result.get(0).getTitle(), is("Test Title"));
    assertThat(result.get(1).getTitle(), is("Test Title 2"));
  }
}