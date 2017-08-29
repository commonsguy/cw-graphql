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

import com.apollographql.apollo.CustomTypeAdapter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

class ISO8601Adapter implements CustomTypeAdapter<Date> {
  private static final SimpleDateFormat ISO8601=
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

  // based in part on https://stackoverflow.com/a/10621553/115145

  @Override
  public Date decode(String value) {
    try {
      return(ISO8601.parse(value.replace("Z", "+00:00")));
    }
    catch (ParseException e) {
      throw new IllegalArgumentException(value+" is not a valid ISO 8601 date", e);
    }
  }

  @Override
  public String encode(Date value) {
    return(ISO8601.format(value));
  }
}
