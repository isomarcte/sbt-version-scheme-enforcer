package io.isomarcte.sbt.version.scheme.enforcer.core

trait Order1[F[_]] extends Serializable {
  def liftCompare[A, B](compare: A => B => Int, x: F[A], y: F[B]): Int

  // final //

  final def compare1[A](x: F[A], y: F[A])(implicit A: Ordering[A]): Int =
    liftCompare[A, A](x => y => A.compare(x, y), x, y)
}

object Order1 extends Order1LowPriority0 {

  def apply[F[_]](implicit F: Order1[F]): Order1[F] = F

  def orderingFromOrder1[F[_], A](implicit F: Order1[F], A: Ordering[A]): Ordering[F[A]] =
    new Ordering[F[A]] {
      override def compare(x: F[A], y: F[A]): Int =
        F.compare1[A](x, y)
    }

  implicit val idInstance: Order1[Id] =
    new Order1[Id] {
      override def liftCompare[A, B](compare: A => (B => Int), x: Id[A], y: Id[B]): Int =
        compare(x)(y)
    }
}

trait Order1LowPriority0 {
  implicit def nested[F[_]: Order1, G[_]: Order1]: Order1[Lambda[A => F[G[A]]]] =
    new Order1[Lambda[A => F[G[A]]]] {
      override def liftCompare[A, B](compare: A => B => Int, x: F[G[A]], y: F[G[B]]): Int =
        Order1[F].liftCompare((a: G[A]) => (b: G[B]) => Order1[G].liftCompare(compare, a, b), x, y)
    }
}
