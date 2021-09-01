package io.isomarcte.sbt.version.scheme.enforcer.core.internal

trait Order1[F[_]] extends Serializable {
  def liftCompare[A, B](compare: A => B => Int, x: F[A], y: F[B]): Int

  // final //

  final def compare1[A](x: F[A], y: F[A])(implicit A: Ordering[A]): Int =
    liftCompare[A, A](x => y => A.compare(x, y), x, y)
}

object Order1 {

  def apply[F[_]](implicit F: Order1[F]): Order1[F] = F

  implicit def orderingFromOrder1[F[_], A](implicit F: Order1[F], A: Ordering[A]): Ordering[F[A]] =
    new Ordering[F[A]] {
      override def compare(x: F[A], y: F[A]): Int =
        F.compare1[A](x, y)
    }
}
