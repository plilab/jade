public class NestingTest4 {
    public static void main(String[] args) {
      int i = 0;
      while (i < 3) {
        int j = 0;
        while (j < 2) {
          j++;
        }
        i++;
      }
    }
  }