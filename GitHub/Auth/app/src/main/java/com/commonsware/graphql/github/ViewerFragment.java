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

package com.commonsware.graphql.github;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.commonsware.graphql.github.api.Whoami;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ViewerFragment extends RecyclerViewFragment {
  private final OkHttpClient ok=new OkHttpClient.Builder()
    .addInterceptor(chain -> {
      Request orig=chain.request();
      Request.Builder builder=orig.newBuilder()
        .method(orig.method(), orig.body())
        .header("Authorization", "bearer "+BuildConfig.GITHUB_TOKEN);

      return(chain.proceed(builder.build()));
    })
    .build();
  private ApolloClient apolloClient=ApolloClient.builder()
    .okHttpClient(ok)
    .serverUrl("https://api.github.com/graphql")
    .build();
  private Observable<Whoami.Data> observable;
  private Disposable sub;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);

    observable=Rx2Apollo.from(apolloClient.query(new Whoami()).watcher())
      .subscribeOn(Schedulers.io())
      .map(response -> (getFields(response)))
      .cache()
      .observeOn(AndroidSchedulers.mainThread());
  }

  @Override
  public void onViewCreated(View v, Bundle savedInstanceState) {
    super.onViewCreated(v, savedInstanceState);

    setLayoutManager(new LinearLayoutManager(getActivity()));

    getRecyclerView()
      .addItemDecoration(new DividerItemDecoration(getActivity(),
        LinearLayoutManager.VERTICAL));

    unsub();
    sub=observable.subscribe(
      s -> setLogin(s.viewer().login()),
      error -> {
        Toast
          .makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG)
          .show();
        Log.e(getClass().getSimpleName(), "Exception processing request",
          error);
      });
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

  private Whoami.Data getFields(Response<Whoami.Data> response) {
    if (response.hasErrors()) {
      throw new RuntimeException(response.errors().get(0).message());
    }

    return(response.data());
  }

  private void setLogin(String login) {
    ((MainActivity)getActivity()).setLogin(login);
  }
}
