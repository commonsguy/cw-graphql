var express = require('express');
var app = express();
var v0p1 = require('./server-v0.1.js')

app.use(v0p1.route(), v0p1.handler());
app.listen(4000);
console.log('Running a GraphQL API server at localhost:4000'+v0p1.route());
