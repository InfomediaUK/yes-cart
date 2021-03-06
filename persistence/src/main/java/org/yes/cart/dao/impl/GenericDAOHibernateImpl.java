/*
 * Copyright 2009 Denys Pavlov, Igor Azarnyi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.yes.cart.dao.impl;

import org.hibernate.*;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.RowCountProjection;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yes.cart.dao.CriteriaTuner;
import org.yes.cart.dao.EntityFactory;
import org.yes.cart.dao.GenericDAO;
import org.yes.cart.dao.ResultsIterator;
import org.yes.cart.domain.entity.Identifiable;
import org.yes.cart.domain.entity.Product;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


/**
 * User: Igor Azarny iazarny@yahoo.com
 * Date: 08-May-2011
 * Time: 11:12:54
 */
public class GenericDAOHibernateImpl<T, PK extends Serializable> implements GenericDAO<T, PK> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericDAOHibernateImpl.class);

    private final Class<T> persistentClass;
    private final EntityFactory entityFactory;
    protected SessionFactory sessionFactory;

    /**
     * Set the Hibernate SessionFactory to be used by this DAO.
     * Will automatically create a HibernateTemplate for the given SessionFactory.
     */
    public final void setSessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;

    }

    /**
     * Default constructor.
     *
     * @param type          - entity type
     * @param entityFactory {@link EntityFactory} to create the entities
     */
    @SuppressWarnings("unchecked")
    public GenericDAOHibernateImpl(final Class<T> type, final EntityFactory entityFactory) {
        this.persistentClass = type;
        this.entityFactory = entityFactory;
    }

    /**
     * {@inheritDoc}
     */
    public EntityFactory getEntityFactory() {
        return entityFactory;
    }

    /**
     * {@inheritDoc}
     */
    public <I> I getEntityIdentifier(final Object entity) {
        if (entity == null) {
            // That's ok - it is null
            return null;
        } if (entity instanceof HibernateProxy && !Hibernate.isInitialized(entity)) {
            // Avoid Lazy select by getting identifier from session meta
            // If hibernate proxy is initialised then DO NOT use this approach as chances
            // are that this is detached entity from cache which is not associate with the
            // session and will result in exception
            return (I) sessionFactory.getCurrentSession().getIdentifier(entity);
        } else if (entity instanceof Identifiable) {
            // If it is not proxy or it is initialised then we can use identifiable
            return (I) Long.valueOf(((Identifiable) entity).getId());
        }
        throw new IllegalArgumentException("Cannot get PK from object: " + entity);
    }

    /**
     * {@inheritDoc}
     */
    public T findById(PK id) {
        return findById(id, false);
    }

    private static final LockOptions UPDATE = new LockOptions(LockMode.PESSIMISTIC_WRITE);

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T findById(final PK id, final boolean lock) {
        T entity;
        if (lock) {
            entity = (T) sessionFactory.getCurrentSession().get(getPersistentClass(), id, UPDATE);
        } else {
            entity = (T) sessionFactory.getCurrentSession().get(getPersistentClass(), id);
        }
        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByExample(final T exampleInstance, final String[] excludeProperty) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        Example example = Example.create(exampleInstance);
        for (String exclude : excludeProperty) {
            example.excludeProperty(exclude);
        }
        crit.add(example);
        return crit.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T findSingleByNamedQuery(final String namedQueryName, final Object... parameters) {
        List<T> rez = (List<T>) this.findByNamedQuery(namedQueryName, parameters);
        if (!rez.isEmpty()) {
            return rez.get(0);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T findSingleByNamedQueryCached(final String namedQueryName, final Object... parameters) {
        List<T> rez = (List<T>) this.findByNamedQueryCached(namedQueryName, parameters);
        if (!rez.isEmpty()) {
            return rez.get(0);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Object getScalarResultByNamedQuery(final String namedQueryName, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        setQueryParameters(query, parameters);
        return query.uniqueResult();
    }

    /**
     * {@inheritDoc}
     */
    public Object getScalarResultByNamedQueryWithInit(final String namedQueryName,  final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        setQueryParameters(query, parameters);
        final Object obj = query.uniqueResult();
        if (obj instanceof Product) {
            Hibernate.initialize(((Product) obj).getAttributes());

        }
        return obj;
    }

    /**
     * {@inheritDoc}
     */
    public List<Object> findByQuery(final String hsqlQuery, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().createQuery(hsqlQuery);
        setQueryParameters(query, parameters);
        return query.list();
    }


    /**
     * {@inheritDoc}
     */
    public ResultsIterator<Object> findByQueryIterator(final String hsqlQuery, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().createQuery(hsqlQuery);
        if (parameters != null) {
            setQueryParameters(query, parameters);
        }
        return new ResultsIteratorImpl<Object>(query.scroll(ScrollMode.FORWARD_ONLY));
    }

    /**
     * {@inheritDoc}
     */
    public Object findSingleByQuery(final String hsqlQuery, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().createQuery(hsqlQuery);
        setQueryParameters(query, parameters);
        final List rez = query.list();
        int size = rez.size();
        switch (size) {
            case 0: {
                return null;
            }
            case 1: {
                return rez.get(0);
            }
            default: {
                LOG.error("#findSingleByQuery has more than one result for {}, [{}]", hsqlQuery, parameters);
                return rez.get(0);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByNamedQuery(final String namedQueryName, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        if (parameters != null) {
            setQueryParameters(query, parameters);
        }
        return query.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public ResultsIterator<T> findByNamedQueryIterator(final String namedQueryName, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        if (parameters != null) {
            setQueryParameters(query, parameters);
        }
        return new ResultsIteratorImpl<T>(query.scroll(ScrollMode.FORWARD_ONLY));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByNamedQueryForUpdate(final String namedQueryName, final int timeout, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        LockOptions opts = new LockOptions(LockMode.PESSIMISTIC_WRITE);
        opts.setTimeOut(timeout);
        query.setLockOptions(opts);
        if (parameters != null) {
            setQueryParameters(query, parameters);
        }
        return query.list();
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByNamedQueryCached(final String namedQueryName, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        setQueryParameters(query, parameters);
        return query.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<Object> findQueryObjectByNamedQuery(final String namedQueryName, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        setQueryParameters(query, parameters);
        return query.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public ResultsIterator<Object> findQueryObjectByNamedQueryIterator(final String namedQueryName, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        setQueryParameters(query, parameters);
        final ScrollableResults results = query.scroll(ScrollMode.FORWARD_ONLY);
        return new ResultsIteratorImpl<Object>(results);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<Object> findQueryObjectRangeByNamedQuery(final String namedQueryName, final int firstResult, final int maxResults, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        setQueryParameters(query, parameters);
        query.setFirstResult(firstResult);
        query.setMaxResults(maxResults);
        return query.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findQueryObjectsByNamedQuery(final String namedQueryName, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        setQueryParameters(query, parameters);
        return query.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findQueryObjectsRangeByNamedQuery(final String namedQueryName, final int firstResult, final int maxResults, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        setQueryParameters(query, parameters);
        query.setFirstResult(firstResult);
        query.setMaxResults(maxResults);
        return query.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findRangeByNamedQuery(final String namedQueryName,
                                         final int firstResult,
                                         final int maxResults,
                                         final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        query.setFirstResult(firstResult);
        query.setMaxResults(maxResults);
        setQueryParameters(query, parameters);
        return query.list();
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findAll() {
        return findByCriteria();
    }

    /**
     * {@inheritDoc}
     */
    public ResultsIterator<T> findAllIterator() {
        final Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        final ScrollableResults results = crit.scroll(ScrollMode.FORWARD_ONLY);
        return new ResultsIteratorImpl<T>(results);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T saveOrUpdate(T entity) {
        sessionFactory.getCurrentSession().saveOrUpdate(entity);
        return entity;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T create(T entity) {
        sessionFactory.getCurrentSession().save(entity);
        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T update(T entity) {
        sessionFactory.getCurrentSession().update(entity);
        return entity;
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final Object entity) {
        if (entity != null) {
            sessionFactory.getCurrentSession().delete(entity);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void refresh(final Object entity) {
        sessionFactory.getCurrentSession().refresh(entity);
    }

    /**
     * {@inheritDoc}
     */
    public void evict(Object entity) {
        sessionFactory.getCurrentSession().evict(entity);

    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByCriteria(Criterion... criterion) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        return crit.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByCriteria(final int firstResult, final int maxResults, final Criterion... criterion) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        crit.setFirstResult(firstResult);
        crit.setMaxResults(maxResults);
        return crit.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByCriteria(final int firstResult, final int maxResults, final Criterion[] criterion, final Order[] order) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        for (Order o : order) {
            crit.addOrder(o);
        }
        crit.setFirstResult(firstResult);
        crit.setMaxResults(maxResults);
        return crit.list();
    }

    @Override
    public int findCountByCriteria(final Criterion... criterion) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        crit.setProjection(new RowCountProjection());
        return ((Number) crit.uniqueResult()).intValue();
    }

    /**
     * {@inheritDoc}
     */
    public T findSingleByCriteria(final Criterion... criterion) {
        return findSingleByCriteria(null, criterion);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByCriteria(final CriteriaTuner criteriaTuner, final Criterion... criterion) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        if (criteriaTuner != null) {
            criteriaTuner.tune(crit);
        }
        return crit.list();

    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByCriteria(final CriteriaTuner criteriaTuner, final int firstResult, final int maxResults, final Criterion... criterion) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        if (criteriaTuner != null) {
            criteriaTuner.tune(crit);
        }
        crit.setFirstResult(firstResult);
        crit.setMaxResults(maxResults);
        return crit.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> findByCriteria(final CriteriaTuner criteriaTuner, final int firstResult, final int maxResults, final Criterion[] criterion, final Order[] order) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        for (Order o : order) {
            crit.addOrder(o);
        }
        if (criteriaTuner != null) {
            criteriaTuner.tune(crit);
        }
        crit.setFirstResult(firstResult);
        crit.setMaxResults(maxResults);
        return crit.list();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public int findCountByCriteria(final CriteriaTuner criteriaTuner, final Criterion... criterion) {

        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        if (criteriaTuner != null) {
            criteriaTuner.tune(crit);
        }
        crit.setProjection(new RowCountProjection());
        return ((Number) crit.uniqueResult()).intValue();
    }

    /**
     * Find entities by criteria.
     * @param firstResult scroll to first result.
     * @param criterion given criteria
     * @return list of found entities.
     */
    @SuppressWarnings("unchecked")
    public T findUniqueByCriteria(final int firstResult, final Criterion... criterion) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        return (T)  crit.setFirstResult(firstResult).setMaxResults(1).uniqueResult();

    }


    /**
     * {@inheritDoc}
     */
    public T findSingleByCriteria(final CriteriaTuner criteriaTuner, final Criterion... criterion) {
        Criteria crit = sessionFactory.getCurrentSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        if (criteriaTuner != null) {
            criteriaTuner.tune(crit);
        }
        return (T) crit.uniqueResult();
    }


    private Class<T> getPersistentClass() {
        return persistentClass;
    }

    /**
     * {@inheritDoc}
     */
    public int executeNativeUpdate(final String nativeQuery) {
        SQLQuery sqlQuery = sessionFactory.getCurrentSession().createSQLQuery(nativeQuery);
        return sqlQuery.executeUpdate();
    }

    /**
     * {@inheritDoc}
     */
    public List executeNativeQuery(final String nativeQuery) {
        SQLQuery sqlQuery = sessionFactory.getCurrentSession().createSQLQuery(nativeQuery);
        return sqlQuery.list();
    }

    /**
     * {@inheritDoc}
     */
    public List executeHsqlQuery(final String hsql) {
        Query query = sessionFactory.getCurrentSession().createQuery(hsql);
        return query.list();
    }


    /**
     * {@inheritDoc}
     */
    public int executeHsqlUpdate(final String hsql, final Object... parameters) {
        Query query = sessionFactory.getCurrentSession().createQuery(hsql);
        setQueryParameters(query, parameters);
        return query.executeUpdate();
    }


    /**
     * {@inheritDoc}
     */
    public int executeNativeUpdate(final String nativeQuery, final Object... parameters) {
        SQLQuery sqlQuery = sessionFactory.getCurrentSession().createSQLQuery(nativeQuery);
        setQueryParameters(sqlQuery, parameters);
        return sqlQuery.executeUpdate();
    }


    /**
     * {@inheritDoc}
     */
    public int executeUpdate(final String namedQueryName, final Object... parameters) {
        final Query query = sessionFactory.getCurrentSession().getNamedQuery(namedQueryName);
        setQueryParameters(query, parameters);
        return query.executeUpdate();
    }

    private void setQueryParameters(final Query query, final Object[] parameters) {
        if (parameters != null) {
            int idx = 1;
            for (Object param : parameters) {
                if (param instanceof Collection) {
                    query.setParameterList(String.valueOf(idx), (Collection) param);
                } else {
                    query.setParameter(String.valueOf(idx), param);
                }
                idx++;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flushClear() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() {
        sessionFactory.getCurrentSession().flush();
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        sessionFactory.getCurrentSession().clear();
    }

}
