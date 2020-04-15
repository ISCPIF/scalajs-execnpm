package execnpm

import java.nio.file.{ Files, Paths }

import execnpm.NpmDeps._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.jsdependencies.sbtplugin.JSDependenciesPlugin
import org.scalajs.jsdependencies.core.ResolvedJSDependency
import org.scalajs.sbtplugin.ScalaJSPlugin

import scala.collection.JavaConverters._
import sbt.Keys._
import sbt._

object ExecNpmPlugin extends AutoPlugin {

  override lazy val requires = JSDependenciesPlugin

  // Exported keys
  object autoImport {

    import KeyRanks._

    val npmDeps = settingKey[NpmDeps]("List of js dependencies to be fetched")

    val allNpmDeps = taskKey[NpmDeps]("json file containing all npm js dependencies collected from all dependencies")

    val dependencyFile = taskKey[File]("File containing all external js files")

    val cssFile = taskKey[File]("File containing all external js files")

    val npmUpdate = taskKey[File]("Fetch NPM dependencies")

    val jsonFile = taskKey[File]("package.json file path")

    val resolvedJSDependencies = taskKey[Attributed[Seq[ResolvedJSDependency]]]("JS dependencies after resolution.")
  }

  import ExecNpmPlugin.autoImport._

  override lazy val projectSettings = Seq(
    npmDeps in Compile := List.empty,

    allNpmDeps in Compile := List.empty,

    skip in JSDependenciesPlugin.autoImport.packageJSDependencies := false,

    cssFile := (target in Compile).value / "css",

    resolvedJSDependencies in Compile := {
      val logger = streams.value.log
      val prev = (resolvedJSDependencies in Compile).value

      // Fetch the js paths in node_modules
      val resources = {
        val nodeModules = (npmUpdate in Compile).value / "node_modules"

        val sortedNpmDeps = (allNpmDeps in Compile).value.distinct.sortBy(_.appendMode)

        (for {
          m <- sortedNpmDeps
          js <- m.resources
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

      val jss = resources.filter {
        _.ext == "js"
      }
      val csss = resources.filter {
        _.ext == "css"
      }

      val cssTarget = (cssFile in Compile).value / "deps.css"
      cssTarget.delete

      for (
        f <- csss
      ) yield {
        IO.append(cssTarget, Files.readAllBytes(Paths.get(f.toURI)))
      }

      val resolvedDependencies = jss.map { f =>
        ResolvedJSDependency.minimal(f.toPath)
      }

      prev.map(_ ++ resolvedDependencies)
    },
    //GoogleClosureCompiler
    //      val minified = (packageMinifiedJSDependencies in Compile).value
    //      val compiler = new Compiler()
    //
    //      val modules = List(new JSModule(minified.getName)).asJava
    //
    //      compiler.compileModules(List().asJava, modules, new CompilerOptions)
    //
    //      val f = minified.getAbsoluteFile
    //      // minified.delete()
    //      IO.write(f, compiler.toSource)
    //      println("FILLE  " + f.getAbsolutePath + " // " + compiler.toSource.size)
    //      f

    //
    dependencyFile := (JSDependenciesPlugin.autoImport.packageMinifiedJSDependencies in Compile).value,

    (products in Compile) := (products in Compile).dependsOn(npmDepsManifest).value) ++ perScalaJSStageSettings(fullOptJS) ++ perScalaJSStageSettings(fastOptJS)

  protected def perScalaJSStageSettings(stage: TaskKey[Attributed[File]]): Seq[Def.Setting[_]] =
    inConfig(Compile)(perConfigSettings)

  private lazy val perConfigSettings: Seq[Def.Setting[_]] = Seq(
    allNpmDeps := NpmDeps.collectFromClasspath((fullClasspath in Compile).value).distinct,

    jsonFile := Tasks.writeOnlyDepsPackageJson(
      (crossTarget in Compile).value,
      (allNpmDeps in Compile).value.map { dep => dep.module.split("/").head -> dep.version },
      fullClasspath.value,
      configuration.value,
      streams.value).file,

    npmUpdate := scalajsbundler.sbtplugin.NpmUpdateTasks.npmUpdate(
      new java.io.File(""), // Seems to be used with yarn only
      (crossTarget in Compile).value,
      jsonFile.value,
      false,
      JSDependenciesPlugin.autoImport.scalaJSNativeLibraries.value.data,
      streams.value,
      Seq(),
      Seq()))

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
