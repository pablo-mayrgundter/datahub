'use strict';

var ROOT = 'data';

function FileCtrl($scope, $location, $http) {

  $scope.loc = $location;

  // TODO(pmy): This is currently the root for the servlet config.
  // Need to do something cleaner here.
  if ($scope.loc.path() == '') {
    $scope.loc.path('/' + ROOT);
  }

  $scope.pathParts = [];
  $scope.content = {};
  $scope.subdirs = {};
  // Used to hold for display
  $scope.searchQuery = {};
  $scope.searchRes = {};
  $scope.navLinks = [];

  $scope.query = '';
  /** Used by put actions. */
  $scope.fileName = '';

  $scope.getFile = function (path) {
    var parts = path.split('/');
    return parts[parts.length - 1];
  };

  $scope.setNavLinks = function() {
    $scope.navLinks.length = 0;
    var links = [];
    var path = '';
    var count = 0;
    for (var p in $scope.pathParts) {
      if (count++ == 0) {
        continue;
      }
      var part = $scope.pathParts[p];
      path += '/' + part;
      var obj = {'link': path, 'name': part};
      $scope.navLinks.push(obj);
    }
  };

  $scope.$on('$locationChangeSuccess', function() {
      $scope.searchRes = {};
      $http.get($scope.loc.path()).success(function(rsp) {
          $scope.content = rsp;
          document.getElementById('objHtml').innerHTML = obj2html(rsp);
        });
      $http.get($scope.loc.path() + '/').success(function(rsp) {
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

  $scope.toggleEdit = function() {
    var objHtmlElt = document.getElementById('objHtml');
    var contentElt = document.getElementById('fileObject');
    var editBtn = document.getElementById('editBtn');
    var saveBtn = document.getElementById('saveBtn');
    if (contentElt.hasAttribute('readonly')) {
      // Make editable.
      objHtmlElt.style.display = 'none';
      contentElt.removeAttribute('readonly');
      editBtn.style.display = 'none';
      saveBtn.style.display = 'inline-block';
    } else {
      // Freeze.
      contentElt.setAttribute('readonly', 'true');
      editBtn.style.display = 'inline-block';
      saveBtn.style.display = 'none';
      objHtmlElt.style.display = 'block';
    }
  };

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
    var me = this;
    $http.put(newPath, obj).success(function() {
        document.getElementById('objHtml').innerHTML = obj2html(obj);
        me.toggleEdit();
      });
  };

  $scope.add = function(name) {
    var newPath = $scope.loc.path() + '/' + name;
    $http.put(newPath, {}).success(function(data, status, headers) {
        $scope.loc.path(newPath);
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

  $scope.search = function(query) {
    var q = $scope.loc.path() + '?q=' + query;
    $scope.searchQuery = query;
    $http.get(q).success(function(res) {
        $scope.searchRes = res;
        var sub = {};
        var results = res.results;
        for (var ndx in results) {
          var res = results[ndx];
          for (var path in res) {
            sub[path] = res[path];
          }
        }
        $scope.subdirs = sub;
      })
  };

  $scope.isEmpty = function(obj) {
    for (var i in obj) {
      return false;
    }
    return true;
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

