package deltix.connector.common.util;

import deltix.ember.message.trade.CustomAttribute;
import deltix.util.collections.generated.ObjectList;
import edu.umd.cs.findbugs.annotations.Nullable;

public class ConnectorUtil {

	/**
	 * Post only equals to ExecInst  = PARTICIPATE_DONT_INITIATE
	 */
	public static final int POST_ONLY_TAG = 18;
	public static final byte POST_ONLY_VALUE_BYTE = '6';
	public static final char POST_ONLY_VALUE_CHAR = '6';

	public static boolean isPostOnly(byte value) {
		return value == POST_ONLY_VALUE_BYTE;
	}

	public static boolean isPostOnly(char value) {
		return value == POST_ONLY_VALUE_CHAR;
	}

	public static boolean isPostOnly(@Nullable ObjectList<CustomAttribute> attributes) {
		if (attributes == null || attributes.isEmpty())
			return false;

		for (int i = 0; i < attributes.size(); i++) {
			CustomAttribute attribute = attributes.get(i);
			if (attribute.getKey() == POST_ONLY_TAG) {
				CharSequence value = attribute.getValue();
				if (value == null || value.length() != 1)
					return false;
				return isPostOnly(value.charAt(0));
			}
		}
		return false;
	}
}