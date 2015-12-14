lazy val commonSettings = Seq(
  organization := "edu.illinois.i3.htrc.apps",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq("-feature", "-language:postfixOps", "-target:jvm-1.8"),
  resolvers += "I3 Repository" at "http://nexus.htrc.illinois.edu/content/groups/public"
  // wartremoverWarnings ++= Warts.all
)

lazy val pairtree_to_text = (project in file(".")).
  enablePlugins(JavaAppPackaging).
  settings(commonSettings: _*).
  settings(
    name := "pairtree-to-text",
    version := "1.2-SNAPSHOT",
    libraryDependencies ++= Seq(
        "org.rogach"                    %% "scallop"              % "0.9.5",
        "org.scala-lang.modules"        %% "scala-xml"            % "1.0.5",
        "com.jsuereth"                  %% "scala-arm"            % "1.4",
        "edu.illinois.i3.htrc.tools"    %  "htrc-pairtree-helper" % "1.5",
        "org.scalatest"                 %% "scalatest"            % "2.2.4"      % Test
      )
  )
