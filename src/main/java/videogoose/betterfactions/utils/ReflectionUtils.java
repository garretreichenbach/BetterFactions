package videogoose.betterfactions.utils;

import java.lang.reflect.*;
import java.util.Map;

/**
 * Utility class for reflection-based operations, including runtime enum injection.
 */
public class ReflectionUtils {

	private static final Map<Class<?>, Class<?>> BOXED_TO_PRIMITIVE = Map.of(
		Integer.class, int.class,
		Long.class, long.class,
		Double.class, double.class,
		Float.class, float.class,
		Boolean.class, boolean.class,
		Byte.class, byte.class,
		Short.class, short.class
	);

	public static Object invokePrivateMethod(Object instance, String methodName, Class<?>[] paramTypes, Object... args) throws ReflectiveOperationException {
		Method method = instance.getClass().getDeclaredMethod(methodName, paramTypes);
		method.setAccessible(true);
		return method.invoke(instance, args);
	}

	public static Object invokePrivateStaticMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes, Object... args) throws ReflectiveOperationException {
		Method method = clazz.getDeclaredMethod(methodName, paramTypes);
		method.setAccessible(true);
		return method.invoke(null, args);
	}

	public static Object getPrivateField(Class<?> clazz, Object instance, String fieldName) throws ReflectiveOperationException {
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(instance);
	}

	public static void setPrivateField(Class<?> clazz, Object instance, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(instance, value);
	}

	/**
	 * Injects a new enum value into the specified enum class at runtime.
	 *
	 * @param enumClass The enum class to modify
	 * @param name      The name of the new enum constant
	 * @param ordinal   The ordinal value for the new enum constant
	 * @param params    Additional constructor parameters (after the implicit String name and int ordinal)
	 * @return The newly created enum instance
	 * @throws Exception if injection fails
	 */
	public static Object injectEnumValue(Class<?> enumClass, String name, int ordinal, Object... params) throws Exception {
		// Read the existing $VALUES array
		Field valuesField = enumClass.getDeclaredField("$VALUES");
		valuesField.setAccessible(true);
		Object[] oldValues = (Object[]) valuesField.get(null);

		// Build a new array with space for the new constant
		Object[] newValues = (Object[]) Array.newInstance(enumClass, oldValues.length + 1);
		System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);

		// Build constructor parameter types: [String, int, ...params]
		Class<?>[] paramTypes = buildParamTypes(params);

		// Find a matching constructor
		Constructor<?> constructor = findConstructor(enumClass, paramTypes);
		constructor.setAccessible(true);

		// Build constructor arguments: [name, ordinal, ...params]
		Object[] args = new Object[2 + params.length];
		args[0] = name;
		args[1] = ordinal;
		System.arraycopy(params, 0, args, 2, params.length);

		// Create the new enum instance
		Object newEnumInstance = createEnumInstance(constructor, args, name);
		newValues[oldValues.length] = newEnumInstance;

		// Write the new $VALUES array back to the enum class
		setStaticFinalField(valuesField, newValues);

		// Injection successful
		return newEnumInstance;
	}

	private static Class<?>[] buildParamTypes(Object[] params) {
		Class<?>[] paramTypes = new Class<?>[2 + params.length];
		paramTypes[0] = String.class;
		paramTypes[1] = int.class;
		for (int i = 0; i < params.length; i++) {
			Class<?> paramClass = params[i].getClass();
			paramTypes[i + 2] = BOXED_TO_PRIMITIVE.getOrDefault(paramClass, paramClass);
		}
		return paramTypes;
	}

	private static Constructor<?> findConstructor(Class<?> enumClass, Class<?>[] paramTypes) throws NoSuchMethodException {
		// Try exact match first
		try {
			return enumClass.getDeclaredConstructor(paramTypes);
		} catch (NoSuchMethodException ignored) {}

		// Try with boxed types
		Class<?>[] boxedTypes = Arrays.copyOf(paramTypes, paramTypes.length);
		for (int i = 2; i < boxedTypes.length; i++) {
			if (boxedTypes[i] == int.class) boxedTypes[i] = Integer.class;
			else if (boxedTypes[i] == long.class) boxedTypes[i] = Long.class;
		}
		try {
			return enumClass.getDeclaredConstructor(boxedTypes);
		} catch (NoSuchMethodException ignored) {}

		// Fall back to matching by parameter count
		for (Constructor<?> c : enumClass.getDeclaredConstructors()) {
			if (c.getParameterTypes().length == paramTypes.length) {
				return c;
			}
		}
		throw new NoSuchMethodException("No matching constructor found for " + enumClass.getName() + " with " + paramTypes.length + " parameters");
	}

	private static Object createEnumInstance(Constructor<?> constructor, Object[] args, String name) {
		// Method 1: ReflectionFactory (most reliable for enums)
		try {
			Class<?> rfClass = Class.forName("sun.reflect.ReflectionFactory");
			Object rf = rfClass.getDeclaredMethod("getReflectionFactory").invoke(null);
			Object accessor = rfClass.getDeclaredMethod("newConstructorAccessor", Constructor.class).invoke(rf, constructor);
			Method newInstance = accessor.getClass().getDeclaredMethod("newInstance", Object[].class);
			newInstance.setAccessible(true);
			return newInstance.invoke(accessor, new Object[]{args});
		} catch (Exception e) {
			// ReflectionFactory failed, trying override bypass
		}

		// Method 2: Override field bypass
		try {
			Field overrideField;
			try {
				overrideField = Constructor.class.getDeclaredField("override");
			} catch (NoSuchFieldException ex) {
				overrideField = AccessibleObject.class.getDeclaredField("override");
			}
			overrideField.setAccessible(true);
			overrideField.set(constructor, true);
			return constructor.newInstance(args);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create enum instance for " + name, e);
		}
	}

	private static void setStaticFinalField(Field field, Object value) {
		// Method 1: Unsafe
		try {
			Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
			Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Object unsafe = theUnsafe.get(null);

			Object base = unsafeClass.getDeclaredMethod("staticFieldBase", Field.class).invoke(unsafe, field);
			long offset = (long) unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class).invoke(unsafe, field);
			unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, base, offset, value);
			return;
		} catch (Exception e) {
			// Unsafe failed, trying modifier removal
		}

		// Method 2: Remove FINAL modifier
		try {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			field.set(null, value);
		} catch (Exception e) {
			throw new RuntimeException("Could not set $VALUES field for enum injection", e);
		}
	}
}
