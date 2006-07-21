/* RequestInfo.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Wed Apr 19 14:02:14     2006, Created by tomyeh@potix.com
}}IS_NOTE

Copyright (C) 2006 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package com.potix.zk.ui.sys;

import com.potix.util.resource.Locator;

import com.potix.zk.ui.WebApp;
import com.potix.zk.ui.Desktop;
import com.potix.zk.ui.Session;

/**
 * The request information used with {@link com.potix.zk.ui.sys.UiFactory}.
 *
 * @author <a href="mailto:tomyeh@potix.com">tomyeh@potix.com</a>
 */
public interface RequestInfo {
	/** Returns the Web application; never null.
	 */
	public WebApp getWebApp();
	/** Returns the session, or null if not available.
	 */
	public Session getSession();
	/** Returns the desktop, or null if not created yet.
	 * In other words, it returns null if
	 * {@link com.potix.zk.ui.sys.UiFactory#newDesktop} is called.
	 */
	public Desktop getDesktop();
	/** Returns the native request. For HTTP, it is
	 * javax.servlet.http.HttpServletRequest. For portlets, it is
	 * javax.portlet.RenderRequest
	 */
	public Object getNativeRequest();

	/** Returns the locator used to retrieve resources, such as
	 * taglib and zscript files, or null if {@link #getWebApp} will be used
	 * as the locator.
	 */
	public Locator getLocator();
	/** Sets the locator used to retrieve resources, such as
	 * taglib and zscript files.
	 *
	 * @param locator the locator used to locate taglib and other resources.
	 * If null, {@link #getWebApp} is used.
	 */
	public void setLocator(Locator locator);
}
