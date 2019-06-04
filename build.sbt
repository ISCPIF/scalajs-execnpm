val execnpm =
  project.in(file("."))
    .settings(
      sbtPlugin := true,
      name := "scalajs-execnpm",
      organization := "fr.iscpif",
      description := "Npm finder and js aggregator for Scala.js projects",
      addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.11.0")
    )

publishMavenStyle := true

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

licenses := Seq("Affero GPLv3" -> url("http://www.gnu.org/licenses/"))

homepage := Some(url("https://github.com/ISCPIF/scalajs-execnpm"))

scmInfo := Some(ScmInfo(url("https://github.com/ISCPIF/scalajs-execnpm.git"), "scm:git@github.com:ISCPIF/scalajs-execnpm.git"))

pomExtra := (
  <developers>
    <developer>
      <id>mathieu.leclaire</id>
      <name>Mathieu Leclaire</name>
    </developer>
  </developers>
  )

releasePublishArtifactsAction := PgpKeys.publishSigned.value

releaseVersionBump := sbtrelease.Version.Bump.Minor

releaseTagComment := s"Releasing ${(version in ThisBuild).value}"

releaseCommitMessage := s"Bump version to ${(version in ThisBuild).value}"

//libraryDependencies += "com.google.javascript" % "closure-compiler" % "v20190513"

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeRelease"),
  pushChanges
)

scalariformAutoformat := true