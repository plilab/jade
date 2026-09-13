/*
Test Case for multiple .class file
*/
class Animal {
  public void makeSound() {
    System.out.println("Animal sound");
  }
}

class Dog extends Animal {
    public void makeSound() {
        System.out.println("woof woof");
  }
}

public class Test5 {
  public static void main(String[] args) {
    int i = 0;
  }
}