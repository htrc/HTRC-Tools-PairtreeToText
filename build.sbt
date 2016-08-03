lazy val commonSettings = Seq(
  organization := "edu.illinois.i3.htrc.apps",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-feature", "-language:postfixOps", "-target:jvm-1.8"),
  resolvers += "I3 Repository" at "http://nexus.htrc.illinois.edu/content/groups/public",
  publishTo <<= isSnapshot { (isSnapshot: Boolean) =>
    val nexus = "https://nexus.htrc.illinois.edu/"
    if (isSnapshot)
      Some("HTRC Snapshots Repository" at nexus + "content/repositories/snapshots")
    else
      Some("HTRC Releases Repository"  at nexus + "content/repositories/releases")
  },
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  // wartremoverWarnings ++= Warts.all
)

lazy val pairtree_to_text = (project in file(".")).
  enablePlugins(JavaAppPackaging).
  settings(commonSettings: _*).
  settings(
    name := "pairtree-to-text",
    version := "2.0",
    libraryDependencies ++= Seq(
        "org.rogach"                    %% "scallop"              % "2.0.0",
        "org.scala-lang.modules"        %% "scala-xml"            % "1.0.5",
        "com.jsuereth"                  %% "scala-arm"            % "1.4",
        "edu.illinois.i3.htrc.tools"    %  "htrc-pairtree-helper" % "2.0",
        "org.scalatest"                 %% "scalatest"            % "2.2.4"      % Test
      )
  )
