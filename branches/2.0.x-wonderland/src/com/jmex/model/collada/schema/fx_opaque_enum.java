/**
 * fx_opaque_enum.java
 *
 * This file was generated by XMLSpy 2007sp2 Enterprise Edition.
 *
 * YOU SHOULD NOT MODIFY THIS FILE, BECAUSE IT WILL BE
 * OVERWRITTEN WHEN YOU RE-RUN CODE GENERATION.
 *
 * Refer to the XMLSpy Documentation for further details.
 * http://www.altova.com/xmlspy
 */


package com.jmex.model.collada.schema;

import com.jmex.xml.types.SchemaString;

public class fx_opaque_enum extends SchemaString {
	public static final int EA_ONE = 0; /* A_ONE */
	public static final int ERGB_ZERO = 1; /* RGB_ZERO */

	public static String[] sEnumValues = {
		"A_ONE",
		"RGB_ZERO",
	};

	public fx_opaque_enum() {
		super();
	}

	public fx_opaque_enum(String newValue) {
		super(newValue);
		validate();
	}

	public fx_opaque_enum(SchemaString newValue) {
		super(newValue);
		validate();
	}

	public static int getEnumerationCount() {
		return sEnumValues.length;
	}

	public static String getEnumerationValue(int index) {
		return sEnumValues[index];
	}

	public static boolean isValidEnumerationValue(String val) {
		for (int i = 0; i < sEnumValues.length; i++) {
			if (val.equals(sEnumValues[i]))
				return true;
		}
		return false;
	}

	public void validate() {

		if (!isValidEnumerationValue(toString()))
			throw new com.jmex.xml.xml.XmlException("Value of fx_opaque_enum is invalid.");
	}
}