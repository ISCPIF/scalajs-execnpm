package execnpm

import java.io.{ BufferedInputStream, FileInputStream }
import java.util.zip.ZipInputStream

import org.scalajs.core.tools.json._
import sbt._

import scalajsbundler.NpmDependencies

object NpmDeps {

  case class Dep(module: String, version: String, resources: List[String], appendMode: Boolean = false, overrideMode: Boolean = false)

  type NpmDeps = List[Dep]

  /** Name of the file containing the NPM deps */
  val manifestFileName = "NPM_DEPS"

  def empty: NpmDeps = List[Dep]()

  implicit val serializerDep: JSONSerializer[Dep] =
    new JSONSerializer[Dep] {
      def serialize(npmManifest: Dep): JSON =
        new JSONObjBuilder()
          .fld("module", npmManifest.module)
          .fld("version", npmManifest.version)
          .fld("resources", npmManifest.resources)
          .fld("appendMode", npmManifest.appendMode)
          .fld("overrideMode", npmManifest.overrideMode)
          .toJSON
    }

  implicit val deserializer: JSONDeserializer[Dep] =
    new JSONDeserializer[Dep] {
      def deserialize(json: JSON): Dep = {
        val obj = new JSONObjExtractor(json)
        Dep(
          obj.fld[String]("module"),
          obj.fld[String]("version"),
          obj.fld[List[String]]("resources"),
          obj.fld[Boolean]("appendMode"),
          obj.fld[Boolean]("overrideMode"))
      }
    }

  def filterOverrided(deps: NpmDeps) = {
    val (overriders, notoverriding) = deps.partition { _.overrideMode }
    val intersection = notoverriding.map { _.module }.intersect(overriders.map { _.module })
    val filtered = notoverriding.filterNot { no =>
      intersection.contains(no.module)
    }
    overriders ++ filtered
  }

  /**
   * @param cp Classpath
   * @return All the NPM dependencies found in the given classpath
   */
  def collectFromClasspath(cp: Def.Classpath): NpmDeps = {
    filterOverrided((
      for {
        cpEntry <- Attributed.data(cp) if cpEntry.exists
        results <- if (cpEntry.isFile && cpEntry.name.endsWith(".jar")) Seq({
          val stream = new ZipInputStream(new BufferedInputStream(new FileInputStream(cpEntry)))
          try {
            val deps = Iterator.continually(stream.getNextEntry())
              .takeWhile(_ != null)
              .filter(_.getName == NpmDeps.manifestFileName)
              .flatMap(_ => {
                fromJSON[NpmDeps](readJSON(IO.readStream(stream)))
              }).to[List]
            deps
          } finally {
            stream.close()
          }
        })
        else if (cpEntry.isDirectory) {
          (for {
            (file, _) <- Path.selectSubpaths(cpEntry, new ExactFilter(NpmDeps.manifestFileName))
          } yield {
            fromJSON[NpmDeps](readJSON(IO.read(file)))
          }).toList
        } else sys.error(s"Illegal classpath entry: ${cpEntry.absolutePath}")
      } yield results).toList.flatten)
  }

  def writeNpmDepsJson(npmDeps: NpmDeps, targetFile: File): File = {
    IO.write(targetFile, jsonToString(npmDeps.toJSON))
    targetFile
  }
}
