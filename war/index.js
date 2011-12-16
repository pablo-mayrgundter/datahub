angular.service('Items', function($resource) {
    return $resource('/data/numbers');
  });

function ItemRsrc(Items) {
  this.items = Items.query();
  this.add = function() {
    var obj = new Items();
    obj['a'] = 'foo';
    obj.$save();
    this.items.push(obj);
  }
}

angular.widget('my:input', function(expression, compileElement) {
    var compiler = this;
    var expr = compileElement.attr('expr');
    console.log(expr);
    return function(linkElement) {
      var currentScope = this;
      currentScope.$watch(expr, function(value) {
          for (propName in value)
            if (propName.substring(0,1) != '$') // ignore angular extensions
              console.log(propName + ':' + value[propName]);
        });
    };
  });
