'use strict';

function UserCtrl($scope, $location, $http) {
  $scope.user = {};
  // TODO(pmy): the '/#' prefix shouldn't be hardcoded, but don't
  // understand angular hash path handling yet.
  $http.get('/user', {params: {'continueUrl': '/#' + $location.path()}})
    .success(function(rsp) {
      $scope.user = rsp;
    });
}
datahub.controller('UserCtrl', ['$scope', '$location', '$http', UserCtrl]);
