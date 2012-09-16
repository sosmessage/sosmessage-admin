$(document).ready(function() {
  jsRoutes.controllers.Statistics.requestsStats().ajax({
      success : function(data) {
        $("#stats-requests").html(data)
      }
  });
  jsRoutes.controllers.Statistics.usersStats().ajax({
      success : function(data) {
        $("#stats-users").html(data)
      }
  });
  jsRoutes.controllers.Statistics.appsStats().ajax({
      success : function(data) {
        $("#stats-apps").html(data)
      }
  });
});
