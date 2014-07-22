'use strict';

function obj2html(obj) {
  if (!(obj instanceof Object)) {
    return obj + '';
  }
  var html = '';
  var arr = obj instanceof Array;
  html += arr ? '<ol>' : '<ul>';
  for (var i in obj) {
    if (arr) {
      html += '<li>' + obj2html(obj[i]);
    } else {
      html += '<li>' + i + ': ' + obj2html(obj[i]);
    }
  }
  html += arr ? '</ol>' : '</ul>';
  return html;
}
