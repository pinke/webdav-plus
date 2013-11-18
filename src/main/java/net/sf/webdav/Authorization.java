/*
 * Copyright (c) 1996-2013 ApexSoft co.,td.
 * site: http://www.apexsoft.com.cn
 * 版权所有：福建顶点软件股份有限公司
 * 地址：福建省福州市软件大道89号软件园顶点软件中心 350003
 */

package net.sf.webdav;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: xpk
 * Date: 13-11-18
 * Time: 下午3:53
 */
public interface Authorization {

	/**
	 *
	 * @param request     HttpServletRequest
	 * @param response    HttpServletRequest
	 * @param config ServletConfig
	 * @return true|false   can continue
	 */

	public boolean showLogin(HttpServletRequest request, HttpServletResponse response, ServletConfig config);

	public boolean validate(HttpServletRequest request, ServletConfig config);
}
