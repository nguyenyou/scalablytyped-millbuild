package org.scalablytyped.converter.internal

import scala.collection.mutable
import scala.collection.immutable.Range

object IArray {
  def apply[A <: AnyRef](as: A*): IArray[A] =
    if (as.isEmpty) Empty else new IArray[A](as.toArray[AnyRef], as.length)

  def fromOption[A <: AnyRef](oa: Option[A]): IArray[A] =
    oa match {
      case Some(a) => apply(a)
      case None    => Empty
    }

  def fromOptions[A <: AnyRef](as: Option[A]*): IArray[A] =
    apply(as.flatten*)

  def fromArray[A <: AnyRef](as: Array[A]): IArray[A] =
    fromArrayAndSize(as.asInstanceOf[Array[AnyRef]], as.length)

  def fromTraversable[A <: AnyRef](as: Iterable[A]): IArray[A] =
    fromArrayAndSize(as.asInstanceOf[Iterable[AnyRef]].toArray, as.size)

  @inline private def fromArrayAndSize[A <: AnyRef](
      as: Array[AnyRef],
      length: Int
  ): IArray[A] =
    if (length == 0) Empty else new IArray[A](as, length)

  val Empty = new IArray(Array.ofDim(0), 0)

  object first {
    def unapply[A <: AnyRef](as: IArray[A]): Option[A] = as.headOption
  }
  object last {
    def unapply[A <: AnyRef](as: IArray[A]): Option[A] = as.lastOption
  }
  object exactlyOne {
    def unapply[A <: AnyRef](as: IArray[A]): Option[A] =
      if (as.length == 1) Some(as(0)) else None
  }
  object exactlyTwo {
    def unapply[A <: AnyRef](as: IArray[A]): Option[(A, A)] =
      if (as.length == 2) Some((as(0), as(1))) else None
  }
  object exactlyThree {
    def unapply[A <: AnyRef](as: IArray[A]): Option[(A, A, A)] =
      if (as.length == 3) Some((as(0), as(1), as(2))) else None
  }
  object exactlyFour {
    def unapply[A <: AnyRef](as: IArray[A]): Option[(A, A, A, A)] =
      if (as.length == 4) Some((as(0), as(1), as(2), as(3))) else None
  }
  object headTail {
    def unapply[A <: AnyRef](as: IArray[A]): Option[(A, IArray[A])] =
      if (as.length == 0) None else Some((as.head, as.tail))
  }
  object initLast {
    def unapply[A <: AnyRef](as: IArray[A]): Option[(IArray[A], A)] =
      if (as.length == 0) None else Some((as.init, as.last))
  }

  object Builder {
    def empty[A <: AnyRef]: Builder[A] = new Builder[A](32)
    def empty[A <: AnyRef](initialCapacity: Int): Builder[A] =
      new Builder[A](initialCapacity)
  }

  final class Builder[A <: AnyRef](initialCapacity: Int)
      extends mutable.Builder[A, IArray[A]] {
    private val buf = mutable.ArrayBuffer.empty[A]

    override def addOne(elem: A): this.type = {
      buf += elem
      this
    }
    override def clear(): Unit = buf.clear()

    override def result(): IArray[A] =
      new IArray[A](buf.toArray[AnyRef], buf.size)

    def ++=(as: IArray[A]): this.type = {
      var idx = 0
      while (idx < as.length) {
        this.addOne(as(idx))
        idx += 1
      }
      this
    }

    def isEmpty: Boolean = buf.isEmpty
  }

  @inline implicit final class IArrayOps[A <: AnyRef](val as: IArray[A])
      extends AnyRef {
    def contains(a: A): Boolean = {
      var idx = 0
      while (idx < as.length) {
        if (as.array(idx) == a) {
          return true
        }
        idx += 1
      }
      false
    }

    def partitionCollect3[A1 <: AnyRef, A2 <: AnyRef, A3 <: AnyRef](
        t1: PartialFunction[A, A1],
        t2: PartialFunction[A, A2],
        t3: PartialFunction[A, A3]
    ): (IArray[A1], IArray[A2], IArray[A3], IArray[A]) = {
      val a1s = Array.ofDim[AnyRef](as.length)
      var a1num = 0
      val a2s = Array.ofDim[AnyRef](as.length)
      var a2num = 0
      val a3s = Array.ofDim[AnyRef](as.length)
      var a3num = 0
      val rest = Array.ofDim[AnyRef](as.length)
      var restnum = 0

      var idx = 0
      while (idx < as.length) {
        as(idx) match {
          case t if t1.isDefinedAt(t) =>
            a1s(a1num) = t1(t)
            a1num += 1
          case t if t2.isDefinedAt(t) =>
            a2s(a2num) = t2(t)
            a2num += 1
          case t if t3.isDefinedAt(t) =>
            a3s(a3num) = t3(t)
            a3num += 1
          case t =>
            rest(restnum) = t
            restnum += 1
        }
        idx += 1
      }

      (
        fromArrayAndSize[A1](a1s, a1num),
        fromArrayAndSize[A2](a2s, a2num),
        fromArrayAndSize[A3](a3s, a3num),
        fromArrayAndSize[A](rest, restnum)
      )
    }

    def nonEmptyOpt: Option[IArray[A]] =
      if (as.isEmpty) None else Some(as)
  }
}

final class IArray[+A <: AnyRef](
    private val array: Array[AnyRef],
    val length: Int
) extends Serializable { self =>
  @inline def isEmpty: Boolean = length == 0
  @inline def nonEmpty: Boolean = length > 0
  @inline def apply(n: Int): A = array(n).asInstanceOf[A]
  @inline def isDefinedAt(n: Int): Boolean = n < length

  @inline def map[B <: AnyRef](f: A => B): IArray[B] = {
    if (isEmpty) return IArray.Empty
    val newArray = Array.ofDim[AnyRef](length)
    var i = 0
    while (i < length) {
      newArray(i) = f(apply(i))
      i += 1
    }
    IArray.fromArrayAndSize[B](newArray, length)
  }

  @inline def foreach(f: A => Unit): Unit = {
    var i = 0
    while (i < length) {
      f(apply(i))
      i += 1
    }
  }

  @inline def foldLeft[Z](z: Z)(f: (Z, A) => Z): Z = {
    var current = z
    var idx = 0
    while (idx < length) {
      current = f(current, apply(idx))
      idx += 1
    }
    current
  }

  def headOption: Option[A] =
    if (isEmpty) None else Some(apply(0))

  def head: A =
    headOption.getOrElse(sys.error("head of empty list"))

  def tail: IArray[A] =
    if (isEmpty) sys.error("tail of empty list") else drop(1)

  def init: IArray[A] =
    if (isEmpty) sys.error("init of empty list") else dropRight(1)

  def lastOption: Option[A] =
    if (isEmpty) None else Some(apply(length - 1))

  def last: A =
    lastOption.getOrElse(sys.error("last of empty list"))

  @inline def forall(p: A => Boolean): Boolean = {
    var idx = 0
    while (idx < length) {
      if (!p(apply(idx))) return false
      idx += 1
    }
    true
  }

  @inline def exists(p: A => Boolean): Boolean = {
    var idx = 0
    while (idx < length) {
      if (p(apply(idx))) return true
      idx += 1
    }
    false
  }

  def ++[B >: A <: AnyRef](that: IArray[B]): IArray[B] =
    if (isEmpty) that
    else if (that.isEmpty) this
    else {
      val newLength = length + that.length
      val ret = Array.ofDim[AnyRef](newLength)
      System.arraycopy(array, 0, ret, 0, length)
      System.arraycopy(that.array, 0, ret, length, that.length)
      IArray.fromArrayAndSize[A](ret, newLength)
    }

  def :+[B >: A <: AnyRef](elem: B): IArray[B] = {
    val newLength = length + 1
    val ret = Array.ofDim[AnyRef](newLength)
    System.arraycopy(array, 0, ret, 0, length)
    ret(length) = elem
    IArray.fromArrayAndSize[B](ret, newLength)
  }

  def drop(n: Int): IArray[A] = {
    val newLength = math.max(0, length - n)
    if (newLength == 0) return IArray.Empty
    val ret = Array.ofDim[AnyRef](newLength)
    System.arraycopy(array, n, ret, 0, newLength)
    IArray.fromArrayAndSize[A](ret, newLength)
  }

  def dropRight(n: Int): IArray[A] =
    IArray.fromArrayAndSize[A](array, math.max(0, length - n))

  def toList: List[A] = {
    val builder = List.newBuilder[A]
    var i = 0
    while (i < length) {
      builder += apply(i)
      i += 1
    }
    builder.result()
  }

  def mkString(init: String, sep: String, post: String): String = {
    val sb = new StringBuilder()
    sb.append(init)
    var i = 0
    while (i < length) {
      if (i != 0) sb.append(sep)
      sb.append(apply(i))
      i += 1
    }
    sb.append(post)
    sb.toString
  }

  def partition(p: A => Boolean): (IArray[A], IArray[A]) = {
    val trues = IArray.Builder.empty[A]
    val falses = IArray.Builder.empty[A]
    var i = 0
    while (i < length) {
      val elem = apply(i)
      if (p(elem)) trues += elem
      else falses += elem
      i += 1
    }
    (trues.result(), falses.result())
  }

  def startsWith[B >: A <: AnyRef](that: IArray[B]): Boolean = {
    if (that.length > length) false
    else {
      var i = 0
      while (i < that.length) {
        if (apply(i) != that(i)) return false
        i += 1
      }
      true
    }
  }

  def mkString: String = mkString("", "", "")
  def mkString(sep: String): String = mkString("", sep, "")

  override def toString: String = mkString("IArray(", ", ", ")")

  override lazy val hashCode: Int = {
    var idx = 0
    val prime = 31
    var result = 1
    while (idx < length) {
      result = prime * result + apply(idx).##
      idx += 1
    }
    result
  }

  override def equals(obj: Any): Boolean =
    obj match {
      case other: IArray[_]
          if other.length == length && hashCode == other.hashCode =>
        var idx = 0
        while (idx < length) {
          if (apply(idx) != other(idx)) return false
          idx += 1
        }
        true
      case _ => false
    }
}
