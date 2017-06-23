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

package com.commonsware.graphql.trips.crud;

import android.graphics.drawable.Drawable;
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
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.commonsware.graphql.trips.api.FindTrips;
import com.commonsware.graphql.trips.api.GetAllTrips;
import com.commonsware.graphql.trips.api.fragment.TripFields;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SimpleTripsFragment extends RecyclerViewFragment
  implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
  private static final String ARG_SEARCH="search";
  private Observable<List<TripFields>> observable;
  private Disposable sub;
  private SearchView sv;
  private MenuItem search;
  private boolean refreshNeeded=false;

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
    EventBus.getDefault().register(this);
    observe();
  }

  @Override
  public void onViewCreated(View v, Bundle savedInstanceState) {
    super.onViewCreated(v, savedInstanceState);

    setLayoutManager(new LinearLayoutManager(getActivity()));

    getRecyclerView()
      .addItemDecoration(new DividerItemDecoration(getActivity(),
        LinearLayoutManager.VERTICAL));

    unsub();

    if (refreshNeeded) {
      observe();
      refreshNeeded=false;
    }

    sub();
  }

  @Override
  public void onDestroy() {
    EventBus.getDefault().unregister(this);
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

  private ApolloClient getApolloClient() {
    return(((MainActivity)getActivity()).getApolloClient());
  }

  String getSearchExpression() {
    Bundle args=getArguments();

    if (args==null) {
      return(null);
    }

    return(args.getString(ARG_SEARCH));
  }

  @Subscribe(threadMode =ThreadMode.MAIN)
  public void onTripCreated(TripCreatedEvent event) {
    if (getActivity()!=null && event.exception==null) {
      unsub();
      observe();
      sub();
    }
    else {
      refreshNeeded=true;
    }
  }

  private void observe() {
    String search=getSearchExpression();

    if (search==null) {
      observable=Rx2Apollo.from(getApolloClient().query(new GetAllTrips()).watcher())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map(response -> (getAllTripsFields(response)))
        .cache();
    }
    else {
      FindTrips query=FindTrips.builder().search(search).build();

      observable=Rx2Apollo.from(getApolloClient().query(query).watcher())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map(response -> (getFindTripsFields(response)))
        .cache();
    }
  }

  private void sub() {
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

  List<TripFields> getAllTripsFields(Response<GetAllTrips.Data> response) {
    if (response.hasErrors()) {
      throw new RuntimeException(response.errors().get(0).message());
    }

    List<TripFields> result=new ArrayList<>();

    for (GetAllTrips.AllTrip trip : response.data().allTrips()) {
      result.add(trip.fragments().tripFields());
    }

    return(result);
  }

  List<TripFields> getFindTripsFields(Response<FindTrips.Data> response) {
    if (response.hasErrors()) {
      throw new RuntimeException(response.errors().get(0).message());
    }

    List<TripFields> result=new ArrayList<>();

    for (FindTrips.FindTrip trip : response.data().findTrips()) {
      result.add(trip.fragments().tripFields());
    }

    return(result);
  }

  private RecyclerView.Adapter buildAdapter(List<TripFields> trips) {
   return(new TripsAdapter(trips, getActivity().getLayoutInflater(),
      android.text.format.DateFormat.getDateFormat(getActivity())));
  }

  private static class TripsAdapter extends RecyclerView.Adapter<RowHolder> {
    private final List<TripFields> trips;
    private final LayoutInflater inflater;
    private final DateFormat dateFormat;

    private TripsAdapter(List<TripFields> trips,
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

    void bind(TripFields trip) {
      try {
        Date parsedStartTime=MainActivity.ISO8601.parse(trip.startTime());
        rowLabel.setText(String.format("%s : %s",
          dateFormat.format(parsedStartTime), trip.title()));
      }
      catch (ParseException e) {
        Log.e(getClass().getSimpleName(), "Exception parsing "+trip.startTime(), e);
      }
    }
  }
}
