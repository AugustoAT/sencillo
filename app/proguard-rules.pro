# Room y Compose traen sus propias reglas. En release se eliminan los logs de depuración.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
