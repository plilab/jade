/**
 * Special method names.
 * 
 * These are not valid Java names, but are special exceptions for initialization.
 * https://docs.oracle.com/javase/specs/jvms/se26/html/jvms-4.html#jvms-4.2
 */
object MethodName {
  const val INIT = "<init>"
  const val CLINIT = "<clinit>"
}
