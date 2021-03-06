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

package org.yes.cart.web.service.ws.client;

/**
 * User: denispavlov
 * Date: 13-10-09
 * Time: 11:03 PM
 */
public interface WsClientAbstractFactory {

    /**
     * Get user specific factory for WS.
     *
     * @param service  service interface
     * @param userName username
     * @param password password of the user
     * @param hashed   flag to denote if password is in hashed form
     * @param url      web service url
     * @param timeout  timeout for ws calls
     *
     * @return abstract factory
     */
     <S> WsClientFactory<S> getFactory(Class<S> service,
                                       String userName,
                                       String password,
                                       boolean hashed,
                                       String url,
                                       long timeout);

}
