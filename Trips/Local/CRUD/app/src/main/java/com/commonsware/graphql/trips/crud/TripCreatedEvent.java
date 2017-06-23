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

import com.apollographql.apollo.api.Response;
import com.commonsware.graphql.trips.api.CreateTrip;

class TripCreatedEvent {
  final Response<CreateTrip.Data> response;
  final Exception exception;

  TripCreatedEvent(Response<CreateTrip.Data> response) {
    this.response=response;
    this.exception=null;
  }

  TripCreatedEvent(Exception e) {
    this.exception=e;
    this.response=null;
  }
}
