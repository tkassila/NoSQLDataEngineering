'use strict'

var mongoose = require('mongoose');
var Identifier = require('./IdentifierSchema');

var Events = new mongoose.Schema({
  _id: {type: String, required: true},
  classification: {type: String, required: true},
  end_date: {type: String, required: true},
  identifiers: {type: [Identifier], default: undefined},
  name: {type: String, required: true},
  organization_id: {type: String, ref: "Organizations"},
  start_date: {type: String, required: true}
}, { versionKey: false, collection: 'events'});


module.exports = Events;
