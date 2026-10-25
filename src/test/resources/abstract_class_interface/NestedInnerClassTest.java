/*
 * Test case for nested inner classes (inner class within an inner class).
 */

public class NestedInnerClassTest {
    private String outerField = "Outer field";

    public class InnerClass {
        private String innerField = "Inner field";

        public class InnerInnerClass {
            public void display() {
                System.out.println(outerField);
                System.out.println(innerField);
            }
        }

        public void runInnerInner() {
            InnerInnerClass innerInner = new InnerInnerClass();
            innerInner.display();
        }
    }

    public static void main(String[] args) {
        NestedInnerClassTest outer = new NestedInnerClassTest();
        NestedInnerClassTest.InnerClass inner = outer.new InnerClass();
        inner.runInnerInner();
    }
}
