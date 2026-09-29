/**
 * The decompiled output should invoke the correct constructors even when they are overloaded.
 * It should be able to diffrentiate between calls to `this` and `super`.
 */

package invokespecial;

class OverloadedConstructor extends OverloadedConstructorParentClass {
    OverloadedConstructor(String name) {
        super(name);
    }

    OverloadedConstructor(String name, boolean useName) {
        this(name);
    }
}

class OverloadedConstructorParentClass {
    OverloadedConstructorParentClass(String name) {}
}
