/**
 * When a field has no initializer, it should not be set to null.
 *
 * If the field is a primitive, setting it to null is invalid.
 * If the field is not a primitive, it is null be default, so there is no need to set to null.
 *
 * See https://github.com/plilab/jade/pull/10 for context.
 */
class PrimitiveFieldShouldNotBeNull {
    public int uninitializedValue;
    public int initializedValue = 42;
}
