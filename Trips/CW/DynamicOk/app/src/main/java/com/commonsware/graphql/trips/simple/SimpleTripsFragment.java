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

package com.commonsware.graphql.trips.simple;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SimpleTripsFragment extends Fragment {
  private static final MediaType MEDIA_TYPE_JSON
    =MediaType.parse("application/json; charset=utf-8");
  private static final String QUERY="query";
  private static final String DOCUMENT=
    "{ allTrips { id title startTime priority duration creationTime } }";
  private static final String ENDPOINT=
    "https://graphql-demo.commonsware.com/0.1/graphql";
  private Observable<String> observable;
  private Disposable sub;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);

    observable=Observable
      .defer(new Callable<ObservableSource<String>>() {
        @Override
        public ObservableSource<String> call() throws Exception {
          return(Observable.just(query()));
        }
      })
      .subscribeOn(Schedulers.io())
      .map(this::prettify)
      .observeOn(AndroidSchedulers.mainThread())
      .cache();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return(inflater.inflate(R.layout.main, container, false));
  }

  @Override
  public void onViewCreated(View v, Bundle savedInstanceState) {
    super.onViewCreated(v, savedInstanceState);

    ((TextView)v.findViewById(R.id.result)).setHorizontallyScrolling(true);

    unsub();
    sub=observable.subscribe(
      this::updateText,
      error -> Toast
        .makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG)
        .show()
    );
  }

  @Override
  public void onDestroy() {
    unsub();

    super.onDestroy();
  }

  private void unsub() {
    if (sub!=null && !sub.isDisposed()) {
      sub.dispose();
    }
  }

  private void updateText(String text) {
    ((TextView)getView().findViewById(R.id.result)).setText(text);
  }

  private String prettify(String raw) {
    Gson gson=new GsonBuilder().setPrettyPrinting().create();
    JsonElement json=new JsonParser().parse(raw);

    return(gson.toJson(json));
  }

  private String query() throws IOException {
    HashMap<String, String> payload=new HashMap<>();

    payload.put(QUERY, DOCUMENT);

    String body=new Gson().toJson(payload);
    Request request=new Request.Builder()
      .url(ENDPOINT)
      .post(RequestBody.create(MEDIA_TYPE_JSON, body))
      .build();
    Response response=new OkHttpClient().newCall(request).execute();

    return(response.body().string());
  }
}
