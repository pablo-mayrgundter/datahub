'use strict';

function login() {
  new Resource('/user').get({params: {'continueUrl': window.location.path}})
    .success(function(rsp) {
      console.log(rsp);
      $('user').innerHTML = rsp;
    });
}
