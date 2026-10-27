public final class ExpressionFoldingCases {
    public static int basic(int y, int v) {
        int x = y + 1;
        int z = v + 2;
        int w = x * z;
        return w;
    }

    public static int direct(int y, int v) {
        return (y + 1) * (v + 2);
    }

    public static int reusedValue(int a) {
        int x = fn(a);
        return x + x;
    }

    public static int fn(int a) {
        return a + 1;
    }

    public static int ifStatement(int a, int b) {
        if (a > b) {
            return a;
        } else {
            return b;
        }
    }

    public static int chainedPhi(boolean a, boolean b) {
        int x;
        if (a) {
            x = 10;
        } else {
            x = 20;
        }

        if (b) {
            x = 30;
        }

        return x;
    }

    public static int loopFoldability() {
        int sum = 1;

        for (int i = 0; i < 5; i++) {
            int adjusted = sum + i;
            int doubled = adjusted * 2;
            sum = adjusted + doubled;
        }

        return sum;
    }

    public static int conditionalInitLoop(int n) {
        int result = 7;
        int x;

        while (n > 0) {
            if (n % 2 == 0) x = 42;
            n--;
        }

        return result;
    }

    public static int arrayReadThenWrite(int[] a) {
        int x = a[0];
        a[0] = 9;
        return x;
    }

    public static int repeatedOperands(int value) {
        return value + value;
    }

    public static int callOrder(int[] trace) {
        int firstValue = first(trace);
        int secondValue = second(trace);
        return secondValue + firstValue;
    }

    public static int first(int[] trace) {
        trace[0] = trace[0] * 10 + 1;
        return 1;
    }

    public static int second(int[] trace) {
        trace[0] = trace[0] * 10 + 2;
        return 2;
    }
}
