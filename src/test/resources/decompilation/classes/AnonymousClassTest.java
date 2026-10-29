/*
 * Test case for anonymous classes.
 */

class Animal {
  public void makeSound() {
    System.out.println("Animal sound");
  }
}

public class AnonymousClassTest {
  public static void main(String[] args) {
    Animal myAnimal = new Animal() {
      public void makeSound() {
        System.out.println("Woof woof");
      }
    }; 

    myAnimal.makeSound();
  }
}