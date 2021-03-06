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

package org.yes.cart.promotion.impl.action;

import org.junit.Test;
import org.yes.cart.BaseCoreDBTestCase;
import org.yes.cart.domain.entity.Promotion;
import org.yes.cart.service.domain.PromotionService;
import org.yes.cart.shoppingcart.*;
import org.yes.cart.shoppingcart.impl.ShoppingCartImpl;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;

/**
 * User: denispavlov
 * Date: 13-10-30
 * Time: 11:31 PM
 */
public class ItemDiscountPromotionActionTest extends BaseCoreDBTestCase {

    @Test
    public void testSingleItemDiscountPromotion() throws Exception {

        final PromotionService promotionService = ctx().getBean("promotionService", PromotionService.class);

        MutableShoppingCart shoppingCart = new ShoppingCartImpl();
        shoppingCart.initialise(ctx().getBean("amountCalculationStrategy", AmountCalculationStrategy.class));
        final ShoppingCartCommandFactory commands = ctx().getBean("shoppingCartCommandFactory", ShoppingCartCommandFactory.class);

        // basic init
        commands.execute(shoppingCart,
                (Map) singletonMap(ShoppingCartCommand.CMD_SETSHOP, 10));
        commands.execute(shoppingCart,
                (Map) singletonMap(ShoppingCartCommand.CMD_CHANGECURRENCY, "EUR"));

        // create discount promotion
        final Promotion discount10 = promotionService.getGenericDao().getEntityFactory().getByIface(Promotion.class);
        discount10.setCode("CC_TEST_10%");
        discount10.setShopCode(shoppingCart.getShoppingContext().getShopCode());
        discount10.setCurrency("EUR");
        discount10.setName("10% off on CC_TEST items");
        discount10.setPromoType(Promotion.TYPE_ITEM);
        discount10.setPromoAction(Promotion.ACTION_PERCENT_DISCOUNT);
        discount10.setEligibilityCondition("['CC_TEST4', 'CC_TEST5'].contains(shoppingCartItem.productSkuCode)");
        discount10.setPromoActionContext("10");
        discount10.setEnabled(true);

        promotionService.create(discount10);


        try {
            // add qualifying items
            Map<String, String> param = new HashMap<String, String>();
            param.put(ShoppingCartCommand.CMD_SETQTYSKU, "CC_TEST4");
            param.put(ShoppingCartCommand.CMD_SETQTYSKU_P_QTY, "2.00");
            commands.execute(shoppingCart, (Map) param);

            assertEquals(1, shoppingCart.getCartItemList().size());

            final CartItem cc_test4 = shoppingCart.getCartItemList().get(0);

            assertEquals("CC_TEST4", cc_test4.getProductSkuCode());
            assertTrue(cc_test4.isPromoApplied());
            assertEquals("CC_TEST_10%", cc_test4.getAppliedPromo());
            assertEquals("2", cc_test4.getQty().toString());
            assertEquals("123.00", cc_test4.getListPrice().toString());
            assertEquals("123.00", cc_test4.getSalePrice().toString());
            assertEquals("110.70", cc_test4.getPrice().toString());

            assertEquals("246.00", shoppingCart.getTotal().getListSubTotal().toString());
            assertEquals("221.40", shoppingCart.getTotal().getSubTotal().toString());
        } finally {
            // clean test
            promotionService.delete(discount10);
        }

    }

    @Test
    public void testSingleItemDiscountPromotionWithNonEligible() throws Exception {

        final PromotionService promotionService = ctx().getBean("promotionService", PromotionService.class);

        MutableShoppingCart shoppingCart = new ShoppingCartImpl();
        shoppingCart.initialise(ctx().getBean("amountCalculationStrategy", AmountCalculationStrategy.class));
        final ShoppingCartCommandFactory commands = ctx().getBean("shoppingCartCommandFactory", ShoppingCartCommandFactory.class);

        // basic init
        commands.execute(shoppingCart,
                (Map) singletonMap(ShoppingCartCommand.CMD_SETSHOP, 10));
        commands.execute(shoppingCart,
                (Map) singletonMap(ShoppingCartCommand.CMD_CHANGECURRENCY, "EUR"));

        // create discount promotion
        final Promotion discount10 = promotionService.getGenericDao().getEntityFactory().getByIface(Promotion.class);
        discount10.setCode("CC_TEST_10%");
        discount10.setShopCode(shoppingCart.getShoppingContext().getShopCode());
        discount10.setCurrency("EUR");
        discount10.setName("10% off on CC_TEST items");
        discount10.setPromoType(Promotion.TYPE_ITEM);
        discount10.setPromoAction(Promotion.ACTION_PERCENT_DISCOUNT);
        discount10.setEligibilityCondition("['CC_TEST4', 'CC_TEST5'].contains(shoppingCartItem.productSkuCode)");
        discount10.setPromoActionContext("10");
        discount10.setEnabled(true);

        promotionService.create(discount10);


        try {
            // add qualifying items
            Map<String, String> paramQ = new HashMap<String, String>();
            paramQ.put(ShoppingCartCommand.CMD_SETQTYSKU, "CC_TEST4");
            paramQ.put(ShoppingCartCommand.CMD_SETQTYSKU_P_QTY, "2.00");
            commands.execute(shoppingCart, (Map) paramQ);

            // add non-qualifying items
            Map<String, String> paramNQ = new HashMap<String, String>();
            paramNQ.put(ShoppingCartCommand.CMD_SETQTYSKU, "CC_TEST6");
            paramNQ.put(ShoppingCartCommand.CMD_SETQTYSKU_P_QTY, "1.00");
            commands.execute(shoppingCart, (Map) paramNQ);

            assertEquals(2, shoppingCart.getCartItemList().size());

            final CartItem cc_test4 = shoppingCart.getCartItemList().get(0);

            assertEquals("CC_TEST4", cc_test4.getProductSkuCode());
            assertTrue(cc_test4.isPromoApplied());
            assertEquals("CC_TEST_10%", cc_test4.getAppliedPromo());
            assertEquals("2", cc_test4.getQty().toString());
            assertEquals("123.00", cc_test4.getListPrice().toString());
            assertEquals("123.00", cc_test4.getSalePrice().toString());
            assertEquals("110.70", cc_test4.getPrice().toString());

            final CartItem cc_test6 = shoppingCart.getCartItemList().get(1);

            assertEquals("CC_TEST6", cc_test6.getProductSkuCode());
            assertFalse(cc_test6.isPromoApplied());
            assertNull(cc_test6.getAppliedPromo());
            assertEquals("1", cc_test6.getQty().toString());
            assertEquals("55.17", cc_test6.getListPrice().toString());
            assertEquals("55.17", cc_test6.getSalePrice().toString());
            assertEquals("55.17", cc_test6.getPrice().toString());

            assertEquals("301.17", shoppingCart.getTotal().getListSubTotal().toString());
            assertEquals("276.57", shoppingCart.getTotal().getSubTotal().toString());
        } finally {
            // clean test
            promotionService.delete(discount10);
        }

    }

    @Test
    public void testMultiItemDiscountPromotion() throws Exception {

        final PromotionService promotionService = ctx().getBean("promotionService", PromotionService.class);

        MutableShoppingCart shoppingCart = new ShoppingCartImpl();
        shoppingCart.initialise(ctx().getBean("amountCalculationStrategy", AmountCalculationStrategy.class));
        final ShoppingCartCommandFactory commands = ctx().getBean("shoppingCartCommandFactory", ShoppingCartCommandFactory.class);

        // basic init
        commands.execute(shoppingCart,
                (Map) singletonMap(ShoppingCartCommand.CMD_SETSHOP, 10));
        commands.execute(shoppingCart,
                (Map) singletonMap(ShoppingCartCommand.CMD_CHANGECURRENCY, "EUR"));

        // create discount promotion
        final Promotion discount10 = promotionService.getGenericDao().getEntityFactory().getByIface(Promotion.class);
        discount10.setCode("CC_TEST_10%");
        discount10.setShopCode(shoppingCart.getShoppingContext().getShopCode());
        discount10.setCurrency("EUR");
        discount10.setName("10% off on CC_TEST items");
        discount10.setPromoType(Promotion.TYPE_ITEM);
        discount10.setPromoAction(Promotion.ACTION_PERCENT_DISCOUNT);
        discount10.setEligibilityCondition("['CC_TEST4', 'CC_TEST5'].contains(shoppingCartItem.productSkuCode)");
        discount10.setPromoActionContext("10");
        discount10.setCanBeCombined(true);
        discount10.setEnabled(true);

        promotionService.create(discount10);

        final Promotion discount5 = promotionService.getGenericDao().getEntityFactory().getByIface(Promotion.class);
        discount5.setCode("ALL_5%");
        discount5.setShopCode(shoppingCart.getShoppingContext().getShopCode());
        discount5.setCurrency("EUR");
        discount5.setName("5% off on all");
        discount5.setPromoType(Promotion.TYPE_ITEM);
        discount5.setPromoAction(Promotion.ACTION_PERCENT_DISCOUNT);
        discount5.setEligibilityCondition("true");
        discount5.setPromoActionContext("5");
        discount5.setCanBeCombined(true);
        discount5.setEnabled(true);

        promotionService.create(discount5);


        try {
            // add qualifying items
            Map<String, String> paramQ = new HashMap<String, String>();
            paramQ.put(ShoppingCartCommand.CMD_SETQTYSKU, "CC_TEST4");
            paramQ.put(ShoppingCartCommand.CMD_SETQTYSKU_P_QTY, "2.00");
            commands.execute(shoppingCart, (Map) paramQ);

            // add non-qualifying items
            Map<String, String> paramNQ = new HashMap<String, String>();
            paramNQ.put(ShoppingCartCommand.CMD_SETQTYSKU, "CC_TEST6");
            paramNQ.put(ShoppingCartCommand.CMD_SETQTYSKU_P_QTY, "1.00");
            commands.execute(shoppingCart, (Map) paramNQ);

            assertEquals(2, shoppingCart.getCartItemList().size());

            final CartItem cc_test4 = shoppingCart.getCartItemList().get(0);

            assertEquals("CC_TEST4", cc_test4.getProductSkuCode());
            assertTrue(cc_test4.isPromoApplied());
            assertEquals("CC_TEST_10%,ALL_5%", cc_test4.getAppliedPromo());
            assertEquals("2", cc_test4.getQty().toString());
            assertEquals("123.00", cc_test4.getListPrice().toString());
            assertEquals("123.00", cc_test4.getSalePrice().toString());
            assertEquals("104.55", cc_test4.getPrice().toString());

            final CartItem cc_test6 = shoppingCart.getCartItemList().get(1);

            assertEquals("CC_TEST6", cc_test6.getProductSkuCode());
            assertTrue(cc_test6.isPromoApplied());
            assertEquals("ALL_5%", cc_test6.getAppliedPromo());
            assertEquals("1", cc_test6.getQty().toString());
            assertEquals("55.17", cc_test6.getListPrice().toString());
            assertEquals("55.17", cc_test6.getSalePrice().toString());
            assertEquals("52.41", cc_test6.getPrice().toString());

            assertEquals("301.17", shoppingCart.getTotal().getListSubTotal().toString());
            assertEquals("261.51", shoppingCart.getTotal().getSubTotal().toString());
        } finally {
            // clean test
            promotionService.delete(discount10);
            promotionService.delete(discount5);
        }

    }

    @Test
    public void testBestDealPromotion() throws Exception {

        final PromotionService promotionService = ctx().getBean("promotionService", PromotionService.class);

        MutableShoppingCart shoppingCart = new ShoppingCartImpl();
        shoppingCart.initialise(ctx().getBean("amountCalculationStrategy", AmountCalculationStrategy.class));
        final ShoppingCartCommandFactory commands = ctx().getBean("shoppingCartCommandFactory", ShoppingCartCommandFactory.class);

        // basic init
        commands.execute(shoppingCart,
                (Map) singletonMap(ShoppingCartCommand.CMD_SETSHOP, 10));
        commands.execute(shoppingCart,
                (Map) singletonMap(ShoppingCartCommand.CMD_CHANGECURRENCY, "EUR"));

        // create discount promotion -10%
        final Promotion discount10 = promotionService.getGenericDao().getEntityFactory().getByIface(Promotion.class);
        discount10.setCode("CC_TEST_10%");
        discount10.setShopCode(shoppingCart.getShoppingContext().getShopCode());
        discount10.setCurrency("EUR");
        discount10.setName("10% off on CC_TEST items");
        discount10.setPromoType(Promotion.TYPE_ITEM);
        discount10.setPromoAction(Promotion.ACTION_PERCENT_DISCOUNT);
        discount10.setEligibilityCondition("['CC_TEST4', 'CC_TEST5'].contains(shoppingCartItem.productSkuCode)");
        discount10.setPromoActionContext("10");
        discount10.setCanBeCombined(true);
        discount10.setEnabled(true);

        promotionService.create(discount10);

        // create discount promotion -5%
        final Promotion discount5 = promotionService.getGenericDao().getEntityFactory().getByIface(Promotion.class);
        discount5.setCode("ALL_5%");
        discount5.setShopCode(shoppingCart.getShoppingContext().getShopCode());
        discount5.setCurrency("EUR");
        discount5.setName("5% off on all");
        discount5.setPromoType(Promotion.TYPE_ITEM);
        discount5.setPromoAction(Promotion.ACTION_PERCENT_DISCOUNT);
        discount5.setEligibilityCondition("true");
        discount5.setPromoActionContext("5");
        discount5.setCanBeCombined(false);
        discount5.setEnabled(true);

        promotionService.create(discount5);


        try {
            // add qualifying items
            Map<String, String> paramQ = new HashMap<String, String>();
            paramQ.put(ShoppingCartCommand.CMD_SETQTYSKU, "CC_TEST4");
            paramQ.put(ShoppingCartCommand.CMD_SETQTYSKU_P_QTY, "2.00");
            commands.execute(shoppingCart, (Map) paramQ);

            // add non-qualifying items
            Map<String, String> paramNQ = new HashMap<String, String>();
            paramNQ.put(ShoppingCartCommand.CMD_SETQTYSKU, "CC_TEST6");
            paramNQ.put(ShoppingCartCommand.CMD_SETQTYSKU_P_QTY, "1.00");
            commands.execute(shoppingCart, (Map) paramNQ);

            assertEquals(2, shoppingCart.getCartItemList().size());

            final CartItem cc_test4 = shoppingCart.getCartItemList().get(0);

            assertEquals("CC_TEST4", cc_test4.getProductSkuCode());
            assertTrue(cc_test4.isPromoApplied());
            assertEquals("CC_TEST_10%", cc_test4.getAppliedPromo());
            assertEquals("2", cc_test4.getQty().toString());
            assertEquals("123.00", cc_test4.getListPrice().toString());
            assertEquals("123.00", cc_test4.getSalePrice().toString());
            assertEquals("110.70", cc_test4.getPrice().toString());

            final CartItem cc_test6 = shoppingCart.getCartItemList().get(1);

            assertEquals("CC_TEST6", cc_test6.getProductSkuCode());
            assertTrue(cc_test6.isPromoApplied());
            assertEquals("ALL_5%", cc_test6.getAppliedPromo());
            assertEquals("1", cc_test6.getQty().toString());
            assertEquals("55.17", cc_test6.getListPrice().toString());
            assertEquals("55.17", cc_test6.getSalePrice().toString());
            assertEquals("52.41", cc_test6.getPrice().toString());

            assertEquals("301.17", shoppingCart.getTotal().getListSubTotal().toString());
            assertEquals("273.81", shoppingCart.getTotal().getSubTotal().toString());
        } finally {
            // clean test
            promotionService.delete(discount10);
            promotionService.delete(discount5);
        }

    }
}
