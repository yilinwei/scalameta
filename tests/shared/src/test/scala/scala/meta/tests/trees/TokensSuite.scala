package scala.meta.tests
package trees

import munit._
import scala.meta._
import scala.meta.dialects.Scala211

class TokensSuite extends TreeSuiteBase {
  test("Tree.tokens: parsed, same dialect") {
    val tree = dialects.Scala211("foo + bar // baz").parse[Term].get
    assert(tree.syntax == "foo + bar // baz")
    assert(tree.tokens.syntax == "foo + bar // baz")
  }

  test("Tree.tokens: parsed, different dialect") {
    val tree = dialects.Scala210("foo + bar // baz").parse[Term].get
    assert(tree.syntax == "foo + bar")
    assert(tree.tokens.syntax == "foo + bar // baz")
  }

  test("Tree.tokens: manual") {
    val tree = Term.ApplyInfix(Term.Name("foo"), Term.Name("+"), Nil, List(Term.Name("bar")))
    interceptMessage[trees.Error.MissingDialectException](
      "Tree missing a dialect; update root tree `.withDialectIfRootAndNotSet` first, or call `.printSyntaxFor`."
    )(tree.text)
    assertEquals(tree.printSyntaxFor(implicitly[Dialect]), "foo + bar")
    assertEquals(tree.syntax, "foo + bar")
    assert(tree.origin eq trees.Origin.None)
    val tokens = tree.tokenizeFor(implicitly[Dialect])
    interceptMessage[trees.Error.MissingDialectException](
      "Tree missing a dialect; update root tree `.withDialectIfRootAndNotSet` first, or call `.tokenizeFor`."
    )(tree.tokens)
    assertEquals(tokens.syntax, "foo + bar")
    assert(tokens.forall(_.input.isInstanceOf[Input.VirtualFile]))
    val dialectTree = tree.withDialectIfRootAndNotSet
    assertNotEquals(dialectTree, tree)
    assertEquals(dialectTree.text, "foo + bar")
    assertEquals(dialectTree.printSyntaxFor(implicitly[Dialect]), "foo + bar")
    assert(dialectTree.origin ne trees.Origin.None)
    val dialectTokens = dialectTree.tokens
    assertEquals(dialectTokens.syntax, "foo + bar")
    assert(dialectTokens.forall(_.input.isInstanceOf[Input.VirtualFile]))

    val parsedTree = tree.maybeParseAs[Stat].get
    assert(parsedTree eq parsedTree.maybeParse.get)
    assertEquals(parsedTree.printSyntaxFor(implicitly[Dialect]), "foo + bar")
    assertEquals(parsedTree.syntax, "foo + bar")
    assert(parsedTree.origin.isInstanceOf[trees.Origin.Parsed])
    val parsedTokens = parsedTree.tokens
    assertEquals(parsedTokens.syntax, "foo + bar")
    assert(parsedTokens.forall(_.input.isInstanceOf[Input.VirtualFile]))
  }

  test("Tree.tokens: empty") {
    val emptyTemplate = "class C".parse[Stat].get.children(3)
    assertTree(emptyTemplate)(Template(Nil, Nil, Self(Name(""), None), Nil, Nil))
    assert(emptyTemplate.tokens.structure == "Tokens()")
  }

  test("inline can be used as an identifier") {
    val tree = dialects.Scala211("{ val inline = 42 }").parse[Term].get
    assert(tree.syntax == "{ val inline = 42 }")
  }
}
