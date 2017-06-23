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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.apollographql.apollo.ApolloClient;
import java.text.SimpleDateFormat;
import okhttp3.OkHttpClient;

public class MainActivity extends Activity
  implements FragmentManager.OnBackStackChangedListener {
  private static final String TAG_SEARCH="search";
  @SuppressLint("SimpleDateFormat")
  static final SimpleDateFormat ISO8601=
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  private ApolloClient apolloClient=ApolloClient.builder()
    .okHttpClient(new OkHttpClient())
    .serverUrl(BuildConfig.LOCAL_SERVER_URL)
    .build();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState==null) {
      getFragmentManager().beginTransaction()
        .add(android.R.id.content, new SimpleTripsFragment())
        .commit();
    }

    getFragmentManager().addOnBackStackChangedListener(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);

    return(super.onCreateOptionsMenu(menu));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId()==R.id.add) {
      getFragmentManager()
        .popBackStack(TAG_SEARCH, FragmentManager.POP_BACK_STACK_INCLUSIVE);
      getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new AddTripFragment())
        .addToBackStack(null)
        .commit();

      return(true);
    }

    return(super.onOptionsItemSelected(item));
  }

  @Override
  public void onBackStackChanged() {
    Fragment f=
      getFragmentManager()
        .findFragmentById(android.R.id.content);

    if (f!=null && f instanceof SimpleTripsFragment) {
      updateTitle(((SimpleTripsFragment)f).getSearchExpression());
    }
  }

  ApolloClient getApolloClient() {
    return(apolloClient);
  }

  void searchFor(String search) {
    String tag=null;

    if (getFragmentManager().findFragmentById(android.R.id.content)
      instanceof SimpleTripsFragment) {
      tag=TAG_SEARCH;
    }

    getFragmentManager().beginTransaction()
      .replace(android.R.id.content,
        SimpleTripsFragment.searchFor(search))
      .addToBackStack(tag)
      .commit();
    updateTitle(search);
  }

  private void updateTitle(String search) {
    if (search==null) {
      setTitle(R.string.app_name);
    }
    else {
      setTitle(getString(R.string.title_prefix_search)+search);
    }
  }
}
