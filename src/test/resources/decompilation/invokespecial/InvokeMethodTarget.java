/**
 * `invokespecial` can be used to invoke methods belonging to a class, its superclass or interfaces.
 * Each invocation should resolve to the correct target.
 */

package invokespecial;

class InvokeMethodTarget extends InvokeMethodTargetParentClass implements InvokeMethodTargetInterface1, InvokeMethodTargetInterface2 {
    void foo(int value) {
        super.foo();
    }

    @Override
    void foo() {
        this.foo(42);
    }

    @Override
    public void bar() {
        InvokeMethodTargetInterface1.super.bar();
        InvokeMethodTargetInterface2.super.bar();
    }
}

class InvokeMethodTargetParentClass {
    void foo() {}
}

interface InvokeMethodTargetInterface1 {
    default void bar() {}
}

interface InvokeMethodTargetInterface2 {
    default void bar() {}
}