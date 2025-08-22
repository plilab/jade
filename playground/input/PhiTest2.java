public class PhiTest2 {
    public static boolean cond = true;
  
    public static void main(String[] args) {
      int x = 1;
      outer: while (true) {
        if (x > 0) {
          // jump out of outer and immediately start other loop
          break outer;
        }
        System.out.println("inside first loop");
      }
  
      while (x < 10) {
        x++;
        System.out.println("inside second loop");
      }
    }
  }