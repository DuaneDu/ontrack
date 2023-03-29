angular.module('ontrack.extension.queue', [
    'ot.service.core',
    'ot.service.graphql'
])
    .config(function ($stateProvider) {
        $stateProvider.state('queue-records', {
            url: '/extension/queue/records',
            templateUrl: 'extension/queue/records.tpl.html',
            controller: 'QueueRecordsCtrl'
        });
    })
    .controller('QueueRecordsCtrl', function ($scope, $http, ot, otGraphqlService) {
        const view = ot.view();
        view.title = "Queue messages";
        view.breadcrumbs = ot.homeBreadcrumbs();
        view.commands = [
            ot.viewCloseCommand('/home')
        ];

        $scope.filter = {
            id: undefined
        };

        const query = `
            query QueueRecords(
                $id: String,
            ) {
                queueRecords(
                    id: $id,
                ) {
                    pageItems {
                        state
                        queuePayload {
                            id
                            processor
                            body
                        }
                        startTime
                        endTime
                        routingKey
                        queueName
                        actualPayload
                        exception
                        history {
                            state
                            time
                        }
                    }
                }
            }
        `;

        const loadRecords = () => {
            $scope.loading = true;
            const variables = {
                id: $scope.filter.id ? $scope.filter.id : null
            };
            otGraphqlService.pageGraphQLCall(query, variables).then(data => {
                $scope.messages = data.queueRecords.pageItems;
            }).finally(() => {
                $scope.loading = false;
            });
        };

        loadRecords();

        $scope.onClear = () => {
            $scope.filter.id = undefined;
            loadRecords();
        };

        $scope.onFilter = () => {
            loadRecords();
        };
    })
;