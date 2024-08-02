import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

// Annotation declaration with a list as parameter (@Target uses a list of ElementType as parameter)
// https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/Target.html
@Target(ElementType.ANNOTATION_TYPE)
public @interface AnnotationTest1 {
}
