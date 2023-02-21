angular.module('ontrack.extension.scm', [
    'ot.service.form'
])
    .directive('otScmChangelogBuild', function () {
        return {
            restrict: 'E',
            templateUrl: 'extension/scm/directive.scmChangelogBuild.tpl.html',
            scope: {
                scmBuildView: '='
            },
            transclude: true
        };
    })
    .directive('otScmChangelogIssueValidations', function () {
        return {
            restrict: 'E',
            templateUrl: 'extension/scm/directive.scmChangelogIssueValidations.tpl.html',
            scope: {
                changeLogIssue: '='
            }
        };
    })
    .directive('otExtensionScmIssueCommitBranchInfos', function () {
        return {
            restrict: 'E',
            templateUrl: 'extension/scm/directive.scmIssueCommitBranchInfos.tpl.html',
            scope: {
                infos: '='
            }
        };
    })
    .directive('otExtensionScmChangelogFilechangefilter', function (otScmChangelogFilechangefilterService) {
        return {
            restrict: 'E',
            templateUrl: 'extension/scm/directive.scmChangelogFilechangefilter.tpl.html',
            scope: {
                changeLog: '=',
                filterCallback: '='
            },
            link: function (scope) {
                // Loads the list of filters (async)
                scope.$watch('changeLog', function () {
                    if (scope.changeLog) {
                        otScmChangelogFilechangefilterService.loadFilters(scope.changeLog).then(function (filters) {
                            scope.filters = filters;
                        });
                    }
                });
            },
            controller: function ($scope) {
                // Submitting a pattern
                $scope.submitQuickPattern = function () {
                    var pattern = $scope.quickPattern;
                    if (pattern) {
                        $scope.submitPattern([pattern]);
                    }
                };
                $scope.saveQuickFilter = function () {
                    var pattern = $scope.quickPattern;
                    if (pattern) {
                        $scope.addFileFilter([pattern]);
                    }
                };
                $scope.filterDisplayName = function (filter) {
                    if (filter.shared) {
                        return filter.name + " (*)";
                    } else {
                        return filter.name;
                    }
                };
                $scope.filterCanShare = function (filter) {
                    return filter && !filter._update && otScmChangelogFilechangefilterService.remoteFilters._create;
                };
                $scope.filterCanUnshare = function (filter) {
                    return filter && filter._delete;
                };
                $scope.addFileFilter = function (patterns) {
                    otScmChangelogFilechangefilterService.addFilter($scope.changeLog, patterns).then(function (filter) {
                        // Adds the filter into the list and selects it
                        $scope.filters.push(filter);
                        $scope.selectedFilter = filter;
                    });
                };
                $scope.editFileFilter = function () {
                    if ($scope.selectedFilter) {
                        otScmChangelogFilechangefilterService.editFilter($scope.changeLog, $scope.selectedFilter).then(function (filter) {
                            $scope.selectedFilter.patterns = filter.patterns;
                            $scope.submitPattern(filter.patterns);
                        });
                    }
                };
                $scope.deleteFileFilter = function () {
                    if ($scope.selectedFilter) {
                        otScmChangelogFilechangefilterService.deleteFilter($scope.changeLog, $scope.selectedFilter);
                        $scope.filters.splice($scope.filters.indexOf($scope.selectedFilter), 1);
                        $scope.selectedFilter = undefined;
                    }
                };
                $scope.$watch('selectedFilter', function () {
                    if ($scope.selectedFilter) {
                        $scope.submitPattern($scope.selectedFilter.patterns);
                    } else {
                        $scope.submitPattern(undefined);
                    }
                });
                $scope.submitPattern = function (patterns) {
                    // Sets the function on the callback
                    if ($scope.filterCallback) {
                        $scope.filterCallback(otScmChangelogFilechangefilterService.filterFunction(patterns));
                    }
                };
                $scope.unselectPattern = function () {
                    $scope.quickPattern = '';
                    $scope.selectedFilter = undefined;
                    $scope.submitPattern(undefined);
                };
                $scope.shareFileFilter = otScmChangelogFilechangefilterService.shareFileFilter;
                $scope.diffFileFilter = function (changeLog, quickPattern, filter) {
                    $scope.diffComputing = true;
                    otScmChangelogFilechangefilterService.diffFileFilter(changeLog, quickPattern, filter).finally(function () {
                        $scope.diffComputing = false;
                    });
                };
            }
        };
    })
    .service('otScmChangelogFilechangefilterService', function ($q, $http, $modal, $interpolate,
                                                                ot, otFormService) {
        const self = {};

        // Deprecated, used only for legacy HTTP calls
        function loadStore(project) {
            return loadStoreByProjectId(project.id);
        }

        function loadStoreByProjectId(projectId) {
            const json = localStorage.getItem("fileChangeFilters_" + projectId);
            if (json) {
                return JSON.parse(json);
            } else {
                return {};
            }
        }

        // Deprecated, used only for legacy HTTP calls
        function saveStore(project, store) {
            saveStoreByProjectId(project.id, store);
        }

        function saveStoreByProjectId(projectId, store) {
            localStorage.setItem("fileChangeFilters_" + projectId, JSON.stringify(store));
        }

        function patternMatch(pattern, path) {
            var re = pattern
                .replace(/\*\*/g, '$MULTI$')
                .replace(/\*/g, '$SINGLE$')
                .replace(/\$SINGLE\$/g, '[^\/]+')
                .replace(/\$MULTI\$/g, '.*');
            re = '^' + re + '$';
            var rx = new RegExp(re);
            return rx.test(path);
        }

        self.initFilterConfig = function () {
            var changeLogFileFilterConfig = {};
            changeLogFileFilterConfig.callback = function (filterFunction) {
                changeLogFileFilterConfig.filterFunction = filterFunction;
            };
            changeLogFileFilterConfig.filter = function (changeLogFile) {
                if (changeLogFileFilterConfig.filterFunction) {
                    return changeLogFileFilterConfig.filterFunction(changeLogFile.path);
                } else {
                    return true;
                }
            };
            return changeLogFileFilterConfig;
        };

        self.loadFilters = function (changeLog) {
            var d = $q.defer();
            // Loading shared filters
            ot.pageCall($http.get(changeLog._changeLogFileFilters)).then(function (remoteFilters) {
                // Local filters
                var store = loadStore(changeLog.project);
                // Expansion into objects
                var index = {};
                angular.forEach(store, function (patterns, name) {
                    index[name] = {
                        name: name,
                        patterns: patterns
                    };
                });
                // API
                self.remoteFilters = remoteFilters;
                // Remote filters
                angular.forEach(remoteFilters.resources, function (filter) {
                    index[filter.name] = filter;
                    filter.shared = true;
                });
                // Flattening
                var filters = [];
                angular.forEach(index, function (filter) {
                    filters.push(filter);
                });
                // OK
                d.resolve(filters);
            });
            // OK
            return d.promise;
        };

        self.shareFileFilter = function (changeLog, filter) {
            ot.pageCall($http.post(self.remoteFilters._create, {
                name: filter.name,
                patterns: filter.patterns
            })).then(function (sharedFilter) {
                angular.copy(sharedFilter, filter);
                filter.shared = true;
            });
        };

        self.diffFileFilter = function (changeLog, quickPattern, filter) {
            var patterns = [];
            if (quickPattern) {
                patterns = [quickPattern];
            } else if (filter) {
                patterns = filter.patterns;
            }
            var params = {
                from: changeLog.scmBuildFrom.buildView.build.id,
                to: changeLog.scmBuildTo.buildView.build.id,
                patterns: patterns.join(',')
            };
            return ot.pageCall($http.get(changeLog._diff, {
                params: params
            })).then(function (diff) {
                var link = changeLog._diff;
                link += $interpolate('?from={{from}}&to={{to}}&patterns={{patterns}}')(params);
                $modal.open({
                    templateUrl: 'extension/scm/dialog.scmDiff.tpl.html',
                    controller: 'otExtensionScmDialogDiff',
                    resolve: {
                        config: function () {
                            return {
                                diff: diff,
                                link: link
                            };
                        }
                    }
                });
            });
        };

        self.itemFilterFunction = (patterns, itemFn) => function (item) {
            const path = itemFn(item);
            if (patterns) {
                return patterns.some(pattern => patternMatch(pattern, path));
            } else {
                return true;
            }
        };

        self.filterFunction = function (patterns) {
            return function (path) {
                if (patterns) {
                    return patterns.some(function (pattern) {
                        return patternMatch(pattern, path);
                    });
                } else {
                    return true;
                }
            };
        };

        // Deprecated, used only for legacy HTTP calls
        self.addFilter = function (changeLog, patterns) {
            return self.addFilterByProjectId(changeLog.project.id, patterns);
        };

        self.addFilterByProjectId = function (projectId, patterns) {
            // Form configuration
            const form = {
                fields: [{
                    name: 'name',
                    type: 'text',
                    label: "Name",
                    help: "Name to use to save the filter.",
                    required: true,
                    regex: '.*'
                }, {
                    name: 'patterns',
                    type: 'memo',
                    label: "Filter(s)",
                    help: "List of ANT-like patterns (one per line).",
                    required: true,
                    value: patterns ? patterns.join('\n') : ''
                }]
            };
            // Shows the dialog
            return otFormService.display({
                form: form,
                title: "Create file change filter",
                submit: function (data) {
                    // Loads the store
                    const store = loadStoreByProjectId(projectId);
                    // Controlling the name
                    if (store[data.name]) {
                        return "Filter with name " + data.name + " already exists.";
                    }
                    // Parsing the patterns
                    const patterns = data.patterns.split('\n').map(function (it) {
                        return it.trim();
                    });
                    // Saves the filter
                    store[data.name] = patterns;
                    saveStoreByProjectId(projectId, store);
                    // Returns the filter
                    return {
                        name: data.name,
                        patterns: patterns
                    };
                }
            });
        };

        self.editFilter = function (changeLog, filter) {
            // Form configuration
            var form = {
                fields: [{
                    name: 'name',
                    type: 'text',
                    label: "Name",
                    help: "Name to use to save the filter.",
                    required: true,
                    readOnly: true,
                    regex: '.*',
                    value: filter.name
                }, {
                    name: 'patterns',
                    type: 'memo',
                    label: "Filter(s)",
                    help: "List of ANT-like patterns (one per line).",
                    required: true,
                    value: filter.patterns.join('\n')
                }]
            };
            // Shows the dialog
            return otFormService.display({
                form: form,
                title: "Edit file change filter",
                submit: function (data) {
                    // Parsing the patterns
                    var patterns = data.patterns.split('\n').map(function (it) {
                        return it.trim();
                    });
                    // Saves the filter
                    var store = loadStore(changeLog.project);
                    store[filter.name] = patterns;
                    saveStore(changeLog.project, store);
                    var raw = {
                        name: filter.name,
                        patterns: patterns
                    };
                    // Remote change
                    if (filter._update) {
                        ot.call($http.put(filter._update, raw));
                    }
                    // Returns the filter
                    return raw;
                }
            });
        };

        self.deleteFilter = function (changeLog, filter) {
            // Local changes
            var store = loadStore(changeLog.project);
            delete store[filter.name];
            saveStore(changeLog.project, store);
            // Remote changes
            if (filter._delete) {
                ot.call($http.delete(filter._delete));
            }
        };

        return self;
    })
    .service('otScmChangeLogService', function ($http, $modal, $interpolate, ot) {
        var self = {};

        function storeExportRequest(projectId, exportRequest) {
            localStorage.setItem(
                'issueExportConfig_' + projectId,
                JSON.stringify(exportRequest)
            );
        }

        function loadExportRequest(projectId) {
            var json = localStorage.getItem('issueExportConfig_' + projectId);
            if (json) {
                return JSON.parse(json);
            } else {
                return {
                    grouping: []
                };
            }
        }

        self.displayChangeLogExport = function (config) {

            var projectId = config.changeLog.project.id;

            $modal.open({
                templateUrl: 'extension/scm/scmChangeLogExport.tpl.html',
                controller: function ($scope, $modalInstance) {
                    $scope.config = config;
                    // Export request
                    $scope.exportRequest = loadExportRequest(projectId);
                    // Loading the export formats
                    ot.call($http.get(config.exportFormatsLink)).then(function (exportFormatsResources) {
                        $scope.exportFormats = exportFormatsResources.resources;
                    });

                    // Closing the dialog
                    $scope.cancel = function () {
                        $modalInstance.dismiss('cancel');
                    };

                    // Group management
                    $scope.addGroup = function () {
                        // Adds an empty group
                        $scope.exportRequest.grouping.push({
                            name: '',
                            types: ''
                        });
                    };
                    // Removing a group
                    $scope.removeGroup = function (groups, group) {
                        var idx = groups.indexOf(group);
                        if (idx >= 0) {
                            groups.splice(idx, 1);
                        }
                    };

                    // Export generation
                    $scope.doExport = function () {

                        // Request
                        var request = {
                            from: config.changeLog.scmBuildFrom.buildView.build.id,
                            to: config.changeLog.scmBuildTo.buildView.build.id
                        };

                        // Permalink
                        var url = config.exportIssuesLink;
                        url += $interpolate('?from={{from}}&to={{to}}')(request);

                        // Format
                        if ($scope.exportRequest.format) {
                            request.format = $scope.exportRequest.format;
                            url += $interpolate('&format={{format}}')(request);
                        }

                        // Grouping
                        if ($scope.exportRequest.grouping.length > 0) {
                            var grouping = '';
                            for (var i = 0; i < $scope.exportRequest.grouping.length; i++) {
                                if (i > 0) {
                                    grouping += '|';
                                }
                                var groupSpec = $scope.exportRequest.grouping[i];
                                grouping += groupSpec.name + '=' + groupSpec.types;
                            }
                            grouping = encodeURIComponent(grouping);
                            request.grouping = grouping;
                            url += $interpolate('&grouping={{grouping}}')(request);
                        }

                        // Exclude
                        if ($scope.exportRequest.exclude) {
                            request.exclude = $scope.exportRequest.exclude;
                            url += $interpolate('&exclude={{exclude}}')(request);
                        }

                        // Call
                        $scope.exportCalling = true;
                        ot.call($http.get(url)).then(function success(exportedIssues) {
                            $scope.exportCalling = false;
                            $scope.exportError = '';
                            $scope.exportContent = exportedIssues;
                            $scope.exportPermaLink = url;
                            storeExportRequest(projectId, $scope.exportRequest);
                        }, function error(message) {
                            $scope.exportCalling = false;
                            $scope.exportError = message.content;
                            $scope.exportContent = '';
                            $scope.exportPermaLink = '';
                        });
                    };

                }
            });
        };

        return self;
    })
    .controller('otExtensionScmDialogDiff', function ($scope, $modalInstance, config) {
        // General configuration
        $scope.config = config;
        // Cancelling the dialog
        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    })
    .config(function ($stateProvider) {
        $stateProvider.state('scm-catalog', {
            url: '/extension/scm/catalog',
            templateUrl: 'extension/scm/catalog.tpl.html',
            controller: 'CatalogCtrl'
        });
    })
    .controller('CatalogCtrl', function ($stateParams, $scope, $http, ot, otGraphqlService) {
        $scope.loadingCatalog = true;

        const view = ot.view();
        view.title = "SCM Catalog";
        view.breadcrumbs = ot.homeBreadcrumbs();
        view.commands = [
            ot.viewCloseCommand('/home')
        ];

        $scope.queryVariables = {
            offset: 0,
            size: 30,
            scm: "",
            config: "",
            repository: "",
            project: "",
            link: "ENTRY",
            beforeLastActivity: null,
            afterLastActivity: null,
            beforeCreatedAt: null,
            afterCreatedAt: null,
            team: null,
            sortOn: null,
            sortAscending: true
        };

        $scope.filterLinks = {
            ENTRY: "Only SCM entries",
            ALL: "All entries and orphan projects",
            LINKED: "Linked entries only",
            UNLINKED: "Unlinked entries only",
            ORPHAN: "Orphan projects only"
        };

        const query = `
            query CatalogInfo($offset: Int!, $size: Int!, $scm: String, $config: String, $repository: String, $link: String, $project: String, $sortOn: String, $sortAscending: Boolean, $beforeLastActivity: String, $afterLastActivity: String, $beforeCreatedAt: String, $afterCreatedAt: String, $team: String) {
                scmCatalog(offset: $offset, size: $size, scm: $scm, config: $config, repository: $repository, link: $link, project: $project, sortOn: $sortOn, sortAscending: $sortAscending, beforeLastActivity: $beforeLastActivity, afterLastActivity: $afterLastActivity, beforeCreatedAt: $beforeCreatedAt, afterCreatedAt: $afterCreatedAt, team: $team) {
                    pageInfo {
                      totalSize
                      currentOffset
                      currentSize
                      pageTotal
                      pageIndex
                      nextPage {
                        offset
                        size
                      }
                      previousPage {
                        offset
                        size
                      }
                    }
                    pageItems {
                      entry {
                        scm
                        config
                        repository
                        repositoryPage
                        lastActivity
                        createdAt
                        timestamp
                        teams {
                          id
                          name
                          description
                          url
                          role
                        }
                      }
                      project {
                        id
                        name
                        links {
                          _page
                        }
                      }
                    }
                }
            }
        `;

        $scope.catalogSortOn = 'REPOSITORY';
        $scope.catalogSortAscending = true;

        $scope.changeCatalogSorting = (property) => {
            if ($scope.catalogSortOn === property) {
                $scope.catalogSortAscending = !$scope.catalogSortAscending;
            } else {
                $scope.catalogSortOn = property;
            }
            loadCatalog();
        };

        const loadCatalog = () => {
            $scope.loadingCatalog = true;

            if ($scope.queryVariables.repository && $scope.queryVariables.repository.indexOf("*") < 0) {
                $scope.queryVariables.repository = `.*${$scope.queryVariables.repository}.*`;
            }

            if ($scope.queryVariables.project && $scope.queryVariables.project.indexOf("*") < 0) {
                $scope.queryVariables.project = `.*${$scope.queryVariables.project}.*`;
            }

            $scope.queryVariables.sortOn = $scope.catalogSortOn;
            $scope.queryVariables.sortAscending = $scope.catalogSortAscending;

            otGraphqlService.pageGraphQLCall(query, $scope.queryVariables).then(data => {
                $scope.data = data;
            }).finally(() => {
                $scope.loadingCatalog = false;
            });
        };

        // Loads the issues
        loadCatalog();
        $scope.loadCatalog = loadCatalog;

        // Clearing the filter
        $scope.clearCatalogFilter = function () {
            $scope.queryVariables.scm = "";
            $scope.queryVariables.config = "";
            $scope.queryVariables.repository = "";
            $scope.queryVariables.project = "";
            $scope.queryVariables.link = "ENTRY";
            $scope.queryVariables.team = null;
            loadCatalog();
        };

        // Navigating
        $scope.navigate = pageRequest => {
            $scope.queryVariables.offset = pageRequest.offset;
            loadCatalog();
        };

    })
    .config(function ($stateProvider) {
        $stateProvider.state('scm-catalog-chart-teams', {
            url: '/extension/scm/catalog/chart/teams',
            templateUrl: 'extension/scm/catalog-chart-teams.tpl.html',
            controller: 'CatalogChartTeamsCtrl'
        });
    })
    .controller('CatalogChartTeamsCtrl', function ($scope, ot, otGraphqlService) {
        const view = ot.view();
        view.title = "SCM Catalog Chart Teams";
        view.breadcrumbs = ot.homeBreadcrumbs().concat([
            ['SCM Catalog', '#/extension/scm/catalog']
        ]);
        view.commands = [
            ot.viewCloseCommand('/extension/scm/catalog')
        ];


        const query = `{
            scmCatalogTeams {
                id
                name
                entryCount
            }
        }`;

        // Chart options
        const yTeams = [];
        const xTeams = [];
        const options = {
            title: {
                text: 'Number of repositories per team'
            },
            tooltip: {
                trigger: 'axis',
                axisPointer: {
                    type: 'shadow'
                }
            },
            legend: {
                data: ['Number of repositories']
            },
            grid: {
                left: '3%',
                right: '4%',
                bottom: '3%',
                containLabel: true
            },
            xAxis: {
                type: 'value',
                boundaryGap: [0, 0.01]
            },
            yAxis: {
                data: yTeams
            },
            series: [
                {
                    type: 'bar',
                    data: xTeams,
                    label: {
                        show: true
                    }
                }
            ]
        };

        // Computing the options
        $scope.options = () => {
            return otGraphqlService.pageGraphQLCall(query).then(data => {
                // Sorting teams by descending entry count
                const teams = data.scmCatalogTeams.sort((teamA, teamB) => {
                    return teamA.entryCount - teamB.entryCount;
                });
                // Y bars ==> IDs of the teams
                // X bar values ==> entry count
                teams.forEach(team => {
                    yTeams.push(team.id);
                    xTeams.push(team.entryCount);
                });
                // Returns the options
                return options;
            });
        };
    })
    .config(function ($stateProvider) {
        $stateProvider.state('scm-catalog-chart-team-stats', {
            url: '/extension/scm/catalog/chart/team-stats',
            templateUrl: 'extension/scm/catalog-chart-team-stats.tpl.html',
            controller: 'CatalogChartTeamStatsCtrl'
        });
    })
    .controller('CatalogChartTeamStatsCtrl', function ($scope, ot, otGraphqlService) {
        const view = ot.view();
        view.title = "SCM Catalog Chart Team Stats";
        view.breadcrumbs = ot.homeBreadcrumbs().concat([
            ['SCM Catalog', '#/extension/scm/catalog']
        ]);
        view.commands = [
            ot.viewCloseCommand('/extension/scm/catalog')
        ];

        // Initial chart options
        const y = [];
        const x = [];
        const options = {
            title: {
                text: 'Number of repositories having a given count of teams'
            },
            tooltip: {
                trigger: 'axis',
                axisPointer: {
                    type: 'shadow'
                }
            },
            grid: {
                left: '3%',
                right: '4%',
                bottom: '3%',
                containLabel: true
            },
            xAxis: {
                type: 'value',
                boundaryGap: [0, 0.01]
            },
            yAxis: {
                data: y
            },
            series: [
                {
                    type: 'bar',
                    data: x,
                    label: {
                        show: true
                    }
                }
            ]
        };

        const query = `{
            scmCatalogTeamStats {
                teamCount
                entryCount
            }
        }`;

        // Computing the options
        $scope.options = () => {
            return otGraphqlService.pageGraphQLCall(query).then(data => {
                // Sorting the entries from the highest to the lowest count of teams
                const entries = data.scmCatalogTeamStats.sort((entryA, entryB) => {
                    return entryA.teamCount - entryB.teamCount;
                });
                // Y ==> team count
                // X ==> entry count
                entries.forEach(entry => {
                    y.push(entry.teamCount);
                    x.push(entry.entryCount);
                });
                // Returns the options
                return options;
            });
        };

    })
;