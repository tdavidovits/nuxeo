/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.query.QueryFilter;

/**
 * A {@link Mapper} that uses a {@link UnifiedCachingRowMapper} for row-related operation, and delegates to the
 * {@link Mapper} for others.
 */
public class UnifiedCachingMapper extends UnifiedCachingRowMapper implements CachingMapper {

    /**
     * The {@link Mapper} to which operations are delegated.
     */
    public Mapper mapper;

    @Override
    public void initialize(String repositoryName, Model model, Mapper mapper,
            InvalidationsPropagator invalidationsPropagator, Map<String, String> properties) {
        super.initialize(repositoryName, model, mapper, invalidationsPropagator, properties);
        this.mapper = mapper;
    }

    @Override
    public Identification getIdentification() {
        return mapper.getIdentification();
    }

    @Override
    public void close() {
        super.close();
        mapper.close();
    }

    @Override
    public int getTableSize(String tableName) {
        return mapper.getTableSize(tableName);
    }

    @Override
    public void createDatabase(String ddlMode) {
        mapper.createDatabase(ddlMode);
    }

    @Override
    public Serializable getRootId(String repositoryId) {
        return mapper.getRootId(repositoryId);
    }

    @Override
    public void setRootId(Serializable repositoryId, Serializable id) {
        mapper.setRootId(repositoryId, id);
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter,
            boolean countTotal) {
        return mapper.query(query, queryType, queryFilter, countTotal);
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter, long countUpTo) {
        return mapper.query(query, queryType, queryFilter, countUpTo);
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            Object... params) {
        return mapper.queryAndFetch(query, queryType, queryFilter, params);
    }

    @Override
    public Set<Serializable> getAncestorsIds(Collection<Serializable> ids) {
        return mapper.getAncestorsIds(ids);
    }

    @Override
    public void updateReadAcls() {
        mapper.updateReadAcls();
    }

    @Override
    public void rebuildReadAcls() {
        mapper.rebuildReadAcls();
    }

    @Override
    public int getClusterNodeIdType() {
        return mapper.getClusterNodeIdType();
    }

    @Override
    public void createClusterNode(Serializable nodeId) {
        mapper.createClusterNode(nodeId);
    }

    @Override
    public void removeClusterNode(Serializable nodeId) {
        mapper.removeClusterNode(nodeId);
    }

    @Override
    public void insertClusterInvalidations(Serializable nodeId, Invalidations invalidations) {
        mapper.insertClusterInvalidations(nodeId, invalidations);
    }

    @Override
    public Invalidations getClusterInvalidations(Serializable nodeId) {
        return mapper.getClusterInvalidations(nodeId);
    }

    @Override
    public Lock getLock(Serializable id) {
        return mapper.getLock(id);
    }

    @Override
    public Lock setLock(Serializable id, Lock lock) {
        return mapper.setLock(id, lock);
    }

    @Override
    public Lock removeLock(Serializable id, String owner, boolean force) {
        return mapper.removeLock(id, owner, force);
    }

    @Override
    public void markReferencedBinaries() {
        mapper.markReferencedBinaries();
    }

    @Override
    public int cleanupDeletedRows(int max, Calendar beforeTime) {
        return mapper.cleanupDeletedRows(max, beforeTime);
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        mapper.start(xid, flags);
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        mapper.end(xid, flags);

    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return mapper.prepare(xid);
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        mapper.commit(xid, onePhase);
    }

    // rollback interacts with caches so is in RowMapper

    @Override
    public void forget(Xid xid) throws XAException {
        mapper.forget(xid);
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return mapper.recover(flag);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return mapper.setTransactionTimeout(seconds);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return mapper.getTransactionTimeout();
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        return mapper.isSameRM(xares);
    }

    @Override
    public boolean isConnected() {
        return mapper.isConnected();
    }

    @Override
    public void connect() {
        mapper.connect();
    }

    @Override
    public void disconnect() {
        mapper.disconnect();
    }
}
