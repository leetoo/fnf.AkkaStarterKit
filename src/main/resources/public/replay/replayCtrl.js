angular.module('replay')
    .controller('replayCtrl', function($scope, Replay, ReplayComment, ReplayTag) {
        $scope.replays = [];
        $scope.newComment = {};
        $scope.newTag = {};

        $scope.replayRace = function(replay) {
            Replay.get({ tag:replay.tag });
        }
        
        // At the moment it's not possible to stop a specific replay
        $scope.stopRunningReplay = function() {
            Replay.stop({ tag: 'all' });
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
        
        $scope.removeTag = function(replay, tag) {
            var tagList = replay.metadata.tags;
            function byTagName(element, index, array) {
                element.name === tag.name;
            }
            var tagIndex = tagList.findIndex(byTagName);
            if(tagIndex !== undefined) {
                replay.metadata.tags.splice(tagIndex, 1);
            }
            
            ReplayTag.update({ tag: replay.tag }, tagList);
        }
        
        var init = function() {
            loadReplays();
        }
        
        var loadReplays = function() {
            $scope.replays = Replay.query();
        }
        
        init();
    });