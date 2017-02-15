import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import Dependencies._

showCurrentGitBranch

git.useGitDescribe := true

lazy val commonSettings = Seq(
  organization := "org.hathitrust.htrc",
  organizationName := "HathiTrust Research Center",
  organizationHomepage := Some(url("https://www.hathitrust.org/htrc")),
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq(
    "-feature",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-target:jvm-1.7"
  ),
  javacOptions ++= Seq(
    "-source", "1.7",
    "-target", "1.7"
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

lazy val root = (project in file("."))
  .settings(
    publish      := {},
    publishLocal := {}
  )
  .aggregate(lib, app)

lazy val lib = (project in file("lib")).
  enablePlugins(GitVersioning, GitBranchPrompt).
  settings(commonSettings: _*).
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
      "org.hathitrust.htrc"           %% "running-headers"      % "0.7",
      "org.hathitrust.htrc"           %% "scala-utils"          % "2.1",
      "com.jsuereth"                  %% "scala-arm"            % "2.0",
      "org.hathitrust.htrc"           %  "pairtree-helper"      % "3.1"
        exclude("com.beust", "jcommander"),
      "org.scalacheck"                %% "scalacheck"           % "1.13.4"      % Test,
      "org.scalatest"                 %% "scalatest"            % "3.0.1"       % Test
    ),
    crossScalaVersions := Seq("2.12.1", "2.11.8")
  )

lazy val app = (project in file("app")).dependsOn(lib).
  enablePlugins(GitVersioning, GitBranchPrompt, JavaAppPackaging).
  settings(commonSettings: _*).
  //settings(spark("2.1.0"): _*).
  settings(spark_dev("2.1.0"): _*).
  settings(
    name := "pairtree-to-text",
    libraryDependencies ++= Seq(
      "org.rogach"                    %% "scallop"              % "2.1.0",
      "org.hathitrust.htrc"           %% "spark-utils"          % "1.0.2",
      "ch.qos.logback"                %  "logback-classic"      % "1.2.1",
      "org.codehaus.janino"           %  "janino"               % "3.0.6",
      "org.scalacheck"                %% "scalacheck"           % "1.13.4"      % Test,
      "org.scalatest"                 %% "scalatest"            % "3.0.1"       % Test
    ),
    publish      := {},
    publishLocal := {}
  )
