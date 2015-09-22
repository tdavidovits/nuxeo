/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     btatar
 *
 * $Id: XMLDirectoryWriter.java 30235 2008-02-18 15:35:09Z fguillaume $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentWriter;

/**
 * This class plays a role in the export pipe.It is used to generate xml files that have a nuxeo specific format.Each
 * file contains information about a document model such as,general information like name, uid or document type, and
 * information about the schemas that the document includes.
 *
 * @author btatar
 */
public class XMLDirectoryWriter extends AbstractDocumentWriter {

    private File destination;

    /**
     * Allow to skip the blob from export : useful in case of a Nuxeo 2 Nuxeo migration
     *
     * @since 7.4
     */
    protected boolean skipBlobs = false;

    public XMLDirectoryWriter(String destinationPath) {
        this(new File(destinationPath));
    }

    public XMLDirectoryWriter(File destination) {
        this.destination = destination;
    }

    /**
     * @since 7.4
     */
    public boolean skipBlobs() {
        return skipBlobs;
    }

    /**
     * @since 7.4
     */
    public void setSkipBlobs(boolean skipBlobs) {
        this.skipBlobs = skipBlobs;
    }

    /**
     * Gives the destination where the XML file will be generated.
     */
    public Object getDestination() {
        return destination;
    }

    /**
     * Sets the destination where the XML file will be generated.
     */
    public void setDestination(File destination) {
        this.destination = destination;
    }

    @Override
    public void close() {
        destination = null;
    }

    @Override
    public DocumentTranslationMap write(ExportedDocument doc) throws IOException {

        File file = new File(getDestination() + File.separator + doc.getPath().toString());
        if (!file.mkdirs()) {
            throw new IOException("Cannot create target directory: " + file.getAbsolutePath());
        }
        OutputFormat format = AbstractDocumentWriter.createPrettyPrint();
        XMLWriter writer = null;
        try {
            writer = new XMLWriter(new FileOutputStream(file.getAbsolutePath() + File.separator + "document.xml"),
                    format);
            writer.write(doc.getDocument());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        if (!skipBlobs) {
            Map<String, Blob> blobs = doc.getBlobs();
            for (Map.Entry<String, Blob> entry : blobs.entrySet()) {
                String blobPath = file.getAbsolutePath() + File.separator + entry.getKey();
                entry.getValue().transferTo(new File(blobPath));
            }
        }

        // write external documents
        for (Map.Entry<String, Document> entry : doc.getDocuments().entrySet()) {
            writer = null;
            try {
                writer = new XMLWriter(new FileOutputStream(file.getAbsolutePath() + File.separator + entry.getKey()
                        + ".xml"), format);
                writer.write(entry.getValue());
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }

        return null;
    }

}
