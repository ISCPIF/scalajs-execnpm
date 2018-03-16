package execnpm

import scalajsbundler.util.{ Caching, JSON }
import org.scalajs.sbtplugin.ScalaJSPlugin._
import sbt._

import scalajsbundler.{ NpmDependencies, PackageJson }

object Tasks {

  def writeOnlyDepsPackageJson(
    targetDir: File,
    npmDependencies: Seq[(String, String)],
    fullClasspath: Seq[Attributed[File]],
    configuration: Configuration,
    streams: Keys.TaskStreams): scalajsbundler.BundlerFile.PackageJson = {

    val hash = Seq(
      configuration.name,
      npmDependencies.toString,
      fullClasspath.map(_.data.name).toString).mkString(",")

    val packageJsonFile = targetDir / "package.json"

    Caching.cached(
      packageJsonFile,
      hash,
      streams.cacheDirectory / s"scalajsbundler-package-json-${if (configuration == Compile) "main" else "test"}") { () =>
        writeDepsOnly(
          streams.log,
          packageJsonFile,
          fullClasspath,
          npmDependencies,
          configuration)
        ()
      }

    scalajsbundler.BundlerFile.PackageJson(packageJsonFile)
  }

  def writeDepsOnly(
    log: Logger,
    targetFile: File,
    fullClasspath: Seq[Attributed[File]],
    npmDependencies: Seq[(String, String)],
    currentConfiguration: Configuration) = {

    val npmManifestDependencies = scalajsbundler.NpmDependencies.collectFromClasspath(fullClasspath)
    val deps = dependencies(npmManifestDependencies, npmDependencies, currentConfiguration)

    val packageJson =
      JSON.obj(
        Seq("dependencies" -> JSON.objStr(PackageJson.resolveDependencies(deps, Map.empty, log))): _*)

    log.debug("Writing 'package.json'")
    IO.write(targetFile, packageJson.toJson)
    ()
  }

  def dependencies(
    npmManifestDependencies: NpmDependencies,
    npmDependencies: Seq[(String, String)],
    currentConfiguration: Configuration) = npmDependencies ++ (
    if (currentConfiguration == Compile) npmManifestDependencies.compileDependencies
    else npmManifestDependencies.testDependencies)
}
