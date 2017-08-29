/***
 Copyright (c) 2012-17 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 From _GraphQL and Android_
 https://commonsware.com/GraphQL
 */

package com.commonsware.graphql.trips.simple.statictest.test;

import android.support.test.runner.AndroidJUnit4;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.commonsware.graphql.trips.api.GetAllTrips;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import java.io.IOException;
import java.util.ArrayList;
import io.reactivex.schedulers.Schedulers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;

@RunWith(AndroidJUnit4.class)
public class TripTests {
  private static final String MOCK_RESPONSE="{\n"+
    "  \"data\": {\n"+
    "    \"allTrips\": [\n"+
    "      {\n"+
    "        \"id\": \"2c494055-78bc-430c-9ab7-19817f3fc060\",\n"+
    "        \"title\": \"Vacation!\",\n"+
    "        \"startTime\": \"2017-12-20T13:14:00-05:00\",\n"+
    "        \"priority\": \"MEDIUM\",\n"+
    "        \"duration\": 10080,\n"+
    "        \"creationTime\": \"2017-06-15T23:59:43.190Z\",\n"+
    "        \"__typename\": \"Trip\"\n"+
    "      },\n"+
    "      {\n"+
    "        \"id\": \"e323fed5-6805-4bcf-8cb6-8b7a5014a9d9\",\n"+
    "        \"title\": \"Business Trip\",\n"+
    "        \"startTime\": \"2018-01-14T11:45:00-05:00\",\n"+
    "        \"priority\": \"HIGH\",\n"+
    "        \"duration\": 4320,\n"+
    "        \"creationTime\": \"2017-06-15T23:59:43.190Z\",\n"+
    "        \"__typename\": \"Trip\"\n"+
    "      }\n"+
    "    ]\n"+
    "  }\n"+
    "}";

  @Test
  public void realServer() throws InterruptedException {
    ApolloClient apolloClient=ApolloClient.builder()
      .okHttpClient(new OkHttpClient())
      .serverUrl("https://graphql-demo.commonsware.com/0.3/graphql")
      .build();

    assertResponse(Rx2Apollo.from(apolloClient.query(new GetAllTrips()).watcher())
      .subscribeOn(Schedulers.io())
      .blockingFirst());
  }

  @Test
  public void mockResponse() {
    GetAllTrips.AllTrip firstTrip=Mockito.mock(GetAllTrips.AllTrip.class);
    GetAllTrips.AllTrip secondTrip=Mockito.mock(GetAllTrips.AllTrip.class);
    ArrayList<GetAllTrips.AllTrip> allTrips=new ArrayList<>();

    allTrips.add(firstTrip);
    allTrips.add(secondTrip);

    GetAllTrips.Data data=Mockito.mock(GetAllTrips.Data.class);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        return(allTrips);
      }
    }).when(data).allTrips();

    assertData(data);
  }

  @Test
  public void mockServer() throws IOException, InterruptedException {
    MockWebServer server=new MockWebServer();

    server.enqueue(new MockResponse().setBody(MOCK_RESPONSE));
    server.start();

    try {
      HttpUrl url=server.url("/0.3/graphql");
      ApolloClient apolloClient=ApolloClient.builder()
        .okHttpClient(new OkHttpClient())
        .serverUrl(url.toString())
        .build();

      assertResponse(Rx2Apollo.from(apolloClient.query(new GetAllTrips()).watcher())
        .subscribeOn(Schedulers.io())
        .blockingFirst());
    }
    finally {
      server.shutdown();
    }
  }

  private void assertResponse(Response<GetAllTrips.Data> response) {
    assertData(response.data());
    assertEquals(0, response.errors().size());
  }

  private void assertData(GetAllTrips.Data data) {
    assertEquals(2, data.allTrips().size());
  }
}
