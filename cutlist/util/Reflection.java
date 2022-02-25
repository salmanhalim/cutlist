package cutlist.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Reflection {
    protected static String getMethodNameFromProperty(String prefix, String propertyName) {
        return prefix + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }

    /**
     * Sample call:
     * <code>Reflection.copyProperty(cutList, test, "cutPreference")</code>
     *
     * This will do the equivalent of <code>test.setCutPreference(cutList.getCutPreference())</code>
     *
     * @return true if successful, false if not
     */
    public static boolean copyProperty(Object source, Object destination, String propertyName) {
        Class<?> sourceClass      = source.getClass();
        Class<?> destinationClass = destination.getClass();
        Method   getter           = null;

        // System.out.println("-=-=-=-=-= copyProperty " + "[" + "sourceClass: " + sourceClass + "], [" + "destinationClass: " + destinationClass + "], [" + "propertyName: " + propertyName + "]" + " (" + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        // Try getPropertyName and then isPropertyName if get doesn't work
        // If we have more than just two variants to try, extract this out into a loop and have it return an Optional<Method>.
        try {
            getter = sourceClass.getDeclaredMethod(getMethodNameFromProperty("get", propertyName));
        } catch (NoSuchMethodException nsme) {
            try {
                getter = sourceClass.getDeclaredMethod(getMethodNameFromProperty("is", propertyName));
            } catch (NoSuchMethodException nsme2) {
                // Do nothing; a null value is handled later.
            }
        }

        // System.out.println("-=-=-=-=-= getter: " + (getter) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        if (getter == null) {
            System.out.println(String.format("No getter found for property %s in source class %s.", propertyName, sourceClass));

            return false;
        }

        Class<?> parameterType = getter.getReturnType();

        // System.out.println("-=-=-=-=-= parameterType: " + (parameterType) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

        try {
            Method setter = destinationClass.getDeclaredMethod(getMethodNameFromProperty("set", propertyName), parameterType);

            // System.out.println("-=-=-=-=-= setter: " + (setter) + " (" + new java.util.Date() + " " + (new Exception().getStackTrace()[0]) + ")"); // SALMAN: remove

            setter.invoke(destination, getter.invoke(source));
        } catch (NoSuchMethodException nsme) {
            System.out.println(String.format("No setter found for property %s in source class %s.", propertyName, destination));

            return false;
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println(String.format("Unable to set property %s in %s (source class: %s); access denied.", propertyName, destinationClass, sourceClass));
        }

        return true;
    }
}
