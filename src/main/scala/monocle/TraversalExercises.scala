/*
 * scala-exercises - exercises-monocle
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

package monocle

import org.scalatest._
import org.scalaexercises.definitions._

object TraversalHelper {

  val xs = List(1, 2, 3, 4, 5)

  import scalaz.std.list._ // to get the Traverse instance for List
  val eachL = Traversal.fromTraverse[List, Int]

  case class Point(id: String, x: Int, y: Int)

  val points = Traversal.apply2[Point, Int](_.x, _.y)((x, y, p) => p.copy(x = x, y = y))

  import scalaz.Applicative
  import scalaz.std.map._
  import scalaz.syntax.traverse._
  import scalaz.syntax.applicative._

  def filterKey[K, V](predicate: K => Boolean): Traversal[Map[K, V], V] =
    new Traversal[Map[K, V], V] {
      def modifyF[F[_]: Applicative](f: V => F[V])(s: Map[K, V]): F[Map[K, V]] =
        s.map {
          case (k, v) =>
            k -> (if (predicate(k)) f(v) else v.pure[F])
        }.sequenceU
    }

  val m = Map(1 -> "one", 2 -> "two", 3 -> "three", 4 -> "Four")

}

/** == Traversal ==
 *
 * A [[http://julien-truffaut.github.io/Monocle/optics/traversal.html `Traversal`]]  is the generalisation of an `Optional` to several targets. In other word, a `Traversal` allows to focus from a type `S` into `0` to n values of type `A`.
 *
 * The most common example of a `Traversal` would be to focus into all elements inside of a container (e.g. `List`, `Vector`, `Option`). To do this we will use the relation between the typeclass `scalaz.Traverse` and `Traversal`:
 *
 * {{{
 *   import monocle.Traversal
 *   import scalaz.std.list._   // to get the Traverse instance for List
 *
 *   val xs = List(1,2,3,4,5)
 * }}}
 *
 * @param name traversal
 */
object TraversalExercises extends FlatSpec with Matchers with Section {

  import TraversalHelper._

  /**
   * {{{
   *   val eachL = Traversal.fromTraverse[List, Int]
   *     // eachL: monocle.Traversal[List[Int],Int] = monocle.PTraversal$$anon$5@1c06784
   * }}}
   */
  def exerciseTraversal(res0: List[Int], res1: List[Int]) = {

    eachL.set(0)(xs) should be(res0)

    eachL.modify(_ + 1)(xs) should be(res1)
  }

  /**
   * A `Traversal` is also a `Fold`, so we have access to a few interesting methods to query our data:
   */
  def exerciseFold(res0: List[Int], res1: Option[Int], res2: Option[Int], res3: Boolean) = {

    eachL.getAll(xs) should be(res0)

    eachL.headOption(xs) should be(res1)

    eachL.find(_ > 3)(xs) should be(res2)

    eachL.all(_ % 2 == 0)(xs) should be(res3)
  }

  /**
   * Traversal also offers smart constructors to build a `Traversal` for a fixed number of target (currently 2 to 6 targets):
   *
   * {{{
   *   case class Point(id: String, x: Int, y: Int)
   *
   *   val points = Traversal.apply2[Point, Int](_.x, _.y)((x, y, p) => p.copy(x = x, y = y))
   * }}}
   */
  def exerciseSmartConstruct(res0: Point) =
    points.set(5)(Point("bottom-left", 0, 0)) should be(res0)

  /**
   * Finally, if you want to build something more custom you will have to implement a `Traversal` manually. A `Traversal` is defined by a single method `modifyF` which corresponds to the Van Laarhoven representation.
   *
   * For example, let’s write a `Traversal` for `Map` that will focus into all values where the key satisfies a certain predicate:
   * {{{
   *   import monocle.Traversal
   *   import scalaz.Applicative
   *   import scalaz.std.map._
   *   import scalaz.syntax.traverse._
   *   import scalaz.syntax.applicative._
   *
   *   def filterKey[K, V](predicate: K => Boolean): Traversal[Map[K, V], V] =
   *     new Traversal[Map[K, V], V]{
   *       def modifyF[F[_]: Applicative](f: V => F[V])(s: Map[K, V]): F[Map[K, V]] =
   *       s.map{ case (k, v) =>
   *       k -> (if(predicate(k)) f(v) else v.pure[F])
   *    }.sequenceU
   *   }
   *
   *   val m = Map(1 -> "one", 2 -> "two", 3 -> "three", 4 -> "Four")
   * }}}
   */
  def exerciseModifyF(res0: Map[Int, String]) = {

    val filterEven = filterKey[Int, String](_ % 2 == 0)
    // filterEven: monocle.Traversal[Map[Int,String],String] = $anon$1@5fadf001

    filterEven.modify(_.toUpperCase)(m) should be(res0)

  }

  /** == Law ==
   *
   * A `Traversal` must satisfy all properties defined in `TraversalLaws` from the `core` module. You can check the validity of your own `Traversal` using `TraversalTests` from the `law` module.
   *
   * In particular, a `Traversal` must respect the `modifyGetAll` law which checks that you can modify all elements targeted by a `Traversal`
   *
   * Another important `law` is `composeModify` also known as fusion law:
   *
   */
  def exerciseLaws(res0: Boolean, res1: Boolean) = {

    def modifyGetAll[S, A](t: Traversal[S, A], s: S, f: A => A): Boolean =
      t.getAll(t.modify(f)(s)) == t.getAll(s).map(f)

    def composeModify[S, A](t: Traversal[S, A], s: S, f: A => A, g: A => A): Boolean =
      t.modify(g)(t.modify(f)(s)) == t.modify(g compose f)(s)

    modifyGetAll(eachL, List(1, 2, 3), (x: Int) => x + 1) should be(res0)

    composeModify(eachL, List(1, 2, 3), (x: Int) => x + 1, (y: Int) => y + 2) should be(res1)

  }

}
