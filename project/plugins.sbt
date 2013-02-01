// Comment to get more information during initialization
logLevel := Level.Warn

resolvers ++= Seq(
Resolver.file("Local Repository", file("/Users/troger/code/Play20/repository/local"))(Resolver.ivyStylePatterns),
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("Typesafe ivy-snapshots", url("http://repo.typesafe.com/typesafe/ivy-snapshots"))(Resolver.ivyStylePatterns),
  "Typesafe snapshots" at "http://repo.typesafe.com/typesafe/snapshots"
)

addSbtPlugin("play" % "sbt-plugin" % "2.1-RC3")
