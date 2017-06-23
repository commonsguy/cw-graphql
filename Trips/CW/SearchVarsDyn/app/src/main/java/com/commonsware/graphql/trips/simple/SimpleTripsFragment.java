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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import static android.app.SearchManager.QUERY;

public class SimpleTripsFragment extends RecyclerViewFragment
  implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
  private static final MediaType MEDIA_TYPE_JSON
    =MediaType.parse("application/json; charset=utf-8");
  private static final String ARG_SEARCH="search";
  private static final String KEY_ALL_TRIPS="allTrips";
  private static final String KEY_FIND_TRIPS="findTrips";
  private static final String KEY_TITLE="title";
  private static final String KEY_START_TIME="startTime";
  private static final String VARIABLES="variables";
  private static final String VAR_SEARCH="search";
  @SuppressLint("SimpleDateFormat")
  private static final SimpleDateFormat ISO8601=
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  private static final String DOCUMENT=
    "{ allTrips { id title startTime priority duration creationTime } }";
  private static final String SEARCH_DOCUMENT=
    "query find($search: String!) { findTrips(searchFor: $search) { id title startTime priority duration creationTime } }";
  private static final String ENDPOINT=
    "https://graphql-demo.commonsware.com/0.1/graphql";
  private Observable<GraphQLResponse> observable;
  private Disposable sub;
  private SearchView sv;
  private MenuItem search;

  public static SimpleTripsFragment searchFor(String search) {
    SimpleTripsFragment result=new SimpleTripsFragment();
    Bundle args=new Bundle();

    args.putString(ARG_SEARCH, search);
    result.setArguments(args);

    return(result);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);
    setHasOptionsMenu(true);

    final String searchFor=getSearchExpression();

    observable=Observable
      .defer(new Callable<ObservableSource<GraphQLResponse>>() {
        @Override
        public ObservableSource<GraphQLResponse> call() throws Exception {
          return(Observable.just(searchFor==null ? query() : search(searchFor)));
        }
      })
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .cache();
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
      response -> setAdapter(buildAdapter(response)),
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

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.actions, menu);

    search=menu.findItem(R.id.search);

    sv=(SearchView)search.getActionView();
    sv.setOnQueryTextListener(this);
    sv.setOnCloseListener(this);
    sv.setSubmitButtonEnabled(true);
    sv.setIconifiedByDefault(true);
    sv.setIconified(true);

    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onQueryTextChange(String newText) {
    return(false);
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    ((MainActivity)getActivity()).searchFor(query);
    search.collapseActionView();

    return(true);
  }

  @Override
  public boolean onClose() {
    return(false);
  }

  private void unsub() {
    if (sub!=null && !sub.isDisposed()) {
      sub.dispose();
    }
  }

  String getSearchExpression() {
    Bundle args=getArguments();

    if (args==null) {
      return(null);
    }

    return(args.getString(ARG_SEARCH));
  }

  private RecyclerView.Adapter buildAdapter(GraphQLResponse response) {
    if (response.errors!=null && response.errors.size()>0) {
      Toast
        .makeText(getActivity(), response.errors.get(0).toString(), Toast.LENGTH_LONG)
        .show();

      for (ResponseError error : response.errors) {
        Log.e(getClass().getSimpleName(), error.toString());
      }
    }

    List<Map<String, Object>> trips=null;

    if (response.data!=null) {
      trips=(List<Map<String, Object>>)response.data.get(KEY_ALL_TRIPS);

      if (trips==null) {
        trips=(List<Map<String, Object>>)response.data.get(KEY_FIND_TRIPS);
      }
    }

    if (trips==null) {
      trips=new ArrayList<>();
    }

    return(new TripsAdapter(trips, getActivity().getLayoutInflater(),
      android.text.format.DateFormat.getDateFormat(getActivity())));
  }

  private GraphQLResponse query() throws IOException {
    HashMap<String, String> payload=new HashMap<>();
    Gson gson=new Gson();

    payload.put(QUERY, DOCUMENT);

    String body=gson.toJson(payload);
    Request request=new Request.Builder()
      .url(ENDPOINT)
      .post(RequestBody.create(MEDIA_TYPE_JSON, body))
      .build();
    Response okResponse=new OkHttpClient().newCall(request).execute();

    return(gson.fromJson(okResponse.body().charStream(), GraphQLResponse.class));
  }

  private GraphQLResponse search(String search) throws IOException {
    HashMap<String, Object> payload=new HashMap<>();
    HashMap<String, String> variables=new HashMap<>();
    Gson gson=new Gson();

    variables.put(VAR_SEARCH, search);
    payload.put(QUERY, SEARCH_DOCUMENT);
    payload.put(VARIABLES, variables);

    String body=gson.toJson(payload);
    Request request=new Request.Builder()
      .url(ENDPOINT)
      .post(RequestBody.create(MEDIA_TYPE_JSON, body))
      .build();
    Response okResponse=new OkHttpClient().newCall(request).execute();

    return(gson.fromJson(okResponse.body().charStream(), GraphQLResponse.class));
  }

  private static class TripsAdapter extends RecyclerView.Adapter<RowHolder> {
    private final List<Map<String, Object>> trips;
    private final LayoutInflater inflater;
    private final DateFormat dateFormat;

    private TripsAdapter(List<Map<String, Object>> trips,
                         LayoutInflater inflater, DateFormat dateFormat) {
      this.trips=trips;
      this.inflater=inflater;
      this.dateFormat=dateFormat;
    }

    @Override
    public RowHolder onCreateViewHolder(ViewGroup parent,
                                        int viewType) {
      return(new RowHolder(inflater.inflate(android.R.layout.simple_list_item_1,
        parent, false), dateFormat));
    }

    @Override
    public void onBindViewHolder(RowHolder holder,
                                 int position) {
      holder.bind(trips.get(position));
    }

    @Override
    public int getItemCount() {
      return(trips.size());
    }
  }

  private static class RowHolder extends RecyclerView.ViewHolder {
    private final TextView rowLabel;
    private final DateFormat dateFormat;

    RowHolder(View itemView, DateFormat dateFormat) {
      super(itemView);

      rowLabel=(TextView)itemView.findViewById(android.R.id.text1);
      this.dateFormat=dateFormat;
    }

    void bind(Map<String, Object> trip) {
      String startTime=trip.get(KEY_START_TIME).toString();
      String title=trip.get(KEY_TITLE).toString();

      try {
        Date parsedStartTime=ISO8601.parse(startTime);
        rowLabel.setText(String.format("%s : %s",
          dateFormat.format(parsedStartTime), title));
      }
      catch (ParseException e) {
        Log.e(getClass().getSimpleName(), "Exception parsing "+startTime, e);
      }
    }
  }
}
