/*
 * Class with constructor that passes parameter to super.
 */

import java.util.Arrays;
import java.util.stream.Collectors;

class SampleChildClass extends Exception {
    
    // Constructor that passes message to super
    public SampleChildClass(String message) {
        super(message.length() > 0 ? message : null);
    }

    // Constructor that calls this()
    public SampleChildClass() {
        this("Default error");
    }

    // Constructor with complicated super() arguments using split() and map()
    public SampleChildClass(String message, String delimiter) {
        super(Arrays.stream(message.split(delimiter))
            .collect(Collectors.joining(" ")));
        System.out.println(message);
    }

}
