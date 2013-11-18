/*
 * Copyright (c) 1996-2013 ApexSoft co.,td.
 * site: http://www.apexsoft.com.cn
 * 版权所有：福建顶点软件股份有限公司
 * 地址：福建省福州市软件大道89号软件园顶点软件中心 350003
 */

package net.sf.webdav.authorization;

import net.sf.webdav.Authorization;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: xpk
 * Date: 13-11-18
 * Time: 下午3:53
 */
public class BasicAuthorization implements Authorization {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
			.getLogger(BasicAuthorization.class);


	@Override
	public boolean showLogin(HttpServletRequest request, HttpServletResponse response, ServletConfig config) {

		String realm = config.getInitParameter("basic-realm");
		if (realm == null) realm = "WebDAV";
		response.addHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		return false;
	}

	@Override
	public boolean validate(HttpServletRequest req, ServletConfig config) {

		if ("true".equals(getInitParameter(config, "basic-auth-enabled"))) {

//			org/apache/commons/codec/binary/Base64
//	    Authorization:Basic eHBrOnBhc3N3b3Jk
			//debug xpk password
			final String auth = req.getHeader("Authorization");
			String user = null;
			String pwd = null;
			boolean authed = false;
			if (auth != null && auth.startsWith("Basic ")) {
				LOG.trace(auth);
				String base64 = auth.substring(auth.indexOf(" ") + 1).trim();
				String data = new String(Base64.decodeBase64(base64));
				//data :    xpk:password
				int pos = 0;
				while ((pos = data.indexOf(":", pos)) != -1) {//多个:时
					if ((authed = authByUserAndPassword(data.substring(0, pos), data.substring(pos + 1), config)))
						break;
					pos++;
				}
			}
			return authed;
		}
		return true;
	}

	private String getInitParameter(ServletConfig config, String s) {
		try {
			return config.getInitParameter(s);
		} catch (Throwable e) {
			return null;
		}
	}


	private boolean authByUserAndPassword(String user, String pwd, ServletConfig config) {
		String s_user = getInitParameter(config, ("basic-auth-user"));
		if (s_user == null) s_user = "admin";
		String s_pwd = getInitParameter(config, ("basic-auth-password"));
		if (s_pwd == null) s_pwd = "pwd~123";
		LOG.trace("authByUserAndPassword:" + user + "?" + pwd);
		return (user.equals(s_user) && pwd.equals(s_pwd));
	}

}
