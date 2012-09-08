import sbt._
import sbt.Keys._

import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "sosmessage-admin"
    val appVersion      = "2.1-SNAPSHOT"

    val appDependencies = Seq(
      "org.mongodb" %% "casbah" % "2.4.1",
      "org.streum" %% "configrity-core" % "0.10.2",
      "commons-lang" % "commons-lang" % "2.6"
    )

    val buildSettings =
      Seq(
        scalacOptions ++= Seq("-unchecked", "-deprecation")
    )

    val main = PlayProject(appName, appVersion, appDependencies).settings(defaultScalaSettings: _*)
      .settings(buildSettings: _*)

}
