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
package nl.utwente.ing;

import io.advantageous.qbit.admin.ManagedServiceBuilder;
import io.advantageous.qbit.reactive.Reactor;
import io.advantageous.qbit.reactive.ReactorBuilder;
import io.advantageous.qbit.service.ServiceQueue;
import nl.utwente.ing.api.DPAService;
import nl.utwente.ing.controller.Storage;
import nl.utwente.ing.controller.StorageAsync;

import java.util.concurrent.TimeUnit;

public class DPAMain {

    /**
     * Main method to start the server with the appropriate service.
     * @param args
     */
    public static void main(String... args) {

        final ManagedServiceBuilder managedServiceBuilder = ManagedServiceBuilder.managedServiceBuilder()
                .setRootURI("/api/v1").setPort(8080);

        final Reactor reactor = ReactorBuilder.reactorBuilder().setDefaultTimeOut(1).setTimeUnit(TimeUnit.SECONDS)
                .build();

        final ServiceQueue transactionStorageService = managedServiceBuilder
                .createServiceBuilderForServiceObject(new Storage()).build();

        transactionStorageService.startServiceQueue().startCallBackHandler();

        final StorageAsync storageAsync = transactionStorageService
                .createProxy(StorageAsync.class);

        managedServiceBuilder.addEndpointService(new DPAService(reactor, storageAsync))
                .getEndpointServerBuilder().build().startServer();

        managedServiceBuilder.getAdminBuilder().build().startServer();
    }

}
