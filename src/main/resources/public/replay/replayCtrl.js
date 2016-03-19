angular.module('replay')
    .controller('replayCtrl', function($scope, Replay, ReplayComment) {
        $scope.replays = [];
        $scope.newComment = {};

        $scope.replayRace = function(replay) {
            Replay.get({ tag:replay.tag });
        }
        
        $scope.saveComment = function(replay) {
            var comment = new ReplayComment({ text: $scope.newComment.text });
            comment.$save({ tag:replay.tag }, function () {
                loadReplays();
                $scope.newComment.text = "";   
            }); 
        }
        
        var init = function() {
            loadReplays();
        }
        
        var loadReplays = function() {
            $scope.replays = Replay.query();
        }
        
        init();
    });