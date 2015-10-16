/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.jsf.listener;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.jsf.PageResourceRenderer;
import org.nuxeo.ecm.web.resources.jsf.ResourceBundleRenderer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Moves CSS files to the start of the head tag and reorders js resources for better page rendering.
 *
 * @since 7.10
 */
public class ResourceOrderingListener implements SystemEventListener {

    private static final Log log = LogFactory.getLog(ResourceOrderingListener.class);

    protected static String TARGET_HEAD = "head";

    protected static String SLOT_HEAD_START = "headstart";

    protected static String SLOT_BODY_START = "bodystart";

    protected static String SLOT_BODY_END = "bodyend";

    protected static String DEFER_JS_PROP = "nuxeo.jsf.deferJavaScriptLoading";

    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException {
        UIViewRoot root = (UIViewRoot) event.getSource();
        FacesContext ctx = FacesContext.getCurrentInstance();
        List<UIComponent> cssResources = new ArrayList<UIComponent>();
        List<UIComponent> otherResources = new ArrayList<UIComponent>();
        List<UIComponent> resources = root.getComponentResources(ctx, TARGET_HEAD);
        for (UIComponent r : resources) {
            if (isCssResource(ctx, r)) {
                cssResources.add(r);
            } else {
                otherResources.add(r);
            }
        }
        // add CSS resources back to the head first slot
        for (UIComponent r : cssResources) {
            root.removeComponentResource(ctx, r, TARGET_HEAD);
            root.addComponentResource(ctx, r, SLOT_HEAD_START);
            if (log.isDebugEnabled()) {
                String name = (String) r.getAttributes().get("name");
                if (name == null) {
                    log.debug(String.format("Pushing %s at the beggining of head tag", r));
                } else {
                    log.debug(String.format("Pushing %s at the beggining of head tag", name));
                }
            }
        }
        boolean deferJS = deferJsLoading();
        if (deferJS) {
            // add other resources to the body start
            for (UIComponent r : otherResources) {
                root.removeComponentResource(ctx, r, TARGET_HEAD);
                root.addComponentResource(ctx, r, SLOT_BODY_START);
                if (log.isDebugEnabled()) {
                    String name = (String) r.getAttributes().get("name");
                    if (name == null) {
                        log.debug(String.format("Pushing %s at the end of body tag", r));
                    } else {
                        log.debug(String.format("Pushing %s at the end of body tag", name));
                    }
                }
            }
        }
    }

    protected String getName(UIComponent r) {
        return (String) r.getAttributes().get("name");
    }

    protected boolean isCssResource(FacesContext ctx, UIComponent r) {
        String rtype = r.getRendererType();
        if ("javax.faces.resource.Stylesheet".equals(rtype)) {
            return true;
        }
        if (ResourceBundleRenderer.RENDERER_TYPE.equals(rtype) || PageResourceRenderer.RENDERER_TYPE.equals(rtype)) {
            String type = (String) r.getAttributes().get("type");
            if (ResourceType.css.equals(type) || ResourceType.jsfcss.equals(type)) {
                return true;
            }
            return false;
        }
        String name = (String) r.getAttributes().get("name");
        if (name == null) {
            return false;
        }
        name = name.toLowerCase();
        if (name.contains(".css") || name.contains(".ecss")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isListenerForSource(Object source) {
        return (source instanceof UIViewRoot);
    }

    protected boolean deferJsLoading() {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        return cs.isBooleanPropertyTrue(DEFER_JS_PROP);
    }

}
