/*
 * Copyright (c) 1996-2013 ApexSoft co.,td.
 * site: http://www.apexsoft.com.cn
 * 版权所有：福建顶点软件股份有限公司
 * 地址：福建省福州市软件大道89号软件园顶点软件中心 350003
 */

package net.sf.webdav.exceptions;

public class AccessDeniedException extends WebdavException {

    public AccessDeniedException() {
        super();
    }

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccessDeniedException(Throwable cause) {
        super(cause);
    }
}
