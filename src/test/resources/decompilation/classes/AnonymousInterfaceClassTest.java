/* 
 * Test case for Anonymous Class from an Interface
 */

interface Greeting {
  void sayHello();
}

public class AnonymousInterfaceClassTest {
  public static void main(String[] args) {
    Greeting greet = new Greeting() {
      public void sayHello() {
        System.out.println("Hello, World!");
      }
    };

    greet.sayHello();
  }
}