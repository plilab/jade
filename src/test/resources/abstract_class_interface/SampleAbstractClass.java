/*
 * Abstract class with various types of methods.
 */

abstract class SampleAbstractClass {
    // abstract method
    public abstract void method1();
    
    // non-abstract method
    public void method2(String food) {
        System.out.println(food);
    }
}
