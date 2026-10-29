import java.lang.annotation.*;

@interface FirstName {
    String first();
}

@FirstName(first="Joe")
public class AnnotationTest6 {
    // class body goes here
}
