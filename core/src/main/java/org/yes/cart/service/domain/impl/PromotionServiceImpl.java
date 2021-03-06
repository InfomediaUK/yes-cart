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

package org.yes.cart.service.domain.impl;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.yes.cart.dao.GenericDAO;
import org.yes.cart.domain.entity.Promotion;
import org.yes.cart.service.domain.PromotionService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: denispavlov
 * Date: 13-10-19
 * Time: 11:08 PM
 */
public class PromotionServiceImpl extends BaseGenericServiceImpl<Promotion> implements PromotionService {

    public PromotionServiceImpl(final GenericDAO<Promotion, Long> genericDao) {
        super(genericDao);
    }

    /** {@inheritDoc} */
    public List<Promotion> getPromotionsByShopCode(final String shopCode, final String currency, final boolean active) {
        if (active) {
            final Date now = new Date();
            return getGenericDao().findByNamedQuery("PROMOTION.BY.SHOPCODE.CURRENCY.ACTIVE", shopCode, currency, active, now, now);
        }
        return getGenericDao().findByNamedQuery("PROMOTION.BY.SHOPCODE.CURRENCY", shopCode, currency);
    }

    /** {@inheritDoc} */
    public List<Promotion> findByParameters(final String code,
                                            final String shopCode,
                                            final String currency,
                                            final String tag,
                                            final String type,
                                            final String action,
                                            final Boolean enabled) {

        final List<Criterion> criterionList = new ArrayList<Criterion>();

        if (StringUtils.isNotBlank(code)) {
            criterionList.add(Restrictions.like("code", code, MatchMode.ANYWHERE));
        }

        if (StringUtils.isNotBlank(shopCode)) {
            criterionList.add(Restrictions.like("shopCode", shopCode, MatchMode.ANYWHERE));
        }

        if (StringUtils.isNotBlank(currency)) {
            criterionList.add(Restrictions.like("currency", currency, MatchMode.ANYWHERE));
        }

        if (StringUtils.isNotBlank(tag)) {
            criterionList.add(Restrictions.like("tag", tag, MatchMode.ANYWHERE));
        }

        if (StringUtils.isNotBlank(type)) {
            criterionList.add(Restrictions.like("promoType", type, MatchMode.ANYWHERE));
        }

        if (StringUtils.isNotBlank(action)) {
            criterionList.add(Restrictions.like("promoAction", action, MatchMode.ANYWHERE));
        }
        if (enabled != null) {
            criterionList.add(Restrictions.eq("enabled", enabled));
        }

        if (criterionList.isEmpty()) {
            return getGenericDao().findAll();

        } else {
            return getGenericDao().findByCriteria(
                    criterionList.toArray(new Criterion[criterionList.size()])
            );

        }
    }



    /**
     * {@inheritDoc}
     */
    public Long findPromotionIdByCode(final String code) {
        List<Object> list = getGenericDao().findQueryObjectByNamedQuery("PROMOTION.ID.BY.CODE", code);
        if (list != null && !list.isEmpty()) {
            final Object id = list.get(0);
            if (id instanceof Long) {
                return (Long) id;
            }
        }
        return null;
    }

}
