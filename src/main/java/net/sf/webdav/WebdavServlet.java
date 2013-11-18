/*
 * Copyright (c) 1996-2013 ApexSoft co.,td.
 * site: http://www.apexsoft.com.cn
 * 版权所有：福建顶点软件股份有限公司
 * 地址：福建省福州市软件大道89号软件园顶点软件中心 350003
 */

package net.sf.webdav;

import net.sf.webdav.exceptions.WebdavException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.File;
import java.lang.reflect.Constructor;

/**
 * Servlet which provides support for WebDAV level 2.
 * <p/>
 * the original class is org.apache.catalina.servlets.WebdavServlet by Remy
 * Maucherat, which was heavily changed
 *
 * @author Remy Maucherat
 */

public class WebdavServlet extends WebDavServletBean {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebdavServlet.class);
	private static final String ROOTPATH_PARAMETER = "rootpath";

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		// Parameters from web.xml
		String clazzName = config.getInitParameter(
				"ResourceHandlerImplementation");
		if (clazzName == null || clazzName.equals("")) {
			clazzName = LocalFileSystemStore.class.getName();
		}

		File root = getFileRoot();

		IWebdavStore webdavStore = constructStore(clazzName, root);

		boolean lazyFolderCreationOnPut = getInitParameter("lazyFolderCreationOnPut") != null
				&& getInitParameter("lazyFolderCreationOnPut").equals("1");

		String dftIndexFile = getInitParameter("default-index-file");
		String insteadOf404 = getInitParameter("instead-of-404");

		int noContentLengthHeader = getIntInitParameter("no-content-length-headers");

		super.init(webdavStore, dftIndexFile, insteadOf404,
				noContentLengthHeader, lazyFolderCreationOnPut);
	}

	private int getIntInitParameter(String key) {
		return getInitParameter(key) == null ? -1 : Integer
				.parseInt(getInitParameter(key));
	}

	protected IWebdavStore constructStore(String clazzName, File root) {
		IWebdavStore webdavStore;
		try {
			Class<?> clazz = WebdavServlet.class.getClassLoader().loadClass(
					clazzName);

			Constructor<?> ctor = clazz
					.getConstructor(new Class[]{File.class});

			webdavStore = (IWebdavStore) ctor
					.newInstance(new Object[]{root});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("some problem making store component", e);
		}
		return webdavStore;
	}

	private File getFileRoot() {
		String rootPath = getInitParameter(ROOTPATH_PARAMETER);
		if (rootPath == null) {
			throw new WebdavException("missing parameter: "
					+ ROOTPATH_PARAMETER);
		}
		if (rootPath.startsWith("*WAR-FILE-ROOT*")) {
			String file = LocalFileSystemStore.class.getProtectionDomain()
					.getCodeSource().getLocation().getFile().replace('\\', '/');
			if (file.charAt(0) == '/'
					&& System.getProperty("os.name", "").contains("Windows")) {
				file = file.substring(1, file.length());
			}

			int ix = file.indexOf("/WEB-INF/");
			if (ix != -1) {
				String appendPath = rootPath.substring("*WAR-FILE-ROOT*".length());
				rootPath = file.substring(0, ix);
				if (appendPath.length() > 0) rootPath += appendPath;
				rootPath = rootPath.replace('/', File.separatorChar);
				LOG.debug("ROOT path {}", rootPath);
			} else {
				throw new WebdavException(
						"Could not determine root of war file. Can't extract from path '"
								+ file + "' for this web container");
			}
		}
		return new File(rootPath);
	}

}
