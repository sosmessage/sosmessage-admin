$(document).ready(function() {
  setInterval(function() {
    var ele = $("#stats-requests")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.requestsStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }, 5000)

  setInterval(function() {
    var ele = $("#stats-users")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.usersStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }, 5000)

  setInterval(function() {
    var ele = $("#stats-apps")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.appsStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }, 5000)

});
