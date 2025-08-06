public class phitest {
    public static boolean cond = true;
    public static void main(String[] args) {
        int x = 0;
        if (cond) {
            x = 1;
        } else {
            x = 2;
        }
        int y = x + 3;
        System.out.println(y);
    }
}
