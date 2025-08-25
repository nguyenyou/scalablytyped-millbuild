package org.scalablytyped.converter.internal

import scala.collection.IterableOps

object seqs {
  @inline final implicit class TraversableOps[C[t] <: IterableOps[t, C, C[t]], T](private val ts: C[T])
      extends AnyVal {

    def firstDefined[U](f: T => Option[U]): Option[U] = {
      val it = ts.iterator

      while (it.hasNext) {
        val res = f(it.next())
        if (res.isDefined) return res
      }
      None
    }

    def partitionCollect[T1](t1: PartialFunction[T, T1]): (List[T1], List[T]) = {
      val t1s  = List.newBuilder[T1]
      val rest = List.newBuilder[T]

      ts.foreach {
        case t if t1.isDefinedAt(t) => t1s += t1(t)
        case t                      => rest += t
      }

      (t1s.result(), rest.result())
    }

    def partitionCollect2[T1, T2](t1: PartialFunction[T, T1], t2: PartialFunction[T, T2]): (List[T1], List[T2], List[T]) = {
      val t1s  = List.newBuilder[T1]
      val t2s  = List.newBuilder[T2]
      val rest = List.newBuilder[T]

      ts.foreach {
        case t if t1.isDefinedAt(t) => t1s += t1(t)
        case t if t2.isDefinedAt(t) => t2s += t2(t)
        case t                      => rest += t
      }

      (t1s.result(), t2s.result(), rest.result())
    }

    def partitionCollect3[T1, T2, T3](
        t1: PartialFunction[T, T1],
        t2: PartialFunction[T, T2],
        t3: PartialFunction[T, T3],
    ): (List[T1], List[T2], List[T3], List[T]) = {

      val t1s  = List.newBuilder[T1]
      val t2s  = List.newBuilder[T2]
      val t3s  = List.newBuilder[T3]
      val rest = List.newBuilder[T]

      ts.foreach {
        case t if t1.isDefinedAt(t) => t1s += t1(t)
        case t if t2.isDefinedAt(t) => t2s += t2(t)
        case t if t3.isDefinedAt(t) => t3s += t3(t)
        case t                      => rest += t
      }

      (t1s.result(), t2s.result(), t3s.result(), rest.result())
    }

    def partitionCollect4[T1, T2, T3, T4](
        t1: PartialFunction[T, T1],
        t2: PartialFunction[T, T2],
        t3: PartialFunction[T, T3],
        t4: PartialFunction[T, T4],
    ): (List[T1], List[T2], List[T3], List[T4], List[T]) = {

      val t1s  = List.newBuilder[T1]
      val t2s  = List.newBuilder[T2]
      val t3s  = List.newBuilder[T3]
      val t4s  = List.newBuilder[T4]
      val rest = List.newBuilder[T]

      ts.foreach {
        case t if t1.isDefinedAt(t) => t1s += t1(t)
        case t if t2.isDefinedAt(t) => t2s += t2(t)
        case t if t3.isDefinedAt(t) => t3s += t3(t)
        case t if t4.isDefinedAt(t) => t4s += t4(t)
        case t                      => rest += t
      }

      (t1s.result(), t2s.result(), t3s.result(), t4s.result(), rest.result())
    }

    def partitionCollect5[T1, T2, T3, T4, T5](
        t1: PartialFunction[T, T1],
        t2: PartialFunction[T, T2],
        t3: PartialFunction[T, T3],
        t4: PartialFunction[T, T4],
        t5: PartialFunction[T, T5],
    ): (List[T1], List[T2], List[T3], List[T4], List[T5], List[T]) = {

      val t1s  = List.newBuilder[T1]
      val t2s  = List.newBuilder[T2]
      val t3s  = List.newBuilder[T3]
      val t4s  = List.newBuilder[T4]
      val t5s  = List.newBuilder[T5]
      val rest = List.newBuilder[T]

      ts.foreach {
        case t if t1.isDefinedAt(t) => t1s += t1(t)
        case t if t2.isDefinedAt(t) => t2s += t2(t)
        case t if t3.isDefinedAt(t) => t3s += t3(t)
        case t if t4.isDefinedAt(t) => t4s += t4(t)
        case t if t5.isDefinedAt(t) => t5s += t5(t)
        case t                      => rest += t
      }

      (t1s.result(), t2s.result(), t3s.result(), t4s.result(), t5s.result(), rest.result())
    }
  }
}
