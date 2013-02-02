'use strict';

var root = 'data';

function FileCtrl($scope, $location, $http) {
  $scope.loc = $location;
  $scope.pathParts = [];
  $scope.content = {};
  $scope.subdirs = {};
  $scope.navLinks = [];

  $scope.getFile = function (path) {
    var parts = path.split('/');
    return parts[parts.length - 1];
  };

  $scope.setNavLinks = function() {
    $scope.navLinks.length = 0;
    var links = [];
    var path = '/';
    var count = 0;
    for (var p in $scope.pathParts) {
      var part = $scope.pathParts[p];
      path += part + '/';
      var obj = {"link":path,"name":part};
      $scope.navLinks.push(obj);
    }
  };

  $scope.$on('$locationChangeSuccess', function() {
      $http.get($scope.loc.path()).success(function(rsp) {
          $scope.content = rsp;
        });
      $http.get($scope.loc.path() + "/").success(function(rsp) {
          $scope.subdirs = rsp;
        });
      var p = $location.path();
      // TODO(pmy): we always fetch item, but perhaps should watch for
      // presence of trailing slash to indicate a request for listing.
      if (p.match(/\/$/)) {
        p = p.substring(0, p.length - 1);
        $location.path(p);
      }
      if (p.charAt(0) != '/') {
        // Angular API is that path always leads with /.
        log('Angular path missing leading slash.');
        return;
      }
      p = p.substring(1);
      $scope.pathParts = p.split('/');
      $scope.fileName = $scope.pathParts[$scope.pathParts.length - 1];
      $scope.setNavLinks();
    });

  // to be used for put/name create below.
  $scope.fileName = '';

  $scope.save = function(name) {
    var obj = compileSource('fileObject');
    var newPathParts = [];
    newPathParts = newPathParts.concat($scope.pathParts);
    newPathParts[newPathParts.length - 1] = name;
    var newPath = '';
    for (var i in newPathParts) {
      var part = newPathParts[i];
      newPath += '/' + part;
    }
    // PUT requires a name.
    $http.put(newPath, obj).success(function() {
        log('OK');
      });
  };

  $scope.add = function() {
    // POST will be allocated a name, specified in the response
    // Location header.
    $http.post($scope.loc.path() + '/', obj).success(function(data, status, headers) {
        var path = headers('Location');
        if (path) {
          var pathParts = path.split('/');
          var newName = pathParts[pathParts.length - 1];
          $scope.subdirs[newName] = obj;
        } else {
          log('Missing Location header in successful response.')
        }
      });
  };

  $scope.delete = function(name) {
    if (!name) {
      log('delete passed invalid path: ' + name);
      return;
    }
    $http.delete($scope.loc.path() + '/' + name).success(function() {
        delete $scope.subdirs[name];
        log('Deleted.')
      });
  };
}

FileCtrl.$inject = ['$scope', '$location', '$http'];

// utils

// TODO(pmy): status bar.

function log(a, b) {
  console.log(a);
  if (b) {
    console.log(b);
  }
}

function compileSource(domId) {
  log('domId: ', domId);
  var elt = document.getElementById(domId);
  log('elt: ', elt);
  var val = elt.value;
  log('val: ', val);
 return eval('(' + val + ')');
}
