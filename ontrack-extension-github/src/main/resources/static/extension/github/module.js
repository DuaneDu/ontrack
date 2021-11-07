angular.module('ontrack.extension.github', [
    'ot.service.core',
    'ot.service.graphql',
    'ot.service.configuration',
    'ot.service.form',
    'ot.service.task'
])
    .config(function ($stateProvider) {
        $stateProvider.state('github-ingestion-hook-payloads', {
            url: '/extension/github/ingestion-hook-payloads',
            templateUrl: 'extension/github/ingestion-hook-payloads.tpl.html',
            controller: 'GitHubIngestionHookPayloadsCtrl'
        });
    })
    .controller('GitHubIngestionHookPayloadsCtrl', function ($scope, ot, otGraphqlService, otTaskService) {
        const view = ot.view();
        view.title = 'GitHub Ingestion Hook Payloads';
        view.description = 'List of payloads received and processed by the GitHub Ingestion Hook.';
        view.commands = [];
        view.breadcrumbs = ot.homeBreadcrumbs();

        const query = `
            query GetPayloads(
                $offset: Int!, 
                $size: Int!,
                $statuses: [IngestionHookPayloadStatus!]
            ) {
                gitHubIngestionHookPayloadStatuses
                gitHubIngestionHookPayloads(
                    offset: $offset,
                    size: $size,
                    statuses: $statuses
                ) {
                    pageInfo {
                        totalSize
                        previousPage {
                            offset
                            size
                        }
                        nextPage {
                            offset
                            size
                        }
                    }
                    pageItems {
                        uuid
                        timestamp
                        gitHubEvent
                        status
                        completion
                    }
                }
            }
        `;

        const detailsQuery = `
            query GetPayloadDetail(
                $uuid: String!
            ) {
                gitHubIngestionHookPayloads(
                    uuid: $uuid,
                ) {
                    pageItems {
                        message
                        payload
                    }
                }
            }
        `;

        const variables = {
            offset: 0,
            size: 20,
            statuses: null,
        };

        $scope.filter = {
            statuses: {}
        };

        $scope.loadingPayloads = true;

        const loadPayloads = () => {
            $scope.loadingPayloads = true;
            otGraphqlService.pageGraphQLCall(query, variables).then(data => {
                $scope.statuses = data.gitHubIngestionHookPayloadStatuses;
                $scope.payloads = data.gitHubIngestionHookPayloads.pageItems;
                $scope.pageInfo = data.gitHubIngestionHookPayloads.pageInfo;
            }).finally(() => {
                $scope.loadingPayloads = false;
            });
        };

        loadPayloads();

        $scope.onStatusSelection = () => {
            variables.statuses = [];
            $scope.statuses.forEach((status) => {
                if ($scope.filter.statuses[status] === true) {
                    variables.statuses.push(status);
                }
            });
            if (variables.statuses.length === 0) {
                variables.statuses = null;
            }
            loadPayloads();
        };

        const autoReloadKey = 'github-ingestion-hook-payloads-auto-reload';
        const storedAutoReload = localStorage.getItem(autoReloadKey);
        if (storedAutoReload !== null) {
            $scope.autoReload = storedAutoReload;
        } else {
            $scope.autoReload = false;
        }

        const interval = 10 * 1000; // 10 seconds
        const taskName = 'GitHub Ingestion Hook Payloads';
        const registerReload = () => {
            otTaskService.register(taskName, loadPayloads, interval);
        };

        if ($scope.autoReload) {
            registerReload();
        }

        $scope.toggleAutoReload = () => {
            $scope.autoReload = !$scope.autoReload;
            if ($scope.autoReload) {
                localStorage.setItem(autoReloadKey, $scope.autoReload);
                registerReload();
            } else {
                localStorage.removeItem(autoReloadKey);
                otTaskService.stop(taskName);
            }
        };

        $scope.topPayloads = () => {
            variables.offset = 0;
            variables.size = 20;
            loadPayloads();
        };

        $scope.newerPayloads = () => {
            if ($scope.pageInfo.previousPage) {
                variables.offset = $scope.pageInfo.previousPage.offset;
                variables.size = $scope.pageInfo.previousPage.size;
                loadPayloads();
            }
        };

        $scope.olderPayloads = () => {
            if ($scope.pageInfo.nextPage) {
                variables.offset = $scope.pageInfo.nextPage.offset;
                variables.size = $scope.pageInfo.nextPage.size;
                loadPayloads();
            }
        };

        $scope.toggleDetails = (payload) => {
            if (!payload.loadingDetails) {
                payload.showDetails = !payload.showDetails;
                if (payload.showDetails && !payload.details) {
                    // Loading the details
                    payload.loadingDetails = true;
                    otGraphqlService.pageGraphQLCall(detailsQuery, {uuid: payload.uuid}).then(data => {
                        payload.details = data.gitHubIngestionHookPayloads.pageItems[0];
                        payload.details.payloadJson = JSON.stringify(payload.details.payload, null, 3);
                    }).finally(() => {
                        payload.loadingDetails = false;
                    });
                }
            }
        };

    })
    .config(function ($stateProvider) {
        $stateProvider.state('github-configurations', {
            url: '/extension/github/configurations',
            templateUrl: 'extension/github/github.configurations.tpl.html',
            controller: 'GitHubConfigurationsCtrl'
        });
    })
    .controller('GitHubConfigurationsCtrl', function ($scope, $http, ot, otFormService, otAlertService, otConfigurationService, otGraphqlService) {
        const view = ot.view();
        view.title = 'GitHub configurations';
        view.description = 'Management of the GitHub configurations.';
        view.commands = [];

        // Query to get the list of configurations
        const query = `
            {
                gitHubConfigurations {
                    name
                    url
                    authenticationType
                    rateLimits {
                        core {
                            limit
                            used
                        }
                        graphql {
                            limit
                            used
                        }
                    }
                    user
                    appId
                    appInstallationAccountName
                    appToken {
                        valid
                        createdAt
                        validUntil
                    }
                    links {
                        _update
                        _delete
                    }
                }
            }
        `;

        // Loading the configurations
        $scope.loadingConfigurations = true;
        function load() {
            $scope.loadingConfigurations = true;
            otGraphqlService.pageGraphQLCall(query).then((data) => {
                $scope.configurations = data.gitHubConfigurations;
                view.commands = [
                    {
                        id: 'github-configuration-create',
                        name: "Create a configuration",
                        cls: 'ot-command-new',
                        action: $scope.createConfiguration
                    },
                    ot.viewCloseCommand('/home')
                ];
            }).finally(() => {
                $scope.loadingConfigurations = false;
            });
        }

        load();

        // Creating a configuration
        $scope.createConfiguration = function () {
            otFormService.display({
                uri: '/extension/github/configurations/create',
                title: "GitHub configuration",
                buttons: [ otConfigurationService.testButton('/extension/github/configurations/test') ],
                submit: function (data) {
                    return ot.call($http.post('/extension/github/configurations/create', data));
                }
            }).then(load);
        };

        // Deleting a configuration
        $scope.deleteConfiguration = function (configuration) {
            otAlertService.confirm({
                title: 'GitHub configuration',
                message: "Do you really want to delete this GitHub configuration? Some projects may still refer to it."
            }).then(
                function success() {
                    ot.call($http.delete(configuration.links._delete)).then(load);
                }
            );
        };

        // Updating a configuration
        $scope.updateConfiguration = function (configuration) {
            otFormService.display({
                uri: configuration.links._update,
                title: "GitHub configuration",
                buttons: [ otConfigurationService.testButton('/extension/github/configurations/test') ],
                submit: function (data) {
                    return ot.call($http.put(configuration.links._update, data));
                }
            }).then(load);
        };
    })
;