import com.typesafe.sbt.{GitBranchPrompt, GitVersioning}

showCurrentGitBranch

git.useGitDescribe := true

lazy val commonSettings = Seq(
  organization := "edu.illinois.i3.htrc.apps",
  organizationName := "HathiTrust Research Center",
  organizationHomepage := Some(url("https://www.hathitrust.org/htrc")),
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-feature", "-language:postfixOps", "-target:jvm-1.8"),
  resolvers ++= Seq(
    "I3 Repository" at "http://nexus.htrc.illinois.edu/content/groups/public",
    Resolver.mavenLocal
  ),
  publishTo <<= isSnapshot { (isSnapshot: Boolean) =>
    val nexus = "https://nexus.htrc.illinois.edu/"
    if (isSnapshot)
      Some("HTRC Snapshots Repository" at nexus + "content/repositories/snapshots")
    else
      Some("HTRC Releases Repository"  at nexus + "content/repositories/releases")
  },
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  packageOptions in (Compile, packageBin) += Package.ManifestAttributes(
    ("Git-Sha", git.gitHeadCommit.value.getOrElse("N/A")),
    ("Git-Branch", git.gitCurrentBranch.value),
    ("Git-Version", git.gitDescribedVersion.value.getOrElse("N/A")),
    ("Git-Dirty", git.gitUncommittedChanges.value.toString),
    ("Build-Date", new java.util.Date().toString)
  )
)

lazy val pairtree_to_text = (project in file(".")).
  enablePlugins(GitVersioning, GitBranchPrompt, JavaAppPackaging).
  settings(commonSettings: _*).
  settings(
    name := "pairtree-to-text",
    version := "2.2-SNAPSHOT",
    description := "",
    licenses += "Apache2" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    libraryDependencies ++= Seq(
      "org.rogach"                    %% "scallop"              % "2.0.0",
      "org.scala-lang.modules"        %% "scala-xml"            % "1.0.5",
      "com.jsuereth"                  %% "scala-arm"            % "1.4",
      "edu.illinois.i3.htrc.tools"    %  "htrc-pairtree-helper" % "2.0"
        exclude("com.beust", "jcommander"),
      "org.scalatest"                 %% "scalatest"            % "2.2.4"      % Test
    )
  )
