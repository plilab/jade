// Custom annotation declaration with a single parameter
@interface FirstName {
    String value();
}

@FirstName("Joe")
public class AnnotationTest7 {
    // class body goes here
}
