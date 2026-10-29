/**
 * `super()` should be called with the correct arguments.
 */

package invokespecial;

class SuperclassConstructor {
    SuperclassConstructor(String name) {}
}

class SuperclassConstructorChildClass extends SuperclassConstructor {
    SuperclassConstructorChildClass(String name) {
        super(name);
    }
}
