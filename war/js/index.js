'use strict';

angular.module('demo', ['fileService']).
  config(['$routeProvider', function($routeProvider) {
        $routeProvider.
          when('/data/:path', {templateUrl: 'file.html',
                controller: FileCtrl}).

          otherwise({redirectTo: '/'});
      }]);


var prefix = '/data/';

angular.module('fileService', ['ngResource'])
  .factory('File', function($resource){
      return $resource(prefix + ':name/:id', {}, {
          put:{method:'PUT'},
        });
    });

function FileCtrl($scope, $routeParams, File) {
  $scope.path = $routeParams.path;

  $scope.dir = null;

  $scope.mkdir = function() {
    new File({}).$put({name: $scope.path});
  };

  $scope.list = function() {
    console.log($routeParams);
    $scope.dir = File.get({name: $scope.path});
  };

  $scope.save = function() {
    var f = new File(compileSource('object'));
    if ($scope.name) {
      // PUT requires a name.
      f.$put({name: $scope.path, id: $scope.name}, function(rsp) {
          $scope.dir['/' + $scope.path + '/' + $scope.name] = rsp;
        });
    } else {
      // POST will be allocated a name, specified in the response
      // Location header.
      f.$save({name: $scope.path}, function(rsp, rspHdrs) {
          var path = rspHdrs('Location');
          if (path) {
            path = path.substring(prefix.length - 1);
            $scope.dir[path] = rsp;
          }
        });
    }
  };

  $scope.delete = function(subPath) {
    var parts = subPath.substring(1).split('/');
    if (parts.length != 2) {
      err('delete passed invalid path: ' + subPath);
      return;
    }
    new File().$delete({name: parts[0], id: parts[1]}, function() {
        delete $scope.dir[subPath];
    });
  };
}

FileCtrl.$inject = ['$scope', '$routeParams', 'File'];

// utils

function err(msg) {
  alert(msg);
}

function compileSource(domId) {
  return eval('(' + elt(domId).value + ')');
}

function elt(domId) {
  return document.getElementById(domId);
}
