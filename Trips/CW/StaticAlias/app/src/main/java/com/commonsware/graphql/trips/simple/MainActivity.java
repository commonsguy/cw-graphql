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

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

public class MainActivity extends Activity
  implements FragmentManager.OnBackStackChangedListener {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getFragmentManager().findFragmentById(android.R.id.content) == null) {
      getFragmentManager().beginTransaction()
        .add(android.R.id.content,
          new SimpleTripsFragment()).commit();
    }

    getFragmentManager().addOnBackStackChangedListener(this);
  }

  void searchFor(String search) {
    getFragmentManager().beginTransaction()
      .replace(android.R.id.content,
        SimpleTripsFragment.searchFor(search))
      .addToBackStack(null)
      .commit();
    updateTitle(search);
  }

  @Override
  public void onBackStackChanged() {
    SimpleTripsFragment f=
      (SimpleTripsFragment)getFragmentManager()
        .findFragmentById(android.R.id.content);

    if (f!=null) {
      updateTitle(f.getSearchExpression());
    }
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
