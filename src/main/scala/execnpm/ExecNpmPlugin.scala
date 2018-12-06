package execnpm

import execnpm.NpmDeps._
import org.scalajs.core.tools.io.FileVirtualJSFile
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

object ExecNpmPlugin extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  // Exported keys
  object autoImport {

    val npmDeps = settingKey[NpmDeps]("List of js dependencies to be fetched")

    val allNpmDeps = taskKey[NpmDeps]("json file containing all npm js dependencies collected from all dependencies")

    val dependencyFile = taskKey[File]("File containing all external js files")

    val npmUpdate = taskKey[File]("Fetch NPM dependencies")

    val jsonFile = taskKey[File]("package.json file path")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    npmDeps in Compile := List.empty,

    allNpmDeps in Compile := List.empty,

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
          logger.info(s"Fetch $js ${m.version} in ${nodeModules / m.module}")
          val jsfile = get(nodeModules / m.module, js)

          jsfile match {
            case None => logger.error(s"$js not found")
            case _ =>
          }
          jsfile
        }).flatten

      }

      val resolvedDependencies = jss.map { f =>
        ResolvedJSDependency.minimal(FileVirtualJSFile(f))
      }

      prev.map(_ ++ resolvedDependencies)
    },

    dependencyFile := (packageMinifiedJSDependencies in Compile).value,

    (products in Compile) := (products in Compile).dependsOn(npmDepsManifest).value) ++ perScalaJSStageSettings(fullOptJS) ++ perScalaJSStageSettings(fastOptJS)

  protected def perScalaJSStageSettings(stage: TaskKey[Attributed[File]]): Seq[Def.Setting[_]] =
    inConfig(Compile)(perConfigSettings)

  private lazy val perConfigSettings: Seq[Def.Setting[_]] = Seq(
    allNpmDeps := NpmDeps.collectFromClasspath((fullClasspath in Compile).value).sorted.distinct,

    jsonFile := Tasks.writeOnlyDepsPackageJson(
      (crossTarget in Compile).value,
      (allNpmDeps in Compile).value.map { dep => dep.module -> dep.version },
      fullClasspath.value,
      configuration.value,
      streams.value).file,

    npmUpdate := scalajsbundler.sbtplugin.NpmUpdateTasks.npmUpdate(
      (crossTarget in Compile).value,
      jsonFile.value,
      false,
      scalaJSNativeLibraries.value.data,
      streams.value))

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
        (classDirectory in Compile).value / NpmDeps.manifestFileName)
    }

}
