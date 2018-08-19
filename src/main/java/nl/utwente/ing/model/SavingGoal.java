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

import java.math.BigDecimal;

public class SavingGoal {
    private Integer id;
    private String name;
    private Money goal;
    private Money savePerMonth;
    private Money minimumBalanceRequired;
    private Money balance;

    public SavingGoal(Integer id, String name, Money goal, Money savePerMonth, Money minimumBalanceRequired,
                      Money balance) {
        this.id = id;
        this.name = name;
        this.goal = goal;
        this.savePerMonth = savePerMonth;
        this.minimumBalanceRequired = minimumBalanceRequired;
        this.balance = balance;
        if (balance == null) this.balance = Money.parse("EUR 0.00");
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getGoal() {
        return goal.getAmount();
    }

    public BigDecimal getSavePerMonth() {
        return savePerMonth.getAmount();
    }

    public BigDecimal getMinimumBalanceRequired() {
        return minimumBalanceRequired.getAmount();
    }

    public BigDecimal getBalance() {
        return balance.getAmount();
    }

    public boolean containsNullElements() {
        return name == null || goal == null || savePerMonth == null || minimumBalanceRequired == null;
    }
}
