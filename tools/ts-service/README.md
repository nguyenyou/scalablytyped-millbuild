## TS Port

### How to create object literal

#### js.Dynamic

```scala
js.Dynamic.literal(foo = 42, bar = "foobar")
```

or

```scala
js.Dynamic.literal("foo" -> 42, "bar" -> "foobar")
```

will give us

```scala
{foo: 42, bar: "foobar"}
```