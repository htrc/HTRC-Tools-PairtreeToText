import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}
import Dependencies._

showCurrentGitBranch

git.useGitDescribe := true

lazy val commonSettings = Seq(
  organization := "org.hathitrust.htrc",
  organizationName := "HathiTrust Research Center",
  organizationHomepage := Some(url("https://www.hathitrust.org/htrc")),
  scalaVersion := "2.12.8",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-language:postfixOps",
    "-language:implicitConversions"
  ),
  externalResolvers := Seq(
    Resolver.defaultLocal,
    Resolver.mavenLocal,
    "HTRC Nexus Repository" at "http://nexus.htrc.illinois.edu/content/groups/public",
  ),
  packageOptions in (Compile, packageBin) += Package.ManifestAttributes(
    ("Git-Sha", git.gitHeadCommit.value.getOrElse("N/A")),
    ("Git-Branch", git.gitCurrentBranch.value),
    ("Git-Version", git.gitDescribedVersion.value.getOrElse("N/A")),
    ("Git-Dirty", git.gitUncommittedChanges.value.toString),
    ("Build-Date", new java.util.Date().toString)
  ),
  wartremoverErrors ++= Warts.unsafe.diff(Seq(
    Wart.DefaultArguments,
    Wart.NonUnitStatements,
    Wart.Any,
    Wart.TryPartial
  ))
)

lazy val `pairtree-to-text` = (project in file(".")).
  enablePlugins(GitVersioning, GitBranchPrompt, JavaAppPackaging).
  settings(commonSettings).
  //settings(spark("2.4.3")).
  settings(spark_dev("2.4.3")).
  settings(
    name := "pairtree-to-text",
    description := "Tool that extracts full text from a HT volume stored in Pairtree by " +
      "concatenating the pages in the correct order, optionally performing additional " +
      "post-processing to identify running headers, fix end-of-line hyphenation, and " +
      "reformat the text.",
    licenses += "Apache2" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    libraryDependencies ++= Seq(
      "org.rogach"                    %% "scallop"              % "3.3.1",
      "org.hathitrust.htrc"           %% "data-model"           % "1.3.1",
      "org.hathitrust.htrc"           %% "spark-utils"          % "1.2.0",
      "ch.qos.logback"                %  "logback-classic"      % "1.2.3",
      "org.codehaus.janino"           %  "janino"               % "3.0.12",
      "com.gilt"                      %% "gfc-time"             % "0.0.7",
      "org.scalacheck"                %% "scalacheck"           % "1.14.0"      % Test,
      "org.scalatest"                 %% "scalatest"            % "3.0.8"       % Test
    )
  )
