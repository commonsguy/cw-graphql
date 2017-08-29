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

import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.commonsware.graphql.github.api.MyStars;
import com.commonsware.graphql.github.api.type.CustomType;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
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
    .addCustomTypeAdapter(CustomType.DATETIME, new ISO8601Adapter())
    .serverUrl("https://api.github.com/graphql")
    .build();
  private Observable<MyStars.Data> observable;
  private Disposable sub;
  private RepoAdapter repoMan;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);

    observable=getPages()
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
    repoMan=new RepoAdapter(getActivity().getLayoutInflater(),
      android.text.format.DateFormat.getDateFormat(getActivity()));
    setAdapter(repoMan);

    unsub();
    sub=observable.subscribe(
      s -> applyResults(s.viewer()),
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

  private MyStars.Data getFields(Response<MyStars.Data> response) {
    if (response.hasErrors()) {
      throw new RuntimeException(response.errors().get(0).message());
    }

    return(response.data());
  }

  // based on https://stackoverflow.com/a/45938570/115145

  private Observable<Response<MyStars.Data>> getPages() {
    return(Observable.generate(() -> (getPage(null)),
      (previousPage, responseEmitter) -> {
        MyStars.StarredRepositories repos=previousPage.data().viewer().starredRepositories();
        List<MyStars.Edge> edges=repos.edges();
        MyStars.Edge last=edges.get(edges.size()-1);
        Response<MyStars.Data> result=getPage(last.cursor());

        responseEmitter.onNext(result);

        if (result.hasErrors() || !repos.pageInfo().hasNextPage()) {
          responseEmitter.onComplete();
        }

        return(result);
      }));
  }

  private Response<MyStars.Data> getPage(String cursor) throws ApolloException {
    return(apolloClient.query(MyStars.builder().first(5).after(cursor).build()).execute());
  }

  private void applyResults(MyStars.Viewer viewer) {
    ((MainActivity)getActivity()).setLogin(viewer.login());
    repoMan.addEdges(viewer.starredRepositories().edges());
  }

  private static class RepoAdapter extends RecyclerView.Adapter<RowHolder> {
    private final LayoutInflater inflater;
    private final List<MyStars.Edge> edges=new ArrayList<>();
    private final DateFormat dateFormat;

    RepoAdapter(LayoutInflater inflater, DateFormat dateFormat) {
      this.inflater=inflater;
      this.dateFormat=dateFormat;
    }

    @Override
    public RowHolder onCreateViewHolder(ViewGroup parent,
                                        int viewType) {
      return(new RowHolder(inflater
        .inflate(android.R.layout.simple_list_item_1, parent, false), dateFormat));
    }

    @Override
    public void onBindViewHolder(RowHolder holder, int position) {
      holder.bind(edges.get(position).node());
    }

    @Override
    public int getItemCount() {
      return(edges.size());
    }

    private void addEdges(List<MyStars.Edge> moarEdges) {
      int size=edges.size();

      edges.addAll(moarEdges);
      notifyItemRangeInserted(size, moarEdges.size());
    }
  }

  private static class RowHolder extends RecyclerView.ViewHolder {
    final private TextView name;
    private final DateFormat dateFormat;

    RowHolder(View itemView, DateFormat dateFormat) {
      super(itemView);

      name=(TextView)itemView.findViewById(android.R.id.text1);
      this.dateFormat=dateFormat;
    }

    void bind(MyStars.Node node) {
      name.setText(String.format("%s (%s)", node.name(),
        dateFormat.format(node.createdAt())));
    }
  }
}
