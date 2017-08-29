var express = require('express');
var graphqlHTTP = require('express-graphql');
var { buildSchema } = require('graphql');
var {maskErrors, UserError} = require('graphql-errors');
var uuidV4 = require('uuid/v4');
var { makeExecutableSchema } = require('graphql-tools');

const schemaString = `
  enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    OMG
  }

  type Comment {
    text: String!
  }

  type Link {
    url: String!
    title: String!
  }

  union Note = Comment | Link

  interface Plan {
    id: ID!
    startTime: String!
    title: String!
    notes: [Note!]
    creationTime: String!
    updateTime: String!
    priority: Priority!
    duration: Int!
  }

  # Represents a collection of plans encompassing some trip to somewhere for something
  type Trip implements Plan {

    # A unique ID
    id: ID!

    # When this trip begins (ISO8601 date format)
    startTime: String!

    # Some human-readable identifier of this trip
    title: String!

    # Text and links with additional details about this trip
    notes: [Note!]

    # Server-supplied time when this trip was created
    creationTime: String!

    # Server-supplied time when this trip was last updated
    updateTime: String!

    # How important is this trip, really?
    priority: Priority!

    # How long the trip will be, in minutes
    duration: Int!

    # The flights, lodging, etc. that make up this trip
    plans: [Plan!]!
  }

  type Lodging implements Plan {
    id: ID!
    startTime: String!
    title: String!
    notes: [Note!]
    creationTime: String!
    updateTime: String!
    priority: Priority!
    duration: Int!
    address: String!
  }

  type Flight implements Plan {
    id: ID!
    startTime: String!
    title: String!
    notes: [Note!]
    creationTime: String!
    updateTime: String!
    priority: Priority!
    duration: Int!
    departingAirport: String!
    arrivingAirport: String!
    airlineCode: String!
    flightNumber: String!
    seatNumber: String
  }

  input TripInput {
    startTime: String!
    title: String!
    priority: Priority!
    duration: Int!
  }

  # These are the available queries, representing data that  we can retrieve from this server
  type Query {

    # A list of all of the trips
    allTrips: [Trip!]!

    # Obtain a trip given its unique ID
    getTrip(id: ID!): Trip

    # Find trips by searching their title and notes for a string
    findTrips(searchFor: String!): [Trip!]!
  }

  type Mutation {
    createTrip(trip: TripInput!): Trip!
  }
`;

var ORIGINAL={
  "2c494055-78bc-430c-9ab7-19817f3fc060": {
    "id": "2c494055-78bc-430c-9ab7-19817f3fc060",
    "startTime": "2017-12-20T13:14:00-05:00",
    "creationTime": new Date().toISOString(),
    "updateTime": new Date().toISOString(),
    "title": "Vacation!",
    "notes": [
      {
        "text": "It's gonna be great!"
      },
      {
        "url": "http://www.miragrant.com/",
        "title": "Source of some reading material for the trip"
      }
    ],
    "priority": "MEDIUM",
    "duration": 10080,
    "___type": "Trip",
    "plans": [
      {
        "id": "319185bd-fab0-49e3-86ce-251d2aaa5d23",
        "startTime": "2017-12-20T13:14:00-05:00",
        "creationTime": new Date().toISOString(),
        "updateTime": new Date().toISOString(),
        "title": "Flight to Chicago",
        "notes": null,
        "priority": "HIGH",
        "duration": 150,
        "departingAirport": "EWR",
        "arrivingAirport": "ORD",
        "airlineCode": "UAL",
        "flightNumber": "321",
        "seatNumber": "15C",
        "___type": "Flight"
      },
      {
        "id": "319185bd-fab0-49e3-86ce-251d2aaa5d23",
        "startTime": "2017-12-20T15:00:00-05:00",
        "creationTime": new Date().toISOString(),
        "updateTime": new Date().toISOString(),
        "title": "House of Munster",
        "notes": null,
        "priority": "MEDIUM",
        "duration": 9900,
        "address": "1313 Mockingbird Lane, Springfield, IL, USA 62701",
        "___type": "Lodging"
      }
    ]
  },
  "e323fed5-6805-4bcf-8cb6-8b7a5014a9d9": {
    "id": "e323fed5-6805-4bcf-8cb6-8b7a5014a9d9",
    "startTime": "2018-01-14T11:45:00-05:00",
    "creationTime": new Date().toISOString(),
    "updateTime": new Date().toISOString(),
    "title": "Business Trip",
    "notes": null,
    "priority": "HIGH",
    "duration": 4320,
    "___type": "Trip",
    "plans": [
      {
        "id": "d40eb2e7-3211-422e-858c-403cbe3fa680",
        "startTime": "2018-01-14T11:45:00-05:00",
        "creationTime": new Date().toISOString(),
        "updateTime": new Date().toISOString(),
        "title": "Flight to Denver",
        "notes": [
          {
            "text": "Still need seat assignment!"
          }
        ],
        "priority": "HIGH",
        "duration": 257,
        "departingAirport": "EWR",
        "arrivingAirport": "IAD",
        "airlineCode": "UAL",
        "flightNumber": "456",
        "seatNumber": null,
        "___type": "Flight"
      },
      {
        "id": "e28a591b-cdc9-4328-9e79-9e4ed60ae7d2",
        "startTime": "2018-01-14T15:00:00-05:00",
        "creationTime": new Date().toISOString(),
        "updateTime": new Date().toISOString(),
        "title": "Hotel Von",
        "notes": null,
        "priority": "MEDIUM",
        "duration": 4140,
        "address": "10 Backfield Place, Denver, CO 81023",
        "___type": "Lodging"
      }
    ]
  }
};

function isHosted() {
  return process.argv.indexOf("--hosted")>=0;
}

var model;

if (isHosted()) {
  model=JSON.parse(JSON.stringify(ORIGINAL));
}
else {
  model={};
}

function values(o) {
  return Object.keys(o).map(function(k) { return o[k] })
}

var root = {
  allTrips: function () {
    return values(model);
  },
  getTrip: function ({id}) {
    return model[id];
  },
  findTrips: function ({searchFor}) {
    var trips=values(model);
    var result=[];

    for (var i=0 ; i < trips.length ; i++) {
      var trip=trips[i];

      if (trip.title.indexOf(searchFor)>=0) {
        result.push(trip);
      }
      else {
        var notes=trip.notes;

        if (notes) {
          for (var j=0 ; j < notes.length ; j++) {
            var note=notes[j];

            if (note.text && note.text.indexOf(searchFor)>=0) {
              result.push(trip);
              break;
            }
            else if (note.title && note.title.indexOf(searchFor)>=0) {
              result.push(trip);
              break;
            }
          }
        }
      }
    }

    return result;
  },
  createTrip: function({trip}) {
    if (isHosted()) {
      throw new UserError('Not supported on this server');
    }
    else {
      var date = new Date().toISOString();
      var result = {
        "id": uuidV4(),
        "startTime": trip.startTime,
        "title": trip.title,
        "notes": [],
        "creationTime": date,
        "updateTime": date,
        "priority": trip.priority,
        "duration": trip.duration,
        "plans": [],
        "___type": "Trip"
      };

      model[result.id]=result;

      return result;
    }
  }
};

const resolvers = {
  Plan: {
    __resolveType(data, context, info) {
      if (data.___type=='Flight') {
          return info.schema.getType('Flight');
      }
      else if (data.___type=='Lodging') {
          return info.schema.getType('Lodging');
      }
      else if (data.___type=='Trip') {
          return info.schema.getType('Trip');
      }
      else {
          return null;
      }
    }
  },
  Note: {
    __resolveType(data, context, info) {
      if (data.text) {
          return info.schema.getType('Comment');
      }
      else if (data.url) {
          return info.schema.getType('Link');
      }
      else {
          return null;
      }
    }
  }
}

var schema = makeExecutableSchema({
        typeDefs: schemaString,
        resolvers: resolvers})

module.exports = {
  route: function () {
    return '/0.3/graphql';
  },
  handler: function () {
    return graphqlHTTP({
      schema: schema,
      rootValue: root,
      graphiql: true,
    });
  }
}
