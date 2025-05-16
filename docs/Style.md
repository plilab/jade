# Style Guide

Linters that we use
- ktlint
- detekt (but not formatting because it is just ktlint)
- diktat
- Not sonarlint (TODO: why?)
- Not ktfmt (TODO: why?)

How to run linters: run from clean

Other code guides

## Philosophical Principles

- Can be reproduced over code base
- Ease of maintinance and readability
- Concision
- Obviously has no bugs, not has no obvious bugs

## Rules

### Automated

- Prefer `"${x}"` over `"$x"`.
  - Reason: Avoids mistakes like "$x.y" and minimizes change when going from `"$x"` to `"${y.x}"`
  - [Implementation](../buildSrc/src/main/kotlin/org/michaeldadams/ktlint/LongStringTemplateRule.kt)

### Not Yet Automated

- Prefer `this.x`.  Reason: Makes it clear that `x` is not a local.
- Style preventing foo.getBar() and foo.setBar(x) (unless result is used)
- Style for `class Foo: Bar` vs `class Foo : Bar`
- Style for `object: Foo { ... }` vs `object : Foo { ... }`
- Style `List()` vs `listOf()`
- Style `fun foo(): Unit { ... }` vs `fun foo() { ... }`
- Style no .first
- Style for Clikt options always being before arguments
- Use buildString instead of StringBuilder
- Style for `@Suppress("detekt:FooBar")`

### Will not be automated

### Unsorted

- No newline after `/**`
- Add links for classes mentioned in KDoc
- Style for line break (or not) before tags in KDoc
- Style for Any vs Object (e.g., decompileAnnotationParameter)
- trailing commas

## TODO

- Have linters warn about private members without KDoc
- KDoc Tag Order:
  - KDOC_WRONG_TAGS_ORDER: @receiver, @param, @property, @return, @throws or @exception, @constructor
