/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.preview.restlet;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.NothingToPreviewException;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * Provides a REST API to retrieve the preview of a document.
 *
 * @author tiry
 */
@Name("previewRestlet")
@Scope(ScopeType.EVENT)
public class PreviewRestlet extends BaseNuxeoRestlet {

    private static final Log log = LogFactory.getLog(PreviewRestlet.class);

    @In(create = true)
    protected NavigationContext navigationContext;

    protected CoreSession documentManager;

    protected DocumentModel targetDocument;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    // cache duration in seconds
    // protected static int MAX_CACHE_LIFE = 60 * 10;

    // protected static final Map<String, PreviewCacheEntry> cachedAdapters =
    // new ConcurrentHashMap<String, PreviewCacheEntry>();

    protected static final List<String> previewInProcessing = Collections.synchronizedList(new ArrayList<String>());

    @Override
    public void handle(Request req, Response res) {
        HttpServletRequest request = getHttpRequest(req);
        HttpServletResponse response = getHttpResponse(res);

        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String xpath = (String) req.getAttributes().get("fieldPath");
        xpath = xpath.replace("-", "/");
        List<String> segments = req.getResourceRef().getSegments();
        StringBuilder sb = new StringBuilder();
        for (int i = 6; i < segments.size(); i++) {
            sb.append(segments.get(i));
            sb.append("/");
        }
        String subPath = sb.substring(0, sb.length() - 1);

        try {
            xpath = URLDecoder.decode(xpath, "UTF-8");
            subPath = URLDecoder.decode(subPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }

        String blobPostProcessingParameter = getQueryParamValue(req, "blobPostProcessing", "false");
        boolean blobPostProcessing = Boolean.parseBoolean(blobPostProcessingParameter);

        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }
        if (docid == null || repo.equals("*")) {
            handleError(res, "you must specify a documentId");
            return;
        }
        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            targetDocument = documentManager.getDocument(new IdRef(docid));
        } catch (DocumentNotFoundException e) {
            handleError(res, e);
            return;
        }

        // if it's a managed blob try to use the embed uri for preview
        Blob blobToPreview = getBlobToPreview(xpath);
        BlobManager blobManager = Framework.getService(BlobManager.class);
        try {
            URI uri = blobManager.getURI(blobToPreview, UsageHint.EMBED, null);
            if (uri != null) {
                res.redirectSeeOther(uri.toString());
                return;
            }
        } catch (IOException e) {
            handleError(res, e);
            return;
        }

        localeSetup(req);

        List<Blob> previewBlobs = initCachedBlob(res, xpath, blobPostProcessing);
        if (previewBlobs == null || previewBlobs.isEmpty()) {
            // response was already handled by initCachedBlob
            return;
        }

        // find blob
        Blob blob = null;
        if (StringUtils.isEmpty(subPath)) {
            blob = previewBlobs.get(0);
            blob.setMimeType("text/html");
        } else {
            for (Blob b : previewBlobs) {
                if (subPath.equals(b.getFilename())) {
                    blob = b;
                    break;
                }
            }
        }
        if (blob == null) {
            res.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return;
        }

        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");

        String reason = "preview";
        final Blob fblob = blob;
        if (xpath == null || "default".equals(xpath)) {
            xpath = DownloadService.BLOBHOLDER_0;
        }
        Boolean inline = Boolean.TRUE;
        Map<String, Serializable> extendedInfos = Collections.singletonMap("subPath", subPath);
        DownloadService downloadService = Framework.getService(DownloadService.class);
        try {
            downloadService.downloadBlob(request, response, targetDocument, xpath, blob, blob.getFilename(), reason,
                    extendedInfos, inline, byteRange -> setEntityToBlobOutput(fblob, byteRange, res));
        } catch (IOException e) {
            handleError(res, e);
        }
    }

    /**
     * @since 7.3
     */
    private Blob getBlobToPreview(String xpath) {
        BlobHolder bh;
        if ((xpath == null) || ("default".equals(xpath))) {
            bh = targetDocument.getAdapter(BlobHolder.class);
        } else {
            bh = new DocumentBlobHolder(targetDocument, xpath);
        }
        return bh.getBlob();
    }

    /**
     * @since 5.7
     */
    private void localeSetup(Request req) {
        // Forward locale from HttpRequest to Seam context if not set into DM
        Locale locale = Framework.getService(LocaleProvider.class).getLocale(documentManager);
        if (locale == null) {
            locale = getHttpRequest(req).getLocale();
        }
        localeSelector.setLocale(locale);
    }

    private List<Blob> initCachedBlob(Response res, String xpath, boolean blobPostProcessing) {

        HtmlPreviewAdapter preview = null; // getFromCache(targetDocument,
                                           // xpath);

        // if (preview == null) {
        preview = targetDocument.getAdapter(HtmlPreviewAdapter.class);
        // }

        if (preview == null) {
            handleNoPreview(res, xpath, null);
            return null;
        }

        List<Blob> previewBlobs = null;
        try {
            if (xpath.equals(PreviewHelper.PREVIEWURL_DEFAULTXPATH)) {
                previewBlobs = preview.getFilePreviewBlobs(blobPostProcessing);
            } else {
                previewBlobs = preview.getFilePreviewBlobs(xpath, blobPostProcessing);
            }
            /*
             * if (preview.cachable()) { updateCache(targetDocument, preview, xpath); }
             */
        } catch (PreviewException e) {
            previewInProcessing.remove(targetDocument.getId());
            handleNoPreview(res, xpath, e);
            return null;
        }

        if (previewBlobs == null || previewBlobs.size() == 0) {
            handleNoPreview(res, xpath, null);
            return null;
        }
        return previewBlobs;
    }

    protected void handleNoPreview(Response res, String xpath, Exception e) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body><center><h1>");
        if (e == null) {
            sb.append(resourcesAccessor.getMessages().get("label.not.available.preview") + "</h1>");
        } else {
            sb.append(resourcesAccessor.getMessages().get("label.cannot.generated.preview") + "</h1>");
            sb.append("<pre>Technical issue:</pre>");
            sb.append("<pre>Blob path: ");
            sb.append(xpath);
            sb.append("</pre>");
            sb.append("<pre>");
            sb.append(e.toString());
            sb.append("</pre>");
        }

        sb.append("</center></body></html>");
        if (e instanceof NothingToPreviewException) {
            // Not an error, don't log
        } else {
            log.error("Could not build preview for missing blob at " + xpath, e);
        }

        res.setEntity(sb.toString(), MediaType.TEXT_HTML);
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
    }

}
