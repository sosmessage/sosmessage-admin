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

  function loadIOSRequests() {
    var ele = $("#stats-requests-ios")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.iosRequestsStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }

  function loadAndroidRequests() {
    var ele = $("#stats-requests-android")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.androidRequestsStats().ajax({
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

  function loadIOSApps() {
    var ele = $("#stats-apps-ios")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.iosAppsStats().ajax({
        success : function(data) {
          ele.find(".js-stats-content").html(data)
          loading.hide()
        }
    })
  }

  function loadAndroidApps() {
    var ele = $("#stats-apps-android")
    var loading = ele.find(".js-loading")
    loading.show()
    jsRoutes.controllers.Statistics.androidAppsStats().ajax({
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
  setInterval(loadIOSRequests, 5000)
  setInterval(loadAndroidRequests, 5000)
  setInterval(loadUsers, 30000)
  setInterval(loadApps, 60000)
  setInterval(loadIOSApps, 60000)
  setInterval(loadAndroidApps, 60000)
  setInterval(loadRandomMessages, 10000)
  setInterval(loadBestMessages, 10000)
  setInterval(loadVotedMessages, 10000)
  setInterval(loadContributedMessages, 30000)

  loadRequests()
  loadIOSRequests()
  loadAndroidRequests()
  loadUsers()
  loadApps()
  loadIOSApps()
  loadAndroidApps()
  loadRandomMessages()
  loadBestMessages()
  loadVotedMessages()
  loadContributedMessages()

})
