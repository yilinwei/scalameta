package scala.meta
package syntactic

import java.nio.charset.Charset
import org.scalameta.adt._
import org.scalameta.convert._
import org.scalameta.invariants._
import scala.collection.mutable
import scala.meta.internal.tokenizers._

trait Input {
  def content: Array[Char]
  private val tokenCache = mutable.Map[Dialect, Tokens]()
  def tokens(implicit dialect: Dialect): Tokens = tokenCache.getOrElseUpdate(dialect, tokenize(this))
}

object Input {
  final case object None extends Input {
    lazy val content = new Array[Char](0)
  }
  final case class Slice(input: Input, start: Int, end: Int) extends Input {
    require(0 <= start && start <= input.content.length)
    require(-1 <= end && end < input.content.length)
    lazy val content = input.content.slice(start, end + 1)
  }
  final case class String(s: Predef.String) extends Input {
    lazy val content = s.toArray
  }
  final case class File(f: java.io.File, charset: Charset) extends Input {
    lazy val content = scala.io.Source.fromFile(f)(scala.io.Codec(charset)).mkString.toArray
  }
  object File {
    def apply(path: Predef.String): Input.File = Input.File(new java.io.File(path))
    def apply(f: java.io.File): Input.File = Input.File(f, Charset.forName("UTF-8"))
  }
  // NOTE: This Input is really special in the sense that
  // doing `input.tokens.head.input` won't return `input`.
  // Previously, I tried to do Token.adjust on every token in the payload,
  // so that they point back to the newly created Input.Tokens.
  // Unfortunately, this runs into performance problems - we really can't afford
  // to clone an entire token stream every time when a tree undergoes a slight change.
  // Therefore, I'm letting this inconsistency alone, and we'll see how it pans out.
  final case class Tokens(payload: scala.meta.syntactic.Tokens) extends Input {
    lazy val content = payload.map(_.code).mkString.toArray
    override def tokens(implicit dialect: Dialect) = payload
    override def toString = payload.toString
  }
  final case class Chars(content: Array[Char]) extends Input
  implicit val stringToInput: Convert[scala.Predef.String, Input] = Convert.apply(Input.String(_))
  implicit val fileToInput: Convert[java.io.File, Input] = Convert.apply(f => Input.File(f, Charset.forName("UTF-8")))
  implicit val tokensToInput: Convert[scala.meta.syntactic.Tokens, Input] = Convert.apply(Input.Tokens(_))
  implicit val charsToInput: Convert[Array[Char], Input] = Convert.apply(Input.Chars(_))
}
