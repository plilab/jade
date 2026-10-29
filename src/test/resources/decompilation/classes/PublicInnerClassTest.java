/*
 * Test case for public inner classes.
 */

public class PublicInnerClassTest {
    private String outerField = "Outer field";
    
    // Non-static inner class
    public class InnerClass {
        public void display() {
            System.out.println(outerField);
        }
    }
    
    public static void main(String[] args) {
        PublicInnerClassTest outer = new PublicInnerClassTest();
        PublicInnerClassTest.InnerClass inner = outer.new InnerClass();
        inner.display();
    }
}
