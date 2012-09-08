// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += Resolver.url(
  "sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")
)(Resolver.ivyStylePatterns)

resolvers += Resolver.url("Play", url("https://playframework2.ci.cloudbees.com/job/play2-integrationtest/ws/repository/local/"))(Resolver.ivyStylePatterns)

resolvers ++= Seq(
  Resolver.url("Typesafe Ivy Snapshots", url("http://repo.typesafe.com/typesafe/ivy-snapshots/"))(Resolver.ivyStylePatterns),
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/")

addSbtPlugin("play" % "sbt-plugin" % "2.1-SNAPSHOT")
