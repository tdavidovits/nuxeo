/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.model;

/**
 * Interface to manage a low-level repository.
 */
public interface Repository {

    String getName();

    Session getSession();

    void shutdown();

    int getActiveSessionsCount();

    /**
     * Marks the binaries in use by passing them to the binary manager(s)'s GC mark() method.
     *
     * @since 7.4
     */
    void markReferencedBinaries();

}
