package org.scalablytyped.converter.internal

final class Digest private (bytes: Array[Byte]) {
  lazy val hexString: String = Digest.asString(bytes)
}

object Digest {
  private def asString(buf: Array[Byte]): String =
    buf.map("%02x".format(_)).mkString

  trait Digestable[T] {
    def bytesFrom(t: T): Array[Byte]
  }

  object Digestable {
    def apply[T: Digestable]: Digestable[T] = implicitly

    implicit val StringDigestable: Digestable[String] =
      (t: String) => t.filterNot(_.isWhitespace).getBytes("UTF-8")
    implicit val ByteArrayDigestable: Digestable[Array[Byte]] =
      (bs: Array[Byte]) => bs
  }

  // Simple hash function for Scala.js compatibility (not cryptographically secure)
  private def simpleHash(data: Array[Byte]): Array[Byte] = {
    var hash = 0x811c9dc5
    for (b <- data) {
      hash ^= (b & 0xff)
      hash *= 0x01000193
    }

    // Convert to 16-byte array to mimic MD5 output size
    val result = Array.ofDim[Byte](16)
    for (i <- result.indices) {
      result(i) = ((hash >> (i * 2)) & 0xff).toByte
    }
    result
  }

  def of[T <: AnyRef: Digestable](ts: IArray[T]): Digest = {
    // Convert to regular arrays for easier manipulation
    val allByteArrays = ts.toList.map(t => Digestable[T].bytesFrom(t))
    val totalLength = allByteArrays.map(_.length).sum
    val allBytes = Array.ofDim[Byte](totalLength)

    var offset = 0
    allByteArrays.foreach { bytes =>
      System.arraycopy(bytes, 0, allBytes, offset, bytes.length)
      offset += bytes.length
    }

    val hashBytes = simpleHash(allBytes)
    new Digest(hashBytes)
  }
}
