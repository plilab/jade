# Decompilation Edge Cases

This documents examples where other Java decompilers may fail to produce correct output.

## Interface `super` method targets

The [`invokespecial`](https://docs.oracle.com/javase/specs/jvms/se26/html/jvms-6.html#jvms-6.5.invokespecial) instruction is used to invoke instance methods.
When deciding what method to invoke, there are 3 options:

1. The class's own method
2. The superclass's method
3. An interface's method (i.e. `SomeInterface.super.someMethod()`)

Given the example below:

```java
class Foo implements FooInterface1, FooInterface2 {
    @Override
    public void foo() {
        FooInterface1.super.foo();
        FooInterface2.super.foo();
    }
}

interface FooInterface1 {
    default void foo() {}
}

interface FooInterface2 {
    default void foo() {}
}
```

FernFlower fails to decompile this correctly (as of version 253.29346.240), producing:

```java
class Foo implements FooInterface1, FooInterface2 {
   Foo() {}

   public void foo() {
      super.foo();
      super.foo();
   }
}
```
