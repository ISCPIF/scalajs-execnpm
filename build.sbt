val execnpm =
  project.in(file("."))
    .settings(
      sbtPlugin := true,
      version := "0.2-SNAPSHOT",
      name := "scalajs-execnpm",
      organization := "fr.iscpif",
      description := "Npm finder and js aggregator for Scala.js projects",
      addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.11.0")
    )
