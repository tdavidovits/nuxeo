/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.preview.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerService;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.runtime.api.Framework;

public class PlainTextPreviewer extends AbstractPreviewer implements
        MimeTypePreviewer {

    protected String htmlContent(String content) {
        return "<pre>"
	          + content.replace("&", "&amp;").replace("<", "&lt;").replace(
                        ">", "&gt;").replace("\'", "&apos;").replace("\"",
                        "&quot;").replace("\n", "<br/>") + "</pre>";
    }

    public List<Blob> getPreview(Blob blob, DocumentModel dm)
            throws PreviewException {
        List<Blob> blobResults = new ArrayList<Blob>();

        StringBuilder htmlPage = new StringBuilder();

        htmlPage.append("<html><body>");
        byte[] data;
        try {
            data = blob.getByteArray();
        } catch (IOException e) {
            throw new PreviewException("Cannot fetch blob content", e);
        }

        String content = new String(data);

        HtmlSanitizerService sanitizer = Framework.getService(HtmlSanitizerService.class);
        if (sanitizer == null && !Framework.isTestModeSet()) {
            throw new RuntimeException("Cannot find HtmlSanitizerService");
        }

        htmlPage.append("<?xml version=\"1.0\" encoding=\"UTF-8\"/>");
        htmlPage.append("<html>");
        htmlPage.append("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/></head>");
        htmlPage.append("<body>");
        if (sanitizer != null) {
            htmlPage.append(htmlContent(sanitizer.sanitizeString(content, null)));
        }
        htmlPage.append("</body></html>");

        Blob mainBlob = new StringBlob(htmlPage.toString());
        mainBlob.setFilename("index.html");
        mainBlob.setMimeType("text/html");

        blobResults.add(mainBlob);
        return blobResults;
    }

}
