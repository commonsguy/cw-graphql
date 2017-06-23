/***
 Copyright (c) 17 CommonsWare, LLC
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

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.borax12.materialdaterangepicker.date.DatePickerDialog;
import com.commonsware.graphql.trips.api.CreateTrip;
import com.commonsware.graphql.trips.api.type.Priority;
import com.commonsware.graphql.trips.api.type.TripInput;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.util.Calendar;

public class AddTripFragment extends Fragment
  implements DatePickerDialog.OnDateSetListener {
  private static final Priority[] PRIORITIES={
    Priority.LOW,
    Priority.MEDIUM,
    Priority.HIGH,
    Priority.OMG
  };
  private Calendar start=Calendar.getInstance();
  private Calendar end=Calendar.getInstance();
  private TextView dates;
  private Spinner priority;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);
    end.add(Calendar.DATE, 1);
    EventBus.getDefault().register(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return(inflater.inflate(R.layout.trip_add, container, false));
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    view.findViewById(R.id.cancel).setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          getFragmentManager().popBackStack();
        }
      });

    view.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        save();
      }
    });

    view.findViewById(R.id.date_picker).setOnClickListener(
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          DatePickerDialog dlg=
            DatePickerDialog.newInstance(AddTripFragment.this,
              start.get(Calendar.YEAR), start.get(Calendar.MONTH),
              start.get(Calendar.DAY_OF_MONTH),
              end.get(Calendar.YEAR), end.get(Calendar.MONTH),
              end.get(Calendar.DAY_OF_MONTH));
          dlg.setAutoHighlight(true);
          dlg.show(getFragmentManager(), "dates");
        }
      });

    priority=(Spinner)view.findViewById(R.id.priority);

    ArrayAdapter<CharSequence> priorities=
      ArrayAdapter.createFromResource(getActivity(), R.array.priorities,
        android.R.layout.simple_spinner_item);

    priorities
      .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    priority.setAdapter(priorities);

    dates=(TextView)view.findViewById(R.id.dates);
    updateDates();
  }

  @Override
  public void onDestroy() {
    EventBus.getDefault().unregister(this);

    super.onDestroy();
  }

  @Override
  public void onDateSet(DatePickerDialog view, int year, int monthOfYear,
                        int dayOfMonth, int yearEnd, int monthOfYearEnd,
                        int dayOfMonthEnd) {
    start.set(Calendar.YEAR, year);
    start.set(Calendar.MONTH, monthOfYear);
    start.set(Calendar.DAY_OF_MONTH, dayOfMonth);

    end.set(Calendar.YEAR, yearEnd);
    end.set(Calendar.MONTH, monthOfYearEnd);
    end.set(Calendar.DAY_OF_MONTH, dayOfMonthEnd);

    updateDates();
  }

  @Subscribe(threadMode =ThreadMode.MAIN)
  public void onTripCreated(TripCreatedEvent event) {
    Response<CreateTrip.Data> r=event.response;

    if (event.exception!=null) {
      Toast
        .makeText(getActivity(), event.exception.getMessage(),
          Toast.LENGTH_LONG)
        .show();
      Log.e(getClass().getSimpleName(), "Exception creating trip", event.exception);
    }
    else if (r.errors()!=null && r.errors().size()>0) {
      Toast
        .makeText(getActivity(), r.errors().get(0).message(),
          Toast.LENGTH_LONG)
        .show();
    }
    else {
      Toast
        .makeText(getActivity(), R.string.msg_created, Toast.LENGTH_SHORT)
        .show();
      getFragmentManager().popBackStack();
    }
  }

  private void updateDates() {
    dates.setText(DateUtils.formatDateRange(getActivity(),
      start.getTimeInMillis(), end.getTimeInMillis(),
      DateUtils.FORMAT_SHOW_DATE));
  }

  private ApolloClient getApolloClient() {
    return(((MainActivity)getActivity()).getApolloClient());
  }

  private void save() {
    EditText title=(EditText)getView().findViewById(R.id.title);
    int duration=(int)((end.getTimeInMillis()-start.getTimeInMillis())/60000);

    if (duration<0) {
      duration=-1*duration;
    }

    TripInput trip=TripInput.builder()
      .priority(PRIORITIES[priority.getSelectedItemPosition()])
      .title(title.getText().toString())
      .startTime(MainActivity.ISO8601.format(start.getTime()))
      .duration(duration)
      .build();
    final CreateTrip vars=CreateTrip.builder().trip(trip).build();

    new Thread() {
      @Override
      public void run() {
        try {
          Response<CreateTrip.Data> response=getApolloClient().mutate(vars).execute();
          EventBus.getDefault().post(new TripCreatedEvent(response));
        }
        catch (ApolloException e) {
          EventBus.getDefault().post(new TripCreatedEvent(e));
        }
      }
    }.start();
  }
}
