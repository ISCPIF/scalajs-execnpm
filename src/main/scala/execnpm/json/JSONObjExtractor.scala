package execnpm.json

import scala.collection.mutable

private[execnpm] class JSONObjExtractor(rawData: JSON) {
  private val data = Impl.toMap(rawData)

  def fld[T: JSONDeserializer](name: String): T =
    fromJSON[T](data(name))

  def opt[T: JSONDeserializer](name: String): Option[T] =
    data.get(name).map(fromJSON[T] _)
}
