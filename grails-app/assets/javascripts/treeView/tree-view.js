var app = angular.module('au.org.biodiversity.nsl.tree-browser', ['Mark.Lagendijk.RecursionHelper', 'ngSanitize']);

var MainTreeController = function ($scope, $http, $element) {

    // there may be more than one tree in the angular app. The nodes under this particular tree need a handle
    // to the tree-as-a-whole scope
    $scope.treeScope = $scope;
    $scope.selectedNode = null;

    $scope.loaded = false;
    $scope.loading = true;
    $scope.error = null;

    $scope.nodeByNslNameId = []
    $scope.nodeByNslInstanceId = []

    $scope.forceOpen = function () {
        // do nothing. This kills the recursive function.
    }


    $scope.setSelected = function (selectedNode) {
        $scope.selectedNode = selectedNode;

        // trigger a jquery event on the root element
        $element.trigger('change.nsl-tree', selectedNode.eventData);
    }

    $scope.mouseoverNode = function (selectedNode) {
        $element.trigger('mouseover.nsl-tree', selectedNode.eventData);
    }
    $scope.mouseenterNode = function (selectedNode) {
        $element.trigger('mouseenter.nsl-tree', selectedNode.eventData);
    }
    $scope.mouseleaveNode = function (selectedNode) {
        $element.trigger('mouseleave.nsl-tree', selectedNode.eventData);
    }

    $element[0].getSelectedName = function () {
        return $scope.selectedNode == null ? null : $scope.selectedNode.name;
    }

    $element[0].getSelectedInstance = function () {
        return $scope.selectedNode == null ? null : $scope.selectedNode.instance;
    }

    $element[0].navigateToNslName = function (nameId) {
        if (!nameId) return;

        var alreadyLoaded = $scope.nodeByNslNameId[nameId];

        if (alreadyLoaded) {
            $scope.$apply(function () {
                $scope.navigateToNode(alreadyLoaded);
            });
        }
        else {
            $scope.fetchAndNavigateToName(nameId);
        }
    }

    $element[0].navigateToNslInstance = function (instanceId) {
        if (!instanceId) return;

        var alreadyLoaded = $scope.nodeByNslInstanceId[instanceId];

        if (alreadyLoaded) {
            $scope.$apply(function () {
                $scope.navigateToNode(alreadyLoaded);
            });
        }
        else {
            alert("TODO! ask the server for the branch and then merge it.");
        }
    }

    $scope.navigateToNode = function (node) {
        node.forceOpen();
        $scope.setSelected(node);
        $scope.$$postDigest(function () {
            node.nameElement.scrollintoview()
        });
    }

    $scope.fetchAndNavigateToName = function (nameId) {
        var getUri = $scope.treeScope.servicesUrl + '/api/tree/branch/' + $scope.treeLabel + '/' + nameId;

        $http.get(getUri,
            {
                transformResponse: function (data, headersGetter) {
                    try {
                        return JSON.parse(data);
                    }
                    catch (e) {
                        return data;
                    }
                }
            }
        )
            .success(function (data) {
                $scope.navigateToPath(data);
            })
            .error(function (data, status, headers, config) {
                window.console && console.log("we got an error {"
                + "\ndata: " + data
                + ",\nstatus: " + status
                + ",\nheaders: " + headers
                + ",\nconfig: " + config
                + "\n}");
                $scope.loaded = false;
                $scope.loading = false;
                $scope.error = data;
            });
    }

    $scope.navigateToPath = function (data) {
        navigateSubnodesToPath($scope, data);
    }


    var getUri = $scope.treeScope.servicesUrl + '/api/tree/branch/' + $scope.treeLabel + ($scope.nameId ? '/' + $scope.nameId : '');

    $http.get(getUri,
        {
            transformResponse: function (data, headersGetter) {
                try {
                    return JSON.parse(data);
                }
                catch (e) {
                    return data;
                }
            }
        }
    )
        .success(function (data) {
            $scope.loaded = true;
            $scope.loading = false;
            $scope.nodes = data.subnode;
            if (!$scope.nodes) $scope.nodes = [];
            $scope.open = true;
        })
        .error(function (data, status, headers, config) {
            window.console && console.log("we got an error {"
            + "\ndata: " + data
            + ",\nstatus: " + status
            + ",\nheaders: " + headers
            + ",\nconfig: " + config
            + "\n}");
            $scope.loaded = false;
            $scope.loading = false;
            $scope.error = data;
        });
};

MainTreeController.$inject = ['$scope', '$http', '$element'];
app.controller('MainTreeController', MainTreeController);

function MainTreeDirective() {
    return {
        templateUrl: "/services/ng/tree/_Tree.html",
        scope: {
            treeLabel: '@treeLabel',
            servicesUrl: '@servicesUrl',
            nameId: '@nameId'
        },
        controller: MainTreeController
    };
}

app.directive('mainTree', MainTreeDirective);

var TreeBlockController = function ($scope) {
};

TreeBlockController.$inject = ['$scope'];
app.controller('TreeBlockController', TreeBlockController);


function TreeBlockDirective(RecursionHelper) {
    return {
        templateUrl: "/services/ng/tree/_TreeBlock.html",
        controller: TreeBlockController,
        compile: RecursionHelper.compile // I don't have a link function, so I may as well pass through the compile
    };
}

TreeBlockDirective.$inject = ['RecursionHelper'];
app.directive('treeBlock', TreeBlockDirective);

var TreeRowController = function ($scope, $http, $timeout, $element) {
    $scope.treeScope = $scope.$parent.treeScope;
    $scope.nameElement = $element

    $scope.setNodeAndDataFields = function (node) {
        if (this.name && this.treeScope.nodeByNslNameId[this.name.id] == this) {
            this.treeScope.nodeByNslNameId[this.name.id] = null;
        }

        if (this.instance && this.treeScope.nodeByNslInstanceId[this.instance.id] == this) {
            this.treeScope.nodeByNslInstanceId[this.instance.id] = this;
        }

        node.scope = $scope;

        this.node = node;
        this.link = node.link;
        this.name = node.name;
        this.instance = node.instance;
        this.treeNode = node.node;
        this.hasSubnames = node.hasSubnodes;

        // an object for the event handlers to pass around
        this.eventData = {
            link: node.link,
            name: node.name,
            instance: node.instance
        };

        if (this.name) {
            this.treeScope.nodeByNslNameId[this.name.id] = this;
        }

        if (this.instance) {
            this.treeScope.nodeByNslInstanceId[this.instance.id] = this;
        }
    }

    $scope.setNodeAndDataFields($scope.$parent.nodes[$scope.nodeIndex]);

    $scope.open = $scope.node.fetched

    $scope.$on('$destroy', function () {
        if ($scope.name && $scope.treeScope.nodeByNslNameId[$scope.name.id] == $scope) {
            $scope.treeScope.nodeByNslNameId[$scope.name.id] = null;
        }

        if ($scope.instance && $scope.treeScope.nodeByNslInstanceId[$scope.instance.id] == $scope) {
            $scope.treeScope.nodeByNslInstanceId[$scope.instance.id] = null;
        }
    });


    if ($scope.node.fetched) {
        $scope.loading = true
        $scope.nodes = [];
        $scope.loaded = true;

        var noSubNodesFetched = true;

        $scope.loading = false
        $scope.nodes = $scope.node.subnode;

        for (var n in $scope.nodes) if (n.fetched) {
            noSubNodesFetched = false;
            break;
        }

        if (noSubNodesFetched) {
            $scope.$$postDigest(function () {
                $scope.nameElement.scrollintoview()
            });
            $scope.treeScope.setSelected($scope);
        }
    }
    else {
        $scope.loading = false
        $scope.nodes = [];
        $scope.loaded = false;
    }

    $scope.toggleSubnodes = function () {
        $scope.open = !$scope.open;
    }

    $scope.reloadNoOpen = function () {
        this.reload(false);
    }

    $scope.reloadAndOpen = function () {
        this.reload(true);
    }

    $scope.forceOpen = function () {
        if (!$scope.open) $scope.open = true;
        $scope.$parent.forceOpen();
    }

    $scope.reload = function (open) {
        this.loading = true;

        $http.get($scope.treeScope.servicesUrl + '/api/tree/name/' + $scope.treeScope.treeLabel + ($scope.name ? '/' + $scope.name.id : ''),
            {
                transformResponse: function (data, headersGetter) {
                    try {
                        return JSON.parse(data);
                    }
                    catch (e) {
                        return data;
                    }
                }
            }
        )
            .success(function (data) {
                $scope.loaded = true;
                $scope.loading = false;

                if (open) $scope.open = true;

                $scope.setNodeAndDataFields(data);

                $scope.nodes = data.subnode;
                if (!$scope.nodes) $scope.nodes = [];

            })
            .error(function (data, status, headers, config) {
                window.console && console.log("we got an error {"
                    + "\ndata: " + data
                    + ",\nstatus: " + status
                    + ",\nheaders: " + headers
                    + ",\nconfig: " + config
                    + "\n}");
                $scope.loaded = false;
                $scope.loading = false;
            });
    }
};

TreeRowController.$inject = ['$scope', '$http', '$timeout', '$element'];
app.controller('TreeRowController', TreeRowController);


function TreeRowDirective(RecursionHelper) {
    return {
        templateUrl: "/services/ng/tree/_TreeRow.html",
        scope: {
            nodeIndex: "@"
        },
        controller: TreeRowController,
        compile: RecursionHelper.compile // I don't have a link function, so I may as well pass through the compile
    };
}

TreeRowDirective.$inject = ['RecursionHelper'];
app.directive('treeRow', TreeRowDirective);

function navigateSubnodesToPath($scope, data) {
    var newnodes = [];
    var oldnodes = [];

    for (var i in $scope.nodes) {
        var oldnode = $scope.nodes[i];
        oldnodes[oldnode.node.id] = oldnode;
    }

    var thisIsTheLastFetched = true;

    for (var i in data.subnode) {
        var datanode = data.subnode[i];

        if (datanode.fetched) {
            thisIsTheLastFetched = false;
        }

        if (oldnodes[datanode.node.id]) {
            var oldnode = oldnodes[datanode.node.id];
            newnodes.push(oldnode);

            oldnode.scope.setNodeAndDataFields(datanode);

            if (datanode.fetched) {
                oldnode.scope.open = true; // force open

                // oldnode.scope gets set in the TreeRow constructor
                navigateSubnodesToPath(oldnode.scope, datanode);
            }
        }
        else {
            // the TreeRow constructor will take care of everything else.
            newnodes.push(datanode);
        }
    }

    $scope.nodes = newnodes;

    if (thisIsTheLastFetched && scope.treeElement) {
        $scope.$$postDigest(function () {
            $scope.nameElement.scrollintoview()
        });
        $scope.treeScope.setSelected($scope);
    }

}

function navigateNodeToPath($scope, data) {
    for (; ;) {
        var nodes = $scope.nodes;

        /*
         * Ok. For each data.subnode.
         *
         * Find if the node id is in data.subnodes.
         *
         *
         *
         *
         *
         */


        break;
    }

}

