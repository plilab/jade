/*
 * Test case for private inner classes.
 */

public class PrivateInnerClassTest {
  int x = 10;

  private class InnerClass {
    int y = 5;
  }

  public static void main(String[] args) {
    PrivateInnerClassTest myOuter = new PrivateInnerClassTest();
    PrivateInnerClassTest.InnerClass myInner = myOuter.new InnerClass();
    System.out.println(myInner.y + myOuter.x);
  }
}