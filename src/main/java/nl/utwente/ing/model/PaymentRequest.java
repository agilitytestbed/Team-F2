/*
 * Copyright (c) 2018, Joost Prins <github.com/joostprins> All rights reserved.
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
package nl.utwente.ing.model;

import org.joda.money.Money;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class PaymentRequest {
    private Integer id;
    private String description;
    private DateTime dueDate;
    private Money amount;
    private Integer numberOfRequests;
    private Boolean filled;
    private List<Transaction> transactions;

    public PaymentRequest(Integer id, String description, DateTime dueDate, Money amount, Integer numberOfRequests, Boolean filled) {
        this.id = id;
        this.description = description;
        this.dueDate = dueDate;
        this.amount = amount;
        this.numberOfRequests = numberOfRequests;
        this.filled = filled;
        this.transactions = new ArrayList<>();
    }

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setFilled(Boolean filled) {
        this.filled = filled;
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public DateTime getDueDate() {
        return dueDate;
    }

    public Money getAmount() {
        return amount;
    }

    public Integer getNumberOfRequests() {
        return numberOfRequests;
    }

    public Boolean getFilled() {
        return filled;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
