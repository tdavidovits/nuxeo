/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.admin.runtime;

import org.nuxeo.common.Environment;
import org.nuxeo.connect.packages.dependencies.TargetPlatformFilterHelper;
import org.nuxeo.runtime.api.Framework;

public class PlatformVersionHelper {

    public static final String UNKNOWN = "Unknown";

    public static String getApplicationName() {
        return Framework.getProperty(Environment.PRODUCT_NAME, UNKNOWN);
    }

    public static String getApplicationVersion() {
        return Framework.getProperty(Environment.PRODUCT_VERSION, UNKNOWN);
    }

    public static String getPlatformFilter() {
        if (getDistributionName().equals(UNKNOWN)) {
            return null;
        }
        return getDistributionName() + "-" + getDistributionVersion();
    }

    public static String getDistributionName() {
        return Framework.getProperty(Environment.DISTRIBUTION_NAME, UNKNOWN);
    }

    public static String getDistributionVersion() {
        return Framework.getProperty(Environment.DISTRIBUTION_VERSION, UNKNOWN);
    }

    public static String getDistributionDate() {
        return Framework.getProperty(Environment.DISTRIBUTION_DATE, UNKNOWN);
    }

    public static String getDistributionHost() {
        return Framework.getProperty(Environment.DISTRIBUTION_SERVER, UNKNOWN);
    }

    /**
     * @deprecated Since 6.0. Use {@link TargetPlatformFilterHelper#isCompatibleWithTargetPlatform(String[], String)}
     * @see TargetPlatformFilterHelper
     */
    @Deprecated
    public static boolean isCompatible(final String[] targetPlatforms, String currentPlatform) {
        return TargetPlatformFilterHelper.isCompatibleWithTargetPlatform(targetPlatforms, currentPlatform);
    }

    /**
     * @deprecated Since 6.0. Use {@link TargetPlatformFilterHelper#isCompatibleWithTargetPlatform(String[], String)}
     * @see #getPlatformFilter()
     * @see TargetPlatformFilterHelper
     */
    @Deprecated
    public static boolean isCompatible(String[] targetPlatforms) {
        return isCompatible(targetPlatforms, getPlatformFilter());
    }

}
