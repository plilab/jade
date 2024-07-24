import java.util.ArrayList;

public interface SampleInterface {
    // Abstract methods with no body.
    // For regular methods declarations of interfaces without bodies, if the `abstract` modifier is not explicitly specified, in the bytecodes, the `abstract` modifier is automatically inserted.
    void sampleMethod();
    abstract void sampleMethod2();

    // Default methods with bodies.
    default int sampleDefaultMethod() {
        return 0;
    }

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