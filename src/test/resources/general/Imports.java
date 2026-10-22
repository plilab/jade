import java.util.List;
import java.util.Random;

/**
 * Instead of using fully qualified names for all type declarations,
 * some types can be simplified with import statements.
 *
 * See https://github.com/plilab/jade/pull/17 for context.
 */
public class Imports {
    // Should be List instead of java.util.List.
    void parameterizedImportedType() {
        List<Integer> numbers = List.of(1, 2, 3);
        for (int number : numbers) {
            System.out.println(number);
        }
    }

    // Should be Random instead of java.util.Random.
    void nonParameterizedImportedType() {
        Random rng = new Random();
        System.out.println(rng.nextInt());
    }

    // Should not import java.util.Date nor java.sql.Date.
    void shouldNotImportAmbiguousNames() {
        java.util.Date d1 = new java.util.Date();
        java.sql.Date d2 = new java.sql.Date(0);
        System.out.println(d1);
        System.out.println(d2);
    }

    // Types under java.lang are provided by default, and do not require imports.
    void shouldNotImportJavaLangNames() {
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        sb.append('a');
        sb.append('b');
        sb.append('c');
        System.out.println(sb.toString());
    }
}
