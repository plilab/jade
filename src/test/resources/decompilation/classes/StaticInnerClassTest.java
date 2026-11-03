/*
 * Test case for inner static classes.
 */

public class StaticInnerClassTest {
    private static String outerStaticField = "Outer static field";
    
    // Static nested class
    public static class StaticNestedClass {
        public void display() {
            System.out.println("Accessing: " + outerStaticField);
        }
    }
    
    public static void main(String[] args) {
        StaticNestedClass nested = new StaticNestedClass();
        nested.display();
    }
}
