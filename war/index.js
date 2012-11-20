'use strict';

angular.module('demo', ['datasetService']);

angular.module('datasetService', ['ngResource']).
  factory('Dataset', function($resource){
      return $resource('/data/:name', {}, {});
    });

function DatasetCtrl($scope, $routeParams, Dataset) {
  // $routeParams.name matches "when('/:name'," above.
  $scope.dataset = Dataset.get({name: $routeParams.name}, function(f) {
    });
}

DatasetCtrl.$inject = ['$scope', '$routeParams', 'Dataset'];
