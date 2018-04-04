import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import Dependencies._

showCurrentGitBranch

git.useGitDescribe := true

lazy val commonSettings = Seq(
  organization := "org.hathitrust.htrc",
  organizationName := "HathiTrust Research Center",
  organizationHomepage := Some(url("https://www.hathitrust.org/htrc")),
  scalaVersion := "2.11.12",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-language:postfixOps",
    "-language:implicitConversions"
  ),
  resolvers ++= Seq(
    "I3 Repository" at "http://nexus.htrc.illinois.edu/content/groups/public",
    Resolver.mavenLocal
  ),
  packageOptions in (Compile, packageBin) += Package.ManifestAttributes(
    ("Git-Sha", git.gitHeadCommit.value.getOrElse("N/A")),
    ("Git-Branch", git.gitCurrentBranch.value),
    ("Git-Version", git.gitDescribedVersion.value.getOrElse("N/A")),
    ("Git-Dirty", git.gitUncommittedChanges.value.toString),
    ("Build-Date", new java.util.Date().toString)
  ),
  description := "Tool that extracts full text from a HT volume stored in Pairtree by " +
      "concatenating the pages in the correct order, optionally performing additional " +
      "post-processing to identify running headers, fix end-of-line hyphenation, and " +
      "reformat the text.",
  licenses += "Apache2" -> url("http://www.apache.org/licenses/LICENSE-2.0")
)

lazy val `pairtree-to-text` = (project in file("."))
  .settings(
    publish      := {},
    publishLocal := {}
  )
  .aggregate(lib, app)

lazy val lib = (project in file("lib")).
  enablePlugins(GitVersioning, GitBranchPrompt).
  settings(commonSettings).
  settings(
    name := "pairtree-to-text",
    publishTo := {
      val nexus = "https://nexus.htrc.illinois.edu/"
      if (isSnapshot.value)
        Some("HTRC Snapshots Repository" at nexus + "content/repositories/snapshots")
      else
        Some("HTRC Releases Repository"  at nexus + "content/repositories/releases")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    libraryDependencies ++= Seq(
      "org.hathitrust.htrc"           %% "running-headers"      % "0.8",
      "org.hathitrust.htrc"           %% "scala-utils"          % "2.3.0",
      "com.jsuereth"                  %% "scala-arm"            % "2.0",
      "org.hathitrust.htrc"           %  "pairtree-helper"      % "3.1"
        exclude("com.beust", "jcommander"),
      "org.scalacheck"                %% "scalacheck"           % "1.13.5"      % Test,
      "org.scalatest"                 %% "scalatest"            % "3.0.5"       % Test
    ),
    crossScalaVersions := Seq("2.12.5", "2.11.12")
  )

lazy val app = (project in file("app")).dependsOn(lib).
  enablePlugins(GitVersioning, GitBranchPrompt, JavaAppPackaging).
  settings(commonSettings).
  //settings(spark("2.3.0")).
  settings(spark_dev("2.3.0")).
  settings(
    name := "pairtree-to-text-app",
    libraryDependencies ++= Seq(
      "org.rogach"                    %% "scallop"              % "3.1.2",
      "org.hathitrust.htrc"           %% "spark-utils"          % "1.0.2",
      "ch.qos.logback"                %  "logback-classic"      % "1.2.3",
      "org.codehaus.janino"           %  "janino"               % "3.0.8",
      "com.gilt"                      %% "gfc-time"             % "0.0.7",
      "com.github.nscala-time"        %% "nscala-time"          % "2.18.0",
      "org.scalacheck"                %% "scalacheck"           % "1.13.5"      % Test,
      "org.scalatest"                 %% "scalatest"            % "3.0.5"       % Test
    ),
    publish      := {},
    publishLocal := {},
    executableScriptName := "pairtree-to-text"
  )
