package scala.meta.tests.parsers.dotty

import scala.meta._
import Term.{Super, Name => TermName}
import Type.{Name => TypeName, _}
import Name.Anonymous
import scala.meta.parsers.ParseException
import scala.meta.tests.parsers.ParseSuite
import munit.Location

class TypeSuite extends BaseDottySuite {

  private def assertTpe(expr: String)(tree: Tree)(implicit dialect: Dialect): Unit = {
    assertTree(tpe(expr))(tree)
  }

  test("with-type") {
    runTestAssert[Stat](
      """|type A = AnyRef with
         |  type T>: Null
         |""".stripMargin,
      assertLayout = Some("type A = AnyRef { type T >: Null }")
    )(
      Defn.Type(
        Nil,
        Type.Name("A"),
        Nil,
        Type.Refine(
          Some(Type.Name("AnyRef")),
          List(Decl.Type(Nil, Type.Name("T"), Nil, Type.Bounds(Some(Type.Name("Null")), None)))
        ),
        Type.Bounds(None, None)
      )
    )
  }

  test("with-type2") {
    runTestAssert[Stat](
      """|type A = AnyRef with Product with
         |  type T>: Null
         |""".stripMargin,
      assertLayout = Some("type A = AnyRef with Product { type T >: Null }")
    )(
      Defn.Type(
        Nil,
        Type.Name("A"),
        Nil,
        Type.Refine(
          Some(Type.With(Type.Name("AnyRef"), Type.Name("Product"))),
          List(Decl.Type(Nil, Type.Name("T"), Nil, Type.Bounds(Some(Type.Name("Null")), None)))
        ),
        Type.Bounds(None, None)
      )
    )
  }

  test("with-type3") {
    runTestAssert[Stat](
      """|type A = Product with
         |  type T>: Null
         |  with
         |    type D <: Product
         |""".stripMargin,
      assertLayout = Some("type A = Product { type T >: Null { type D <: Product } }")
    )(
      Defn.Type(
        Nil,
        Type.Name("A"),
        Nil,
        Type.Refine(
          Some(Type.Name("Product")),
          List(
            Decl.Type(
              Nil,
              Type.Name("T"),
              Nil,
              Type.Bounds(
                Some(
                  Type.Refine(
                    Some(Type.Name("Null")),
                    List(
                      Decl.Type(
                        Nil,
                        Type.Name("D"),
                        Nil,
                        Type.Bounds(None, Some(Type.Name("Product")))
                      )
                    )
                  )
                ),
                None
              )
            )
          )
        ),
        Type.Bounds(None, None)
      )
    )
  }

  test("coloneol-type3") {
    runTestAssert[Stat](
      """|type A = Product:
         |  type T>: Null:
         |    type D <: Product
         |""".stripMargin,
      assertLayout = Some(
        "type A = Product { type T >: Null { type D <: Product } }"
      )
    )(
      Defn.Type(
        Nil,
        Type.Name("A"),
        Nil,
        Type.Refine(
          Some(Type.Name("Product")),
          List(
            Decl.Type(
              Nil,
              Type.Name("T"),
              Nil,
              Type.Bounds(
                Some(
                  Type.Refine(
                    Some(Type.Name("Null")),
                    List(
                      Decl.Type(
                        Nil,
                        Type.Name("D"),
                        Nil,
                        Type.Bounds(None, Some(Type.Name("Product")))
                      )
                    )
                  )
                ),
                None
              )
            )
          )
        ),
        Type.Bounds(None, None)
      )
    )
  }

  test("with-type-error") {
    runTestError[Stat](
      """|type A = Product with
         |  type T>: Null
         | with
         |    type D <: Product
         |""".stripMargin,
      "error: ; expected but with found"
    )
  }

  test("with-indent-error") {

    // latter type should be ignored despite indentation
    runTestAssert[Stat](
      """|type A = Product
         |  type T>: Null
         |""".stripMargin,
      assertLayout = Some("type A = Product")
    )(
      Defn.Type(Nil, Type.Name("A"), Nil, Type.Name("Product"), Type.Bounds(None, None))
    )
  }

  test("with-followed-by-brace-indent") {
    runTestAssert[Stat](
      """|type AA = String with Int with
         |    type T>: Null
         |      {
         |        type T>: Int
         |      }
         |""".stripMargin,
      assertLayout = Some("type AA = String with Int { type T >: Null { type T >: Int } }")
    )(
      Defn.Type(
        Nil,
        pname("AA"),
        Nil,
        Type.Refine(
          Some(Type.With(pname("String"), pname("Int"))),
          Decl.Type(
            Nil,
            pname("T"),
            Nil,
            lowBound(
              Type.Refine(
                Some(pname("Null")),
                Decl.Type(Nil, pname("T"), Nil, lowBound(pname("Int"))) :: Nil
              )
            )
          ) :: Nil
        ),
        noBounds
      )
    )
  }

  test("coloneol-followed-by-brace-indent") {
    runTestAssert[Stat](
      """|type AA = String with Int:
         |    type T>: Null:
         |        type T>: Int
         |""".stripMargin,
      assertLayout = Some(
        "type AA = String with Int { type T >: Null { type T >: Int } }"
      )
    )(
      Defn.Type(
        Nil,
        Type.Name("AA"),
        Nil,
        Type.Refine(
          Some(Type.With(Type.Name("String"), Type.Name("Int"))),
          List(
            Decl.Type(
              Nil,
              Type.Name("T"),
              Nil,
              Type.Bounds(
                Some(
                  Type.Refine(
                    Some(Type.Name("Null")),
                    List(
                      Decl.Type(Nil, Type.Name("T"), Nil, Type.Bounds(Some(Type.Name("Int")), None))
                    )
                  )
                ),
                None
              )
            )
          )
        ),
        Type.Bounds(None, None)
      )
    )
  }

  test("with-followed-by-brace") {
    runTestAssert[Stat](
      """|{
         |  type AA = String with Int with
         |    type T>: Null
         |  {
         |    type T>: Int
         |  }
         |}
         |""".stripMargin,
      Some(
        """|{
           |  type AA = String with Int { type T >: Null }
           |  {
           |    type T >: Int
           |  }
           |}
           |""".stripMargin
      )
    )(
      Term.Block(
        List(
          Defn.Type(
            Nil,
            pname("AA"),
            Nil,
            Type.Refine(
              Some(Type.With(pname("String"), pname("Int"))),
              Decl.Type(Nil, pname("T"), Nil, Type.Bounds(Some(pname("Null")), None)) :: Nil
            ),
            Type.Bounds(None, None)
          ),
          Term.Block(
            Decl.Type(Nil, pname("T"), Nil, Type.Bounds(Some(pname("Int")), None)) :: Nil
          )
        )
      )
    )
  }

  test("T") {
    assertTpe("T")(TypeName("T"))
  }

  test("F[T]") {
    assertTpe("F[T]") {
      Apply(TypeName("F"), ArgClause(TypeName("T") :: Nil))
    }
  }

  test("F#T") {
    assertTpe("F#T")(Project(TypeName("F"), TypeName("T")))
  }

  test("A \\/ B") {
    assertTpe("A \\/ B")(ApplyInfix(TypeName("A"), TypeName("\\/"), TypeName("B")))
  }

  test("A * B") {
    assertTpe("A * B")(ApplyInfix(TypeName("A"), TypeName("*"), TypeName("B")))
  }

  test("A * B + C") {
    assertTpe("A * B + C") {
      Type.ApplyInfix(
        Type.ApplyInfix(Type.Name("A"), Type.Name("*"), Type.Name("B")),
        Type.Name("+"),
        Type.Name("C")
      )
    }
  }

  test("A + B * C") {
    assertTpe("A + B * C") {
      Type.ApplyInfix(
        Type.Name("A"),
        Type.Name("+"),
        Type.ApplyInfix(Type.Name("B"), Type.Name("*"), Type.Name("C"))
      )
    }
  }

  test("A * B + C / D") {
    assertTpe("A * B + C / D") {
      Type.ApplyInfix(
        Type.ApplyInfix(Type.Name("A"), Type.Name("*"), Type.Name("B")),
        Type.Name("+"),
        Type.ApplyInfix(Type.Name("C"), Type.Name("/"), Type.Name("D"))
      )
    }
  }

  test("f.T") {
    assertTpe("f.T")(Select(TermName("f"), TypeName("T")))
  }

  test("f.type") {
    assertTpe("f.type")(Singleton(TermName("f")))
  }

  test("super.T") {
    assertTpe("super.T")(Select(Super(Anonymous(), Anonymous()), TypeName("T")))
  }

  test("this.T") {
    assertTpe("this.T")(Select(Term.This(Anonymous()), TypeName("T")))
  }

  test("(A, B)") {
    assertTpe("(A, B)")(Tuple(TypeName("A") :: TypeName("B") :: Nil))
  }

  test("(A, B) => C") {
    assertTpe("(A, B) => C")(Function(TypeName("A") :: TypeName("B") :: Nil, TypeName("C")))
  }

  test("T @foo") {
    assertTpe("T @foo")(
      Annotate(
        TypeName("T"),
        Mod.Annot(Init(Type.Name("foo"), Name.Anonymous(), emptyArgClause)) :: Nil
      )
    )
  }

  test("A with B") {
    assertTpe("A with B")(With(TypeName("A"), TypeName("B")))
  }

  test("A & B is not a special type") {
    assertTpe("A & B")(ApplyInfix(TypeName("A"), TypeName("&"), TypeName("B")))
  }

  test("A with B {}") {
    assertTpe("A with B {}")(Refine(Some(With(TypeName("A"), TypeName("B"))), Nil))
  }

  test("{}") {
    assertTpe("{}")(Refine(None, Nil))
  }

  test("A { def x: A; val y: B; type C }") {
    assertTpe("A { def x: Int; val y: B; type C }") {
      Refine(
        Some(TypeName("A")),
        Decl.Def(Nil, TermName("x"), Type.ParamClause(Nil), Nil, TypeName("Int")) ::
          Decl.Val(Nil, List(Pat.Var(TermName("y"))), TypeName("B")) ::
          Decl.Type(Nil, TypeName("C"), Type.ParamClause(Nil), Type.Bounds(None, None)) :: Nil
      )
    }
  }

  test("F[_ >: lo <: hi]") {
    implicit val dialect: Dialect = dialects.Scala31
    val expected =
      Apply(
        TypeName("F"),
        Wildcard(Bounds(Some(TypeName("lo")), Some(TypeName("hi")))) :: Nil
      )
    assertTpe("F[_ >: lo <: hi]") { expected }
    assertTpe("F[? >: lo <: hi]") { expected }
  }

  test("F[_ >: lo") {
    implicit val dialect: Dialect = dialects.Scala31
    val expected =
      Apply(TypeName("F"), Wildcard(Bounds(Some(TypeName("lo")), None)) :: Nil)
    assertTpe("F[_ >: lo]") { expected }
    assertTpe("F[? >: lo]") { expected }
  }

  test("F[_ <: hi]") {
    implicit val dialect: Dialect = dialects.Scala31
    val expected =
      Apply(TypeName("F"), Wildcard(Bounds(None, Some(TypeName("hi")))) :: Nil)
    assertTpe("F[_ <: hi]") { expected }
    assertTpe("F[? <: hi]") { expected }
  }

  test("F[?]") {
    implicit val dialect: Dialect = dialects.Scala31
    val expected =
      Apply(TypeName("F"), List(Wildcard(Bounds(None, None))))
    assertTpe("F[?]") { expected }
    assertTpe("F[_]") { expected }
  }

  test("F[_]") {
    implicit val dialect: Dialect = dialects.Scala3Future
    assertTpe("F[_]") {
      AnonymousLambda(Apply(TypeName("F"), List(AnonymousParam(None))))
    }
    assertTpe("F[+_]") {
      AnonymousLambda(Apply(TypeName("F"), List(AnonymousParam(Some(Mod.Covariant())))))
    }
    assertTpe("F[-_]") {
      AnonymousLambda(Apply(TypeName("F"), List(AnonymousParam(Some(Mod.Contravariant())))))
    }
  }

  test("F[*]") {
    // will be deprecated in later versions
    implicit val dialect: Dialect = dialects.Scala31
    assertTpe("F[*]") {
      AnonymousLambda(Apply(TypeName("F"), List(AnonymousParam(None))))
    }
    assertTpe("F[+*]") {
      AnonymousLambda(Apply(TypeName("F"), List(AnonymousParam(Some(Mod.Covariant())))))
    }
    assertTpe("F[-*]") {
      AnonymousLambda(Apply(TypeName("F"), List(AnonymousParam(Some(Mod.Contravariant())))))
    }
  }

  test("F[T] forSome { type T }") {
    assertTpe("F[T] forSome { type T }") {
      Existential(
        Apply(TypeName("F"), TypeName("T") :: Nil),
        Decl.Type(Nil, TypeName("T"), Type.ParamClause(Nil), Type.Bounds(None, None)) :: Nil
      )
    }
  }

  test("a.T forSome { val a: A }") {
    assertTpe("a.T forSome { val a: A }")(
      Existential(
        Select(TermName("a"), TypeName("T")),
        Decl.Val(Nil, Pat.Var(TermName("a")) :: Nil, TypeName("A")) :: Nil
      )
    )
  }

  test("A | B is not a special type") {
    assertTpe("A | B")(ApplyInfix(TypeName("A"), TypeName("|"), TypeName("B")))
  }

  test("42.type") {
    intercept[ParseException] {
      tpe("42")(dialects.Scala211)
    }

    assertTpe("42")(Lit.Int(42))(dialects.Scala3)
    assertTpe("-42")(Lit.Int(-42))(dialects.Scala3)
    assertTpe("42L")(Lit.Long(42L))(dialects.Scala3)
    matchSubStructure[Type]("42.0f", { case Lit(42.0f) => () })
    matchSubStructure[Type]("-42.0f", { case Lit(-42.0f) => () })
    matchSubStructure[Type]("42.0d", { case Lit(42.0d) => () })
    matchSubStructure[Type]("-42.0d", { case Lit(-42.0d) => () })
    assertTpe("\"42\"")(Lit.String("42"))(dialects.Scala3)
    assertTpe("true")(Lit.Boolean(true))(dialects.Scala3)
    assertTpe("false")(Lit.Boolean(false))(dialects.Scala3)

    val exceptionScala3 = intercept[ParseException] {
      tpe("() => ()")(dialects.Scala3)
    }
    assertNoDiff(exceptionScala3.shortMessage, "illegal literal type (), use Unit instead")

    val exceptionScala2 = intercept[ParseException] {
      tpe("() => ()")(dialects.Scala213)
    }
    assertNoDiff(exceptionScala2.shortMessage, "illegal literal type (), use Unit instead")

  }

  test("plus-minus-then-underscore-source3") {
    matchSubStructure(
      "+_ => Int",
      { case Type.Function(List(Type.Name("+_")), Type.Name("Int")) => () }
    )(parseType, dialects.Scala213Source3, implicitly[Location])
    assertTpe("Option[- _]") {
      Apply(Type.Name("Option"), ArgClause(List(Type.Name("-_"))))
    }(dialects.Scala213Source3)
  }

  test("[scala213] (x: Int, y)") {
    val err = intercept[ParseException] {
      tpe("(x: Int, y)")(dialects.Scala213)
    }
    assertNoDiff(err.shortMessage, "can't mix function type and dependent function type syntaxes")
  }

  test("[scala213] (x: Int, y: Int)(z: String)") {
    val err = intercept[ParseException] {
      tpe("(x: Int, y: Int)(z: String)")(dialects.Scala213)
    }
    assertNoDiff(err.shortMessage, "dependent function types are not supported")
  }

  test("[scala3] (x: Int, y: Int)(z: String)") {
    val err = intercept[ParseException] {
      tpe("(x: Int, y: Int)(z: String)")(dialects.Scala3)
    }
    assertNoDiff(err.shortMessage, "can't have multiple parameter lists in function types")
  }

  test("#3162 [scala30] higher-kinded is not wildcard 1") {
    implicit val dialect: Dialect = dialects.Scala30
    runTestAssert[Stat]("def foo[A <: C[_]] = bar.baz[_, F[_]]")(
      Defn.Def(
        Nil,
        tname("foo"),
        Type.Param(
          Nil,
          pname("A"),
          Nil,
          Type.Bounds(
            None,
            Some(Type.AnonymousLambda(Type.Apply(pname("C"), List(Type.AnonymousParam(None)))))
          ),
          Nil,
          Nil
        ) :: Nil,
        Nil,
        None,
        Term.ApplyType(
          Term.Select(tname("bar"), tname("baz")),
          List(
            Type.Wildcard(noBounds),
            Type.AnonymousLambda(Type.Apply(pname("F"), List(Type.AnonymousParam(None))))
          )
        )
      )
    )
  }

  test("#3162 [scala3+] higher-kinded is not wildcard 1") {
    implicit val dialect: Dialect = dialects.Scala3.withAllowUnderscoreAsTypePlaceholder(true)
    runTestAssert[Stat]("def foo[A <: C[_]] = bar.baz[_, F[_]]")(
      Defn.Def(
        Nil,
        tname("foo"),
        Type.Param(
          Nil,
          pname("A"),
          Nil,
          Type.Bounds(
            None,
            Some(Type.AnonymousLambda(Type.Apply(pname("C"), List(Type.AnonymousParam(None)))))
          ),
          Nil,
          Nil
        ) :: Nil,
        Nil,
        None,
        Term.ApplyType(
          Term.Select(tname("bar"), tname("baz")),
          List(
            Type.AnonymousParam(None),
            Type.AnonymousLambda(Type.Apply(pname("F"), List(Type.AnonymousParam(None))))
          )
        )
      )
    )
  }

  test("#3162 [scala30] higher-kinded is not wildcard 2") {
    implicit val dialect: Dialect = dialects.Scala30
    runTestAssert[Stat]("gr.pure[Resource[F, _]]")(
      Term.ApplyType(
        Term.Select(tname("gr"), tname("pure")),
        Type.AnonymousLambda(
          Type.Apply(
            pname("Resource"),
            List(pname("F"), Type.AnonymousParam(None))
          )
        ) :: Nil
      )
    )
  }

  test("#3162 [scala3+] higher-kinded is not wildcard 2") {
    implicit val dialect: Dialect = dialects.Scala3.withAllowUnderscoreAsTypePlaceholder(true)
    runTestAssert[Stat]("gr.pure[Resource[F, _]]")(
      Term.ApplyType(
        Term.Select(tname("gr"), tname("pure")),
        Type.AnonymousLambda(
          Type.Apply(
            pname("Resource"),
            List(pname("F"), Type.AnonymousParam(None))
          )
        ) :: Nil
      )
    )
  }

  test("star-dot") {

    runTestAssert[Stat](
      """|
         |given Conversion[*.type, List[*.type]] with
         |  def apply(ast: *.type) = ast :: Nil
         |""".stripMargin,
      Some("given Conversion[*.type, List[*.type]] with { def apply(ast: *.type) = ast :: Nil }")
    )(
      Defn.Given(
        Nil,
        anon,
        None,
        tpl(
          Init(
            Type.Apply(
              pname("Conversion"),
              List(
                Type.Singleton(tname("*")),
                Type.Apply(pname("List"), List(Type.Singleton(tname("*"))))
              )
            ),
            anon,
            emptyArgClause
          ) :: Nil,
          List(
            Defn.Def(
              Nil,
              tname("apply"),
              Nil,
              List(List(tparam("ast", Type.Singleton(tname("*"))))),
              None,
              Term.ApplyInfix(tname("ast"), tname("::"), Nil, List(tname("Nil")))
            )
          )
        )
      )
    )
  }

}
