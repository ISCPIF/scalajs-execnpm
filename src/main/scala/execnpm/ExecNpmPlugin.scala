package execnpm

import execnpm.NpmDeps.NpmDeps
import org.scalajs.core.tools.io.FileVirtualJSFile
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

import scalajsbundler.sbtplugin.NpmDepsPlugin._
import scalajsbundler.sbtplugin.NpmDepsPlugin.autoImport._
import scalajsbundler.sbtplugin.{NpmDepsPlugin, PackageJsonTasks}

object ExecNpmPlugin extends AutoPlugin {

  override lazy val requires = NpmDepsPlugin

  // Exported keys
  object autoImport {

    val npmDeps = settingKey[NpmDeps]("List of js dependencies to be fetched")

    val allNpmDeps = taskKey[NpmDeps]("json file containing all npm js dependencies collected from all dependencies")

    val dependencyFile = taskKey[File]("File containing all external js files")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    npmDeps in Compile := List.empty,

    skip in packageJSDependencies := false,

    resolvedJSDependencies in Compile := {
      val logger = streams.value.log
      val prev = (resolvedJSDependencies in Compile).value

      // Fetch the js paths in node_modules
      val jss = {
        val nodeModules = (npmUpdate in Compile).value / "node_modules"

        (for {
          m <- (allNpmDeps in Compile).value
          js <- m.jsFiles
        } yield {
          logger.info(s"Fetch $js in ${nodeModules / m.module}")
          get(nodeModules / m.module, js)
        }).flatten

      }

      val resolvedDependencies = jss.map { f =>
        ResolvedJSDependency.minimal(FileVirtualJSFile(f))
      }

      prev.map(_ ++ resolvedDependencies)
    },

    dependencyFile := (packageMinifiedJSDependencies in Compile).value,

    (products in Compile) := (products in Compile).dependsOn(npmDepsManifest).value
  ) ++ perScalaJSStageSettings(fullOptJS) ++ perScalaJSStageSettings(fastOptJS)

  protected def perScalaJSStageSettings(stage: TaskKey[Attributed[File]]): Seq[Def.Setting[_]] = Seq(
    useYarn := false
  ) ++ inConfig(Compile)(perConfigSettings)


  private lazy val perConfigSettings: Seq[Def.Setting[_]] = Seq(
    allNpmDeps := NpmDeps.collectFromClasspath((fullClasspath in Compile).value),

    scalaJSBundlerPackageJson := PackageJsonTasks.writePackageJson(
      (crossTarget in npmUpdate).value,
      (allNpmDeps in Compile).value.map { dep => dep.module -> dep.version },
      Seq(),
      Map.empty,
      Map.empty,
      fullClasspath.value,
      configuration.value,
      "3",
      "2.7.1",
      streams.value
    )

  )

  private def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  private def get(path: File, jsFile: String) = {
    val files = recursiveListFiles(path)
    files.find(_.getName == jsFile)
  }

  /**
    * Writes the NpmDeps manifest file.
    */
  val npmDepsManifest: Def.Initialize[Task[File]] =
    Def.task {
      NpmDeps.writeNpmDepsJson(
        (npmDeps in Compile).value,
        (classDirectory in Compile).value / NpmDeps.manifestFileName
      )
    }

}
