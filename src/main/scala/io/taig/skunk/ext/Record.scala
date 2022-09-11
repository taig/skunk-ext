package io.taig.skunk.ext

import cats.syntax.all._
import cats.{Applicative, Eval, Order, Traverse}

import java.time.Instant

final case class Record[A](
    identifier: Record.Identifier,
    updated: Option[Record.Updated],
    created: Record.Created,
    value: A
) {
  def map[B](f: A => B): Record[B] = copy(value = f(value))
  def traverse[G[_]: Applicative, B](f: A => G[B]): G[Record[B]] = f(value).map(value => copy(value = value))
}

object Record {
  final case class Identifier(value: Long) extends AnyVal

  object Identifier {
    implicit val order: Order[Record.Identifier] = Order.by(_.value)
  }

  final case class Updated(value: Instant) extends AnyVal

  final case class Created(value: Instant) extends AnyVal

  final case class Immutable[A](identifier: Record.Identifier, created: Record.Created, value: A) {
    def map[B](f: A => B): Record.Immutable[B] = copy(value = f(value))
    def traverse[G[_]: Applicative, B](f: A => G[B]): G[Record.Immutable[B]] =
      f(value).map(value => copy(value = value))
  }

  object Immutable {
    implicit val traverse: Traverse[Record.Immutable] = new Traverse[Record.Immutable] {
      override def map[A, B](fa: Record.Immutable[A])(f: A => B): Record.Immutable[B] = fa.map(f)
      override def traverse[G[_]: Applicative, A, B](fa: Record.Immutable[A])(f: A => G[B]): G[Record.Immutable[B]] =
        fa.traverse(f)
      override def foldLeft[A, B](fa: Record.Immutable[A], b: B)(f: (B, A) => B): B = f(b, fa.value)
      override def foldRight[A, B](fa: Record.Immutable[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
        f(fa.value, lb)
    }

    def created[A](value: A): ((Identifier, Created)) => Record.Immutable[A] = { case (identifier, created) =>
      Immutable(identifier, created, value)
    }
  }

  def created[A](value: A): ((Identifier, Created)) => Record[A] = { case (identifier, created) =>
    Record(identifier, updated = None, created, value)
  }

  def updated[A](record: Record[A]): Updated => Record[A] = updated => record.copy(updated = updated.some)

  implicit val traverse: Traverse[Record] = new Traverse[Record] {
    override def map[A, B](fa: Record[A])(f: A => B): Record[B] = fa.map(f)
    override def traverse[G[_]: Applicative, A, B](fa: Record[A])(f: A => G[B]): G[Record[B]] = fa.traverse(f)
    override def foldLeft[A, B](fa: Record[A], b: B)(f: (B, A) => B): B = f(b, fa.value)
    override def foldRight[A, B](fa: Record[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = f(fa.value, lb)
  }
}