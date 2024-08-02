import java.lang.annotation.*;

// Annotation with multiple parameters
// Alternatively, we can use the built-in @Generated with multiple parameters
// https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/annotation/processing/Generated.html
@interface Name {
    String first();
    String last();
}

@Name(first = "Joe", last = "Hacker")
public class AnnotationTest5 {
    // class body goes here
}
