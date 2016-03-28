'use strict';

var simulatorApp = angular.module('simulatorApp',
    [
	'ngRoute',
    'ngResource',
    'ui.bootstrap',

	'carrera.commons',	
    'simulator',
    'replay'
    ]);

simulatorApp.config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
            when('/simulator', {
                templateUrl: 'simulator/simulator.html',
                controller: 'simulatorCtrl'
            }).
            when('/replay', {
               templateUrl: 'replay/replay.html',
               controller: 'replayCtrl' 
            }).
            otherwise({
                redirectTo: '/simulator'
            });
    }]);
    
simulatorApp.filter('newlines', function() {
   return function(text) {
       if(!text) {
           return text;
       }
       
       return text.replace(/\n\r?/g, '<br />');
   };
});

