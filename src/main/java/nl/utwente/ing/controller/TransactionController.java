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
package nl.utwente.ing.controller;

import nl.utwente.ing.model.Transaction;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("api/v1/transactions")
public class TransactionController {

    /**
     * Returns a list of all the transactions that are available to the set session id.
     * @param response to edit the status code of the response
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public void getAllTransactions(HttpServletResponse response) {
        response.setStatus(200);
    }

    /**
     * Creates a new transaction that is linked to the current sessions id.
     * @param transaction to be created
     * @param response to edit the status code of the response
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    public void createTransaction(@RequestBody Transaction transaction, HttpServletResponse response) {
        response.setStatus(201);
    }

    /**
     * Returns a specific transaction corresponding to the transaction id.
     * @param response to edit the status code of the response
     */
    @RequestMapping(value = "/{transactionId}", method = RequestMethod.GET)
    public void getTransaction(HttpServletResponse response) {
        response.setStatus(200);
    }

    /**
     * Updates the given transaction corresponding to the transaction id.
     * @param transaction updated transaction
     * @param response to edit the status code of the response
     */
    @RequestMapping(value = "/{transactionId}", method = RequestMethod.PUT)
    public void updateTransaction(@RequestBody Transaction transaction, HttpServletResponse response) {
        response.setStatus(200);
    }

    /**
     * Deletes the transaction corresponding to the given transaction id.
     * @param response to edit the status code of the response
     */
    @RequestMapping(value = "/{transactionId}", method = RequestMethod.DELETE)
    public void deleteTransaction(HttpServletResponse response) {
        response.setStatus(204);
    }

    /**
     * Assigns a category to the specified transaction corresponding to the transaction id.
     * @param response to edit the status code of the response.
     */
    @RequestMapping(value = "/{transactionId}/assignCategory", method = RequestMethod.POST)
    public void assignCategoryToTransaction(HttpServletResponse response) {
        response.setStatus(200);
    }
}
