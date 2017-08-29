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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.commonsware.graphql.trips.api.GetAllTrips;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

public class SimpleTripsFragment extends RecyclerViewFragment {
  @SuppressLint("SimpleDateFormat")
  private static final SimpleDateFormat ISO8601=
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  private ApolloClient apolloClient=ApolloClient.builder()
    .okHttpClient(new OkHttpClient())
    .serverUrl("https://graphql-demo.commonsware.com/0.3/graphql")
    .build();
  private Observable<GetAllTrips.Data> observable;
  private Disposable sub;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);

    observable=Rx2Apollo.from(apolloClient.query(new GetAllTrips()).watcher())
      .subscribeOn(Schedulers.io())
      .map(response -> (getAllTripsFields(response)))
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
      s -> setAdapter(buildAdapter(s)),
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

  private GetAllTrips.Data getAllTripsFields(Response<GetAllTrips.Data> response) {
    if (response.hasErrors()) {
      throw new RuntimeException(response.errors().get(0).message());
    }

    return(response.data());
  }

  private RecyclerView.Adapter buildAdapter(GetAllTrips.Data response) {
    return(new TripsAdapter(response.allTrips(),
      getActivity().getLayoutInflater(),
      android.text.format.DateFormat.getDateFormat(getActivity())));
  }

  private static class TripsAdapter extends RecyclerView.Adapter<RowHolder> {
    private final List<GetAllTrips.AllTrip> trips;
    private final LayoutInflater inflater;
    private final DateFormat dateFormat;

    private TripsAdapter(List<GetAllTrips.AllTrip> trips,
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

    void bind(GetAllTrips.AllTrip trip) {
      try {
        Date parsedStartTime=ISO8601.parse(trip.startTime());
        rowLabel.setText(String.format("%s : %s",
          dateFormat.format(parsedStartTime), trip.title()));
      }
      catch (ParseException e) {
        Log.e(getClass().getSimpleName(), "Exception parsing "+trip.startTime(), e);
      }
    }
  }
}
