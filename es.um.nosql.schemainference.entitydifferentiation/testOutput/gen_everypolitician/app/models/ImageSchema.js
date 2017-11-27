'use strict'

var mongoose = require('mongoose');

var ImageSchema = new mongoose.Schema({
  url: {type: String, required: true}
}, { versionKey: false, _id : false});

module.exports = mongoose.model('Image', ImageSchema);