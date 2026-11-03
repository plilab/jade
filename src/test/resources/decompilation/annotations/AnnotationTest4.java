import java.lang.annotation.*;

/**
 * Nested annotations.
 * Source: https://docs.oracle.com/javase/specs/jls/se17/html/jls-9.html#jls-9.6.1
 */
@interface Author {
    Name value();
}

@interface Name {
    String first();
    String last();
}

@Author(@Name(first = "Joe", last = "Hacker"))
public class AnnotationTest4 {
    // class body goes here
}
