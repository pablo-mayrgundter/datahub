'use strict';

var datahub = angular.module('datahub', []);

// TODO(pmy): replace or move these somewhere better.
function log(a, b) {
  console.log(a);
  if (b) {
    console.log(b);
  }
}

var $ = $ ? $ : function(id) { document.getElementById(id); }

function compileSource(domId) {
  return eval('(' + $(domId).value + ')');
}
