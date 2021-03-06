/* FormStatus.java

	Purpose:
		
	Description:
		
	History:
		2011/12/1 Created by Dennis Chen

Copyright (C) 2011 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.bind;

/**
 * Represents the runtime information of the {@link Form} 
 * @author dennis
 * @since 6.0.0
 */
public interface FormStatus {

	/**
	 * Returns whether the form has been modified 
	 * @return whether the form has been modified.
	 * @see Form#isDirty()
	 */
	public boolean isDirty();
}
