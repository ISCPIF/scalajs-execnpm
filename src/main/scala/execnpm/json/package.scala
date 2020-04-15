package execnpm

import java.io.{ Reader, Writer }

/**
 * Some type-class lightweight wrappers around simple-json.
 *
 *  They allow to write `xyz.toJSON` to obtain classes that can be
 *  serialized by simple-json and `fromJSON[T](xyz)` to get an
 *  object back.
 */
package object json {
  private[execnpm] type JSON = Impl.Repr

  private[execnpm] implicit class JSONPimp[T: JSONSerializer](x: T) {
    def toJSON: JSON = implicitly[JSONSerializer[T]].serialize(x)
  }

  private[execnpm] def fromJSON[T](x: JSON)(implicit d: JSONDeserializer[T]): T =
    d.deserialize(x)

  private[execnpm] def writeJSON(x: JSON, writer: Writer): Unit =
    Impl.serialize(x, writer)

  private[execnpm] def jsonToString(x: JSON): String =
    Impl.serialize(x)

  private[execnpm] def readJSON(str: String): JSON =
    Impl.deserialize(str)

  private[execnpm] def readJSON(reader: Reader): JSON =
    Impl.deserialize(reader)

}
