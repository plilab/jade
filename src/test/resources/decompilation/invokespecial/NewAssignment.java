/**
 * The `new` keyword should be used to allocate an object.
 * During decompilation, some instruction have the special name `<init>`,
 * but they should not be emitted in the final decompiled output since it's not a valid identifier.
 */

package invokespecial;

class NewAssignment {
    public static void main(String[] args) {
        Object value = new NewAssignmentObject();
    }
}

class NewAssignmentObject {}