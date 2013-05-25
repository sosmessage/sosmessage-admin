$(document).ready(function() {
  function loadRequests() {
    var ele = $("#stats-requests")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.requestsStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }

  function loadUsers() {
    var ele = $("#stats-users")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.usersStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }

  function loadApps() {
    var ele = $("#stats-apps")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.appsStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }

  function loadRandomMessages() {
    var ele = $("#stats-random-messages")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.randomMessagesStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }

  function loadBestMessages() {
    var ele = $("#stats-best-messages")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.bestMessagesStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }

  function loadWorstMessages() {
    var ele = $("#stats-worst-messages")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.worstMessagesStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }

  function loadVotedMessages() {
    var ele = $("#stats-voted-messages")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.votedMessagesStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }

  function loadContributedMessages() {
    var ele = $("#stats-contributed-messages")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.contributedMessagesStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }

  setInterval(loadRequests, 5000)
  setInterval(loadUsers, 30000)
  setInterval(loadApps, 60000)
  setInterval(loadRandomMessages, 10000)
  setInterval(loadBestMessages, 10000)
  setInterval(loadWorstMessages, 10000)
  setInterval(loadContributedMessages, 30000)

  loadRequests()
  loadUsers()
  loadApps()
  loadRandomMessages()
  loadBestMessages()
  loadWorstMessages()
  loadContributedMessages()

})
