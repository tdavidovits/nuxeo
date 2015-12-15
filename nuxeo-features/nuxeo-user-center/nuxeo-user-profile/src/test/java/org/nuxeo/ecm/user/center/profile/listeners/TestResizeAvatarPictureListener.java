/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.user.center.profile.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_AVATAR_FIELD;
import static org.nuxeo.ecm.user.center.profile.listeners.ResizeAvatarPictureListener.RESIZED_IMAGE_HEIGHT;
import static org.nuxeo.ecm.user.center.profile.listeners.ResizeAvatarPictureListener.RESIZED_IMAGE_WIDTH;

import java.io.Serializable;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.user.center.profile.UserProfileConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = DefaultRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core", "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.user.center.profile" })
public class TestResizeAvatarPictureListener {

    @Inject
    CoreSession session;

    @Inject
    UserWorkspaceService userWorkspaceService;

    ResizeAvatarPictureListener underTest;

    @Test
    public void testResizeAvatar() throws Exception {
        DocumentModel userWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(
                session, null);
        userWorkspace.addFacet(UserProfileConstants.USER_PROFILE_FACET);

        ImagingService imagingService = Framework.getService(ImagingService.class);
        assertNotNull(imagingService);

        underTest = new ResizeAvatarPictureListener();

        Blob tooBigAvatar = lookForAvatarBlob("data/BigAvatar.jpg");
        assertNotNull(tooBigAvatar);

        underTest.resizeAvatar(userWorkspace, tooBigAvatar);
        Blob resizedImage = (Blob) userWorkspace.getPropertyValue(USER_PROFILE_AVATAR_FIELD);

        assertNotNull(resizedImage);
        assertFalse(tooBigAvatar.equals(resizedImage));

        ImageInfo imageInfo = imagingService.getImageInfo(resizedImage);
        assertTrue(imageInfo.getWidth() < RESIZED_IMAGE_WIDTH);
        assertTrue(imageInfo.getHeight() == RESIZED_IMAGE_HEIGHT);

        Blob limitSizeAvatar = lookForAvatarBlob("data/MediumAvatar.jpg");
        assertNotNull(tooBigAvatar);

        userWorkspace.setPropertyValue(USER_PROFILE_AVATAR_FIELD,
                (Serializable) limitSizeAvatar);

        underTest.resizeAvatar(userWorkspace, limitSizeAvatar);
        resizedImage = (Blob) userWorkspace.getPropertyValue(USER_PROFILE_AVATAR_FIELD);

        assertNotNull(resizedImage);
        assertEquals(limitSizeAvatar, resizedImage);

        imageInfo = imagingService.getImageInfo(resizedImage);

        assertTrue(imageInfo.getWidth() == RESIZED_IMAGE_WIDTH);
        assertTrue(imageInfo.getHeight() == RESIZED_IMAGE_HEIGHT);

        Blob underLimitSizeAvatar = lookForAvatarBlob("data/SmallAvatar.jpg");
        assertNotNull(tooBigAvatar);

        userWorkspace.setPropertyValue(USER_PROFILE_AVATAR_FIELD,
                (Serializable) underLimitSizeAvatar);

        underTest.resizeAvatar(userWorkspace, underLimitSizeAvatar);
        resizedImage = (Blob) userWorkspace.getPropertyValue(USER_PROFILE_AVATAR_FIELD);

        imageInfo = imagingService.getImageInfo(resizedImage);
        assertTrue(imageInfo.getWidth() < RESIZED_IMAGE_WIDTH);
        assertTrue(imageInfo.getHeight() < RESIZED_IMAGE_HEIGHT);

        assertNotNull(resizedImage);
        assertEquals(underLimitSizeAvatar, resizedImage);

    }

    protected Blob lookForAvatarBlob(String avatarImagePath) {
        URL avatarURL = this.getClass().getClassLoader().getResource(
                avatarImagePath);
        Blob originalImage = new URLBlob(avatarURL);
        return originalImage;
    }

}
