angular.module('ontrack.extension.casc', [
    'ot.service.core',
    'ot.service.graphql'
])
    .config(function ($stateProvider) {
        $stateProvider.state('casc-control', {
            url: '/extension/casc/casc-control',
            templateUrl: 'extension/casc/casc-contrl.tpl.html',
            controller: 'CascControlCtrl'
        });
    })
    .controller('CascControlCtrl', function ($scope, ot, otGraphqlService) {
        const view = ot.view();
        view.title = "Configuration as Code";
        view.commands = [
            ot.viewCloseCommand('/home')
        ];

        $scope.loadSchema = () => {
            $scope.loadingSchema = true;
            otGraphqlService.pageGraphQLCall(`
                {
                    casc {
                        schema
                    }
                }
            `).then(data => {
                $scope.schema = data.casc.schema;
            }).finally(() => {
                $scope.loadingSchema = false;
            })
        };
    })
    .directive("otExtensionCascSchemaType", () => ({
        restrict: 'E',
        templateUrl: 'extension/casc/directive.casc-schema-type.tpl.html',
        scope: {
            type: '='
        }
    }))
    .directive("otExtensionCascSchemaTypeSimple", () => ({
        restrict: 'E',
        templateUrl: 'extension/casc/directive.casc-schema-type-simple.tpl.html',
        scope: {
            type: '='
        }
    }))
    // Tip for recursive directives
    .directive("otExtensionCascSchemaTypeObject", function ($compile) {
        return {
            restrict: 'E',
            template: '<div></div>',
            scope: {
                type: '='
            },
            link: function (scope, element) {
                if (angular.isDefined(scope.type)) {
                    $compile('<ot-extension-casc-schema-type type="type"</ot-extension-casc-schema-type>')(scope, function (cloned, scope) {
                        element.append(cloned);
                    });
                }
            }

        };
    })
;