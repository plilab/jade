public class Test3 {
    public static void main(String[] args) {
        int[] arr = {-2,1,-3};
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i != j) {
                    sum += arr[i] * arr[j];
                }
            }
        }
    }
}
