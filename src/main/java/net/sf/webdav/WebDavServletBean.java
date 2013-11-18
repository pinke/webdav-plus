/*
 * Copyright (c) 1996-2013 ApexSoft co.,td.
 * site: http://www.apexsoft.com.cn
 * 版权所有：福建顶点软件股份有限公司
 * 地址：福建省福州市软件大道89号软件园顶点软件中心 350003
 */

package net.sf.webdav;

import net.sf.webdav.exceptions.UnauthenticatedException;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.fromcatalina.MD5Encoder;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.methods.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class WebDavServletBean extends HttpServlet {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
			.getLogger(WebDavServletBean.class);

	/**
	 * MD5 message digest provider.
	 */
	protected static MessageDigest MD5_HELPER;

	/**
	 * The MD5 helper object for this class.
	 */
	protected static final MD5Encoder MD5_ENCODER = new MD5Encoder();

	private static final boolean READ_ONLY = false;
	private ResourceLocks _resLocks;
	private IWebdavStore _store;
	private HashMap<String, IMethodExecutor> _methodMap = new HashMap<String, IMethodExecutor>();

	private Collection<Authorization> authorizations = new LinkedHashSet<Authorization>();

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String auths = config.getInitParameter("authorization-class");
		if (auths != null) {
			for (String auth : auths.split(";")) {
				initAuthorization(auth);
			}
		}
	}

	private void initAuthorization(String className) {
		try {
			Object clz = Class.forName(className).newInstance();
			if (clz instanceof Authorization) {
				authorizations.add((Authorization) clz);
			} else {
				LOG.error("config error class name {} don't instance of Authorization ", className);
			}
		} catch (InstantiationException e) {
			LOG.error("config error form class name {} ,{}", e.getMessage(), e);
		} catch (IllegalAccessException e) {
			LOG.error("config error form class name {} ,{}", e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			LOG.error("config error class name {} not found ", className);
		}
	}

	public WebDavServletBean() {
		_resLocks = new ResourceLocks();

		try {
			MD5_HELPER = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException();
		}
	}

	public void init(IWebdavStore store, String dftIndexFile,
	                 String insteadOf404, int nocontentLenghHeaders,
	                 boolean lazyFolderCreationOnPut) throws ServletException {

		_store = store;

		IMimeTyper mimeTyper = new IMimeTyper() {
			public String getMimeType(String path) {
				return getServletContext().getMimeType(path);
			}
		};

		register("GET", new DoGet(store, dftIndexFile, insteadOf404, _resLocks,
				mimeTyper, nocontentLenghHeaders));
		register("HEAD", new DoHead(store, dftIndexFile, insteadOf404,
				_resLocks, mimeTyper, nocontentLenghHeaders));
		DoDelete doDelete = (DoDelete) register("DELETE", new DoDelete(store,
				_resLocks, READ_ONLY));
		DoCopy doCopy = (DoCopy) register("COPY", new DoCopy(store, _resLocks,
				doDelete, READ_ONLY));
		register("LOCK", new DoLock(store, _resLocks, READ_ONLY));
		register("UNLOCK", new DoUnlock(store, _resLocks, READ_ONLY));
		register("MOVE", new DoMove(_resLocks, doDelete, doCopy, READ_ONLY));
		register("MKCOL", new DoMkcol(store, _resLocks, READ_ONLY));
		register("OPTIONS", new DoOptions(store, _resLocks));
		register("PUT", new DoPut(store, _resLocks, READ_ONLY,
				lazyFolderCreationOnPut));
		register("PROPFIND", new DoPropfind(store, _resLocks, mimeTyper));
		register("PROPPATCH", new DoProppatch(store, _resLocks, READ_ONLY));
		register("*NO*IMPL*", new DoNotImplemented(READ_ONLY));
	}

	private IMethodExecutor register(String methodName, IMethodExecutor method) {
		_methodMap.put(methodName, method);
		return method;
	}

	/**
	 * Handles the special WebDAV methods.
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		//
		for (Authorization authorization : authorizations) {
			if (!authorization.validate(req, getServletConfig())) {
				if (!authorization.showLogin(req, resp, getServletConfig()))
					return;
			}
		}
		String methodName = req.getMethod();
		ITransaction transaction = null;
		boolean needRollback = false;

		if (LOG.isTraceEnabled())
			debugRequest(methodName, req);

		try {
			Principal userPrincipal = req.getUserPrincipal();
			transaction = _store.begin(userPrincipal);
			needRollback = true;
			_store.checkAuthentication(transaction);
			resp.setStatus(WebdavStatus.SC_OK);

			try {
				IMethodExecutor methodExecutor = (IMethodExecutor) _methodMap
						.get(methodName);
				if (methodExecutor == null) {
					methodExecutor = (IMethodExecutor) _methodMap
							.get("*NO*IMPL*");
				}

				methodExecutor.execute(transaction, req, resp);

				_store.commit(transaction);
				needRollback = false;
			} catch (IOException e) {
				java.io.StringWriter sw = new java.io.StringWriter();
				java.io.PrintWriter pw = new java.io.PrintWriter(sw);
				e.printStackTrace(pw);
				LOG.error("IOException: " + sw.toString());
				resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
				_store.rollback(transaction);
				throw new ServletException(e);
			}

		} catch (UnauthenticatedException e) {
			resp.sendError(WebdavStatus.SC_FORBIDDEN);
		} catch (WebdavException e) {
			java.io.StringWriter sw = new java.io.StringWriter();
			java.io.PrintWriter pw = new java.io.PrintWriter(sw);
			e.printStackTrace(pw);
			LOG.error("WebdavException: " + sw.toString());
			throw new ServletException(e);
		} catch (Exception e) {
			java.io.StringWriter sw = new java.io.StringWriter();
			java.io.PrintWriter pw = new java.io.PrintWriter(sw);
			e.printStackTrace(pw);
			LOG.error("Exception: " + sw.toString());
		} finally {
			if (needRollback)
				_store.rollback(transaction);
		}

	}


	private void debugRequest(String methodName, HttpServletRequest req) {
		LOG.trace("-----------");
		LOG.trace("WebdavServlet\n request: methodName = " + methodName);
		LOG.trace("time: " + System.currentTimeMillis());
		LOG.trace("path: " + req.getRequestURI());
		LOG.trace("-----------");
		Enumeration<?> e = req.getHeaderNames();
		while (e.hasMoreElements()) {
			String s = (String) e.nextElement();
			LOG.trace("header: " + s + " " + req.getHeader(s));
		}
		e = req.getAttributeNames();
		while (e.hasMoreElements()) {
			String s = (String) e.nextElement();
			LOG.trace("attribute: " + s + " " + req.getAttribute(s));
		}
		e = req.getParameterNames();
		while (e.hasMoreElements()) {
			String s = (String) e.nextElement();
			LOG.trace("parameter: " + s + " " + req.getParameter(s));
		}
	}

}
