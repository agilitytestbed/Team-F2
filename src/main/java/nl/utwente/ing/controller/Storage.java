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

import nl.utwente.ing.model.Category;
import nl.utwente.ing.model.Transaction;

public class Storage {

    /**
     * Adds a transaction to the database.
     * @param transaction to be added
     * @return true if successful
     */
    public boolean addTransaction(final Transaction transaction) {
        //TODO: add this transaction to the database
        return true;
    }

    /**
     * Removes a transaction from the database.
     * @param transaction to be deleted
     * @return true if successful
     */
    public boolean deleteTransaction(final Transaction transaction) {
        //TODO: delete this category from the database
        return true;
    }

    /**
     * Adds a category to the database.
     * @param category to be added
     * @return true if successful
     */
    public boolean addCategory(final Category category) {
        //TODO: add this category to the database
        return true;
    }

    /**
     * Deletes a category from the database.
     * @param category to be deleted
     * @return true if succesful
     */
    public boolean deleteCategory(final Category category) {
        //TODO: delete this category from the database
        return true;
    }

}
