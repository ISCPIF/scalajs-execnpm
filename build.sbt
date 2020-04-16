import sbt.Keys._
import sbt._

val organisation = "org.openmole"

val execnpm =
    project.in(file("."))
    .settings(
      scalaVersion := "2.12.11",
      sbtPlugin := true,
      name := "scalajs-execnpm",
      organization := organisation,
      description := "Npm finder and js aggregator for Scala.js projects",
      addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.17.0"),
      addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.0")
    )

licenses := Seq("Affero GPLv3" -> url("http://www.gnu.org/licenses/"))

homepage := Some(url("https://github.com/openmole/scalajs-execnpm/"))

scmInfo := Some(ScmInfo(url("https://github.com/openmole/scalajs-execnpm.git"), "git@github.com:openmole/scalajs-execnpm.git"))

scalariformAutoformat := true

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseVersionBump := sbtrelease.Version.Bump.Minor
releaseTagComment := s"Releasing ${(version in ThisBuild).value}"
releaseCommitMessage := s"Bump version to ${(version in ThisBuild).value}"
sonatypeProfileName := organisation
publishConfiguration := publishConfiguration.value.withOverwrite(true)

publishTo := sonatypePublishToBundle.value
publishMavenStyle := true
autoCompilerPlugins := true

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  //setReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

pomExtra := (
  <developers>
    <developer>
      <id>mathieu.leclaire</id>
      <name>Mathieu Leclaire</name>
    </developer>
  </developers>
  )
