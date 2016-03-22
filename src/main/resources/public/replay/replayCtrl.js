angular.module('replay')
    .controller('replayCtrl', function($scope, Replay, ReplayComment, ReplayTag) {
        $scope.replays = [];
        $scope.newComment = {};
        $scope.newTag = {};

        $scope.replayRace = function(replay) {
            Replay.get({ tag:replay.tag });
        }
        
        $scope.stopReplay = function(replay) {
            // Do nothing as of now
        }
        
        $scope.saveComment = function(replay) {
            var comment = new ReplayComment({ text: $scope.newComment.text });
            comment.$save({ tag:replay.tag }, function () {
                loadReplays();
                $scope.newComment.text = "";   
            }); 
        }
        
        $scope.addTag = function(replay) {
            replay.metadata.tags.push({ name: $scope.newTag.name });
            ReplayTag.update({ tag: replay.tag }, replay.metadata.tags);
            $scope.newTag.name = "";            
        }
        
        var init = function() {
            loadReplays();
        }
        
        var loadReplays = function() {
            $scope.replays = Replay.query();
        }
        
        init();
    });