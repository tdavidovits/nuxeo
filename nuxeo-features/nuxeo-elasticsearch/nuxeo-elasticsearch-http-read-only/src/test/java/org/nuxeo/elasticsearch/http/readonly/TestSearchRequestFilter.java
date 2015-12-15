package org.nuxeo.elasticsearch.http.readonly;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.elasticsearch.http.readonly.filter.DefaultSearchRequestFilter;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestSearchRequestFilter {

    private static final String INDICES = "nxutest";

    private static final String TYPES = "doc";

    @Test
    public void testMatchAll() throws Exception {
        String payload = "{\"query\": {\"match_all\": {}}}";
        DefaultSearchRequestFilter filter = new DefaultSearchRequestFilter();
        filter.init(getNonAdminCoreSession(), INDICES, TYPES, "pretty", payload);
        Assert.assertEquals("/nxutest/doc/_search?pretty", filter.getUrl());
        Assert.assertEquals(
                "{\"query\":{\"filtered\":{\"filter\":{\"terms\":{\"ecm:acl\":[\"group1\",\"group2\",\"members\",\"jdoe\",\"Everyone\"]}},\"query\":{\"match_all\":{}}}}}",
                filter.getPayload());
    }

    @Test
    public void testMatchAllAsAdmin() throws Exception {
        String payload = "{\"query\": {\"match_all\": {}}}";
        DefaultSearchRequestFilter filter = new DefaultSearchRequestFilter();
        filter.init(getAdminCoreSession(), INDICES, TYPES, "pretty", payload);
        Assert.assertEquals("/nxutest/doc/_search?pretty", filter.getUrl());
        Assert.assertEquals(payload, filter.getPayload());
    }

    @Test
    public void testUriSearch() throws Exception {
        DefaultSearchRequestFilter filter = new DefaultSearchRequestFilter();
        filter.init(getNonAdminCoreSession(), INDICES, TYPES,
                "size=2&q=dc%5C%3Atitle:Workspaces", null);
        Assert.assertEquals(filter.getUrl(), "/nxutest/doc/_search?size=2");
        Assert.assertEquals(
                filter.getPayload(),
                "{\"query\":{\"filtered\":{\"filter\":{\"terms\":{\"ecm:acl\":[\"group1\",\"group2\",\"members\",\"jdoe\",\"Everyone\"]}},\"query\":{\"query_string\":{\"default_operator\":\"OR\",\"query\":\"dc\\:title:Workspaces\",\"default_field\":\"_all\"}}}}}");
    }

    @Test
    public void testUriSearchWithDefaultFieldAndOperator() throws Exception {
        DefaultSearchRequestFilter filter = new DefaultSearchRequestFilter();
        filter.init(getNonAdminCoreSession(), INDICES, TYPES,
                "q=dc\\:title:Workspaces&pretty&df=dc:title&default_operator=AND", null);
        Assert.assertEquals(filter.getUrl(), "/nxutest/doc/_search?pretty");
        Assert.assertEquals(
                filter.getPayload(),
                "{\"query\":{\"filtered\":{\"filter\":{\"terms\":{\"ecm:acl\":[\"group1\",\"group2\",\"members\",\"jdoe\",\"Everyone\"]}},\"query\":{\"query_string\":{\"default_operator\":\"AND\",\"query\":\"dc\\:title:Workspaces\",\"default_field\":\"dc:title\"}}}}}");
    }

    @Test
    public void testUriSearchAsAdmin() throws Exception {
        DefaultSearchRequestFilter filter = new DefaultSearchRequestFilter();
        filter.init(getAdminCoreSession(), INDICES, TYPES,
                "size=2&q=dc\\:title:Workspaces", null);
        Assert.assertEquals("/nxutest/doc/_search?size=2&q=dc\\:title:Workspaces", filter.getUrl());
        Assert.assertEquals(null, filter.getPayload());
    }

    /**
     * @since 7.4
     */
    public static CoreSession getAdminCoreSession() {
        return new LocalSession() {
            @Override
            public NuxeoPrincipal getPrincipal() {
                return getAdminPrincipal();
            }
        };
    }

    /**
     * @since 7.4
     */
    public static CoreSession getNonAdminCoreSession() {
        return new LocalSession() {
            @Override
            public NuxeoPrincipal getPrincipal() {
                return getNonAdminPrincipal();
            }
        };
    }

    public static NuxeoPrincipal getAdminPrincipal() {
        return new NuxeoPrincipal() {
            @Override
            public String getFirstName() {
                return "John";
            }

            @Override
            public String getLastName() {
                return "Doe";
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public String getCompany() {
                return null;
            }

            @Override
            public String getEmail() {
                return null;
            }

            @Override
            public List<String> getGroups() {
                return null;
            }

            @Override
            public List<String> getAllGroups() {
                return Arrays.asList("group1", "group2", "members");
            }

            @Override
            public boolean isMemberOf(String s) {
                return false;
            }

            @Override
            public List<String> getRoles() {
                return null;
            }

            @Override
            public void setName(String s) {

            }

            @Override
            public void setFirstName(String s) {

            }

            @Override
            public void setLastName(String s) {

            }

            @Override
            public void setGroups(List<String> list) {

            }

            @Override
            public void setRoles(List<String> list) {

            }

            @Override
            public void setCompany(String s) {

            }

            @Override
            public void setPassword(String s) {

            }

            @Override
            public void setEmail(String s) {

            }

            @Override
            public String getPrincipalId() {
                return null;
            }

            @Override
            public void setPrincipalId(String s) {

            }

            @Override
            public DocumentModel getModel() {
                return null;
            }

            @Override
            public void setModel(DocumentModel documentModel) {

            }

            @Override
            public boolean isAdministrator() {
                return true;
            }

            @Override
            public String getTenantId() {
                return null;
            }

            @Override
            public boolean isAnonymous() {
                return false;
            }

            @Override
            public String getOriginatingUser() {
                return null;
            }

            @Override
            public void setOriginatingUser(String s) {

            }

            @Override
            public String getActingUser() {
                return null;
            }

            @Override
            public boolean isTransient() {
                return false;
            }

            @Override
            public String getName() {
                return "admin";
            }
        };
    }

    public static NuxeoPrincipal getNonAdminPrincipal() {
        return new NuxeoPrincipal() {
            @Override
            public String getFirstName() {
                return "John";
            }

            @Override
            public String getLastName() {
                return "Doe";
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public String getCompany() {
                return null;
            }

            @Override
            public String getEmail() {
                return null;
            }

            @Override
            public List<String> getGroups() {
                return null;
            }

            @Override
            public List<String> getAllGroups() {
                return Arrays.asList("group1", "group2", "members");
            }

            @Override
            public boolean isMemberOf(String s) {
                return false;
            }

            @Override
            public List<String> getRoles() {
                return null;
            }

            @Override
            public void setName(String s) {

            }

            @Override
            public void setFirstName(String s) {

            }

            @Override
            public void setLastName(String s) {

            }

            @Override
            public void setGroups(List<String> list) {

            }

            @Override
            public void setRoles(List<String> list) {

            }

            @Override
            public void setCompany(String s) {

            }

            @Override
            public void setPassword(String s) {

            }

            @Override
            public void setEmail(String s) {

            }

            @Override
            public String getPrincipalId() {
                return null;
            }

            @Override
            public void setPrincipalId(String s) {

            }

            @Override
            public DocumentModel getModel() {
                return null;
            }

            @Override
            public void setModel(DocumentModel documentModel) {

            }

            @Override
            public boolean isAdministrator() {
                return false;
            }

            @Override
            public String getTenantId() {
                return null;
            }

            @Override
            public boolean isAnonymous() {
                return false;
            }

            @Override
            public String getOriginatingUser() {
                return null;
            }

            @Override
            public void setOriginatingUser(String s) {

            }

            @Override
            public String getActingUser() {
                return null;
            }

            @Override
            public boolean isTransient() {
                return false;
            }

            @Override
            public String getName() {
                return "jdoe";
            }
        };
    }

}
