import sbt._
import sbt.Keys._

import play.Project._

object ApplicationBuild extends Build {

    val appName         = "sosmessage-admin"
    val appVersion      = "2.1"

    val appDependencies = Seq(
      "org.mongodb" %% "casbah" % "2.5.0",
      "org.streum" %% "configrity-core" % "1.0.0",
      "commons-lang" % "commons-lang" % "2.6"
    )

    val buildSettings =
      Seq(
        scalacOptions ++= Seq("-unchecked", "-deprecation")
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(defaultScalaSettings: _*)
      .settings(buildSettings: _*).settings(
        resolvers ++= Seq(
          Resolver.file("Local Repository", file("/Users/troger/code/Play20/repository/local"))(Resolver.ivyStylePatterns),
          "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
          Resolver.url("Typesafe ivy-snapshots", url("http://repo.typesafe.com/typesafe/ivy-snapshots"))(Resolver.ivyStylePatterns),
          "Typesafe snapshots" at "http://repo.typesafe.com/typesafe/snapshots"
        ),
        templatesImport ++= Seq(
          "http.Context"
        )
      )

}
