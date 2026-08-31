public final class ExpressionFoldingCases {
    private static volatile int volatileValue;

    private static int side(int value) {
        volatileValue = value;
        return value;
    }

    private static void sink(int value) {
        volatileValue = value;
    }

    private static void sink2(int first, int second) {
        volatileValue = first + second;
    }

    public static int basic(int y, int v) {
        int x = y + 1;
        int z = v + 2;
        int w = x * z;
        return w;
    }

    public static int direct(int y, int v) {
        return (y + 1) * (v + 2);
    }

    public static int sharedCall(int y) {
        int x = side(y);
        return x + x;
    }

    public static int orderedCalls(int y, int v) {
        return side(y) + side(v);
    }

    public static void orderedArguments(int y, int v) {
        sink2(side(y), side(v));
    }

    public static int joined(boolean flag, int y, int v) {
        int x;
        if (flag) {
            x = y + 1;
        } else {
            x = v + 2;
        }
        return x * 3;
    }

    public static int conditionalExpression(boolean flag, int y, int v) {
        return (flag ? y + 1 : v + 2) * 3;
    }

    public static boolean shortCircuit(Object value) {
        return value != null && value.hashCode() > 0;
    }

    public static int arrayOrder(int[] values, int index) {
        return values[index++] + values[index];
    }

    public static int exceptionOrder(int numerator, int denominator, int value) {
        return numerator / denominator + side(value);
    }

    public static int postIncrement(int value) {
        return value++ + value;
    }

    public static int oneVolatileRead() {
        int x = volatileValue;
        return x + x;
    }

    public static int unusedCall() {
        int x = side(1);
        return 2;
    }

    public static void disjointScopes(int value) {
        {
            int local = value + 1;
            sink(local);
        }
        {
            String local = "x";
            sink(local.hashCode());
        }
    }

    public static String concat(String left, int right) {
        return left + ":" + right;
    }
}