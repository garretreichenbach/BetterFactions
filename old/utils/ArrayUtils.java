package thederpgamer.betterfactions.utils;

/**
 * <Description>
 *
 * @author Garret Reichenbach
 * @version 1.0 - [05/17/2022]
 */
public class ArrayUtils {

	public static boolean contains(Object[] array, Object object) {
		if(array.length > 0) {
			if(object.getClass().isAssignableFrom(array[0].getClass())) {
				for(Object temp : array) if(temp.equals(object)) return true;
			}
		}
		return false;
	}
}
