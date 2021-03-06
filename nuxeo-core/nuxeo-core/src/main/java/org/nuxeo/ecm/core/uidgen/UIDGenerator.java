/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Dragos Mihalache
 */
package org.nuxeo.ecm.core.uidgen;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;

/**
 * Common interface for UID generators. All UID generators must implement this interface.
 *
 * @author <a href="mailto:dm@nuxeo.com>Dragos Mihalache</a>
 */
public interface UIDGenerator {

    /**
     * Sets the property name used to set the identifier value.
     * <p>
     * The property must be a string like 'schemaPrefix:fieldName' ; the syntax 'schemaName:fieldName' is also accepted.
     * Could be used as a convenient method when there is only one property to set.
     *
     * @param propertyName
     * @see #setPropertyNames(String[])
     */
    void setPropertyName(String propertyName);

    /**
     * Get the property name used to set the identifier value.
     *
     * @see #getPropertyNames()
     */
    String getPropertyName();

    /**
     * Set the properties used to set the identifier value.
     *
     * @param propertyNames
     */
    void setPropertyNames(String[] propertyNames);

    /**
     * Gets the property name used to set the identifier value
     *
     * @return
     */
    String[] getPropertyNames();

    /**
     * The sequencer used to generate unique numbers sequencially.
     *
     * @param sequencer
     */
    void setSequencer(UIDSequencer sequencer);

    String getSequenceKey(DocumentModel document);

    /**
     * Returns a new UID for the given doc.
     */
    String createUID(DocumentModel document);

    /**
     * Creates a new UID for the given doc and sets the field configured in the generator component with this value.
     */
    void setUID(DocumentModel document) throws PropertyNotFoundException;

}
