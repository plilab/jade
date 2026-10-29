import java.util.ArrayList;

/*
 * Tnterface with various types of methods.
 */
public interface SampleInterface {
    // Abstract methods with no body. For methods of interfaces without bodies, if the `abstract` modifier is not present, in the bytecodes, the `abstract` modifier is automatically inserted.
    void sampleMethod();
    abstract void sampleMethod2();

    // Default methods with bodies.
    default int sampleDefaultMethod() {
        return 0;
    }

    // Default method returning void
    default void sampleDefaultMethod2() {}

    default ArrayList<Integer> sampleDefaultMethod3() {
        return new ArrayList<Integer>();
    }

    // Static methods with bodies.
    static int sampleStaticMethod() {
        return 1;
    }

    static void sampleStaticMethod2() {}

    static ArrayList<Integer> sampleStaticMethod3() {
        return new ArrayList<Integer>();
    }
}
