/*
 * Copyright (c) 2018, Joost Prins <github.com/joostprins>, Tom Leemreize <https://github.com/oplosthee>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.utwente.ing.api;

import io.advantageous.qbit.annotation.QueueCallback;
import io.advantageous.qbit.annotation.QueueCallbackType;
import io.advantageous.qbit.annotation.RequestMapping;
import io.advantageous.qbit.annotation.RequestMethod;
import io.advantageous.qbit.reactive.Callback;
import io.advantageous.qbit.reactive.Reactor;
import nl.utwente.ing.controller.StorageAsync;
import nl.utwente.ing.model.Transaction;

import java.util.List;

/**
 * The public REST interface to the Digital Payment Assistant API.
 */
@RequestMapping("/")
public class DPAService {

    private final Reactor reactor;
    private final StorageAsync storageAsync;
    private final AuthorizationService authService;

    /**
     * Constructor for a new Digital Payment Assistant REST service.
     * @param reactor reactor
     * @param storageAsync async interface to the TransactionStorage
     */
    public DPAService(final Reactor reactor, final StorageAsync storageAsync, final AuthorizationService authService) {
        this.reactor = reactor;
        this.reactor.addServiceToFlush(storageAsync);
        this.reactor.addServiceToFlush(authService);
        this.storageAsync = storageAsync;
        this.authService = authService;

    }


    /**
     * Add a transaction to the current session.
     * @param callback
     */
    @RequestMapping(value = "/transactions/", method = RequestMethod.POST, code = 201)
    public void addTransaction(final Callback<Boolean> callback, String sessionID) {
        storageAsync.createTransaction(callback, sessionID); //TODO: validate the session of the user
    }

    /**
     * Get a JSON list of all transactions.
     */
    @RequestMapping(value = "/transactions/")
    public void getAllTransactions(final Callback<List<Transaction>> callback) {
        //TODO:
    }

    @QueueCallback({QueueCallbackType.EMPTY,
            QueueCallbackType.IDLE, QueueCallbackType.LIMIT})
    private void process () {
        /** Call the reactor to process callbacks. */
        reactor.process();
    }

}
