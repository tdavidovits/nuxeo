/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PropertyException extends ClientException {

    private static final long serialVersionUID = 4457374575384629415L;

    public PropertyException(String message) {
        super(message);
    }

    public PropertyException(String message, Throwable cause) {
        super(message, cause);
    }

}