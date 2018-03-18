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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import nl.utwente.ing.controller.database.DBConnection;
import nl.utwente.ing.model.Category;
import nl.utwente.ing.model.Transaction;
import nl.utwente.ing.model.Type;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/v1/transactions")
public class TransactionController {

    /**
     * Returns a list of all the transactions that are available to the session id.
     * @param response to edit the status code of the response
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<Transaction> getAllTransactions(@RequestHeader(value = "X-session-id", required = false) Integer
                                                            headerSessionID,
                                                @RequestParam(value = "session_id", required = false) Integer
                                                        paramSessionID,
                                                @RequestParam(value = "offset", defaultValue = "0") int offset,
                                                @RequestParam(value = "limit", defaultValue = "20") int limit,
                                                @RequestParam(value = "category", required = false) String category,
                                                HttpServletResponse response) {

        String transactionsQuery = "SELECT t.transaction_id, t.amount, t.date, t.external_iban, t.type, c.category_id, c.name\n" +
                "FROM transactions t, categories c\n" +
                "WHERE c.category_id = t.category_id AND\n" +
                "      t.session_id = ? AND\n" +
                "      c.session_id = t.session_id\n";

        Integer sessionID = headerSessionID != null ? headerSessionID : paramSessionID;

        if (sessionID == null) {
            response.setStatus(401);
            return null;
        }

        if (category != null) {
            transactionsQuery += "AND c.name = ?";
        }

        transactionsQuery += "LIMIT ? OFFSET ?;";

        List<Transaction> transactions = new ArrayList<>();

        try {

            Connection connection = DBConnection.instance.getConnection();

            PreparedStatement statement = connection.prepareStatement(transactionsQuery);
            statement.setInt(1, sessionID);

            if (category != null) {
                statement.setString(2, category);
                statement.setInt(3, limit);
                statement.setInt(4, offset);
            } else {
                statement.setInt(2, limit);
                statement.setInt(3, offset);
            }

            ResultSet result = statement.executeQuery();
            while (result.next()) {
                transactions.add(new Transaction(result.getInt(1),
                        result.getString(2),
                        result.getLong(3),
                        result.getString(4),
                        Type.valueOf(result.getString(5)),
                        new Category(result.getInt(6), result.getString(7))
                        ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        response.setStatus(200);
        return transactions;
    }

    /**
     * Creates a new transaction that is linked to the current sessions id.
     * @param response to edit the status code of the response
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    public Transaction createTransaction(@RequestHeader(value = "X-session-id", required = false) Integer
                                              headerSessionID,
                                         @RequestParam(value = "session_id", required = false) Integer
                                              paramSessionID,
                                         @RequestBody String body,
                                         HttpServletResponse response) {
        Integer sessionID = headerSessionID == null ? paramSessionID : headerSessionID;

        if (sessionID == null) {
            response.setStatus(401);
            return null;
        }

        try {
            Gson gson = new Gson();
            Transaction transaction = gson.fromJson(body, Transaction.class);

            if (transaction.getDate() == null || transaction.getAmount() == null || transaction.getExternalIBAN() ==
                    null || transaction.getType() == null) {
                throw new JsonSyntaxException("Transaction is missing attributes");
            }

            String query = "INSERT INTO transactions (date, amount, external_iban, category_id, type, session_id) VALUES " +
                    "(?, ?, ?, ?, ?, ?);";

            try (Connection connection = DBConnection.instance.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)
            ) {
                statement.setString(1, transaction.getDate());
                statement.setLong(2, transaction.getAmount());
                statement.setString(3, transaction.getExternalIBAN());
                statement.setInt(4, transaction.getCategory().getId());
                statement.setString(5, transaction.getType().toString());
                statement.setInt(6, sessionID);
                statement.executeUpdate();

                response.setStatus(201);
                return transaction;
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(500);
                return null;
            }
        } catch (JsonSyntaxException e) {
            response.setStatus(405);
            return null;
        }
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
    @RequestMapping(value = "/{transactionId}/category", method = RequestMethod.PATCH)
    public void assignCategoryToTransaction(HttpServletResponse response) {
        response.setStatus(200);
    }
}
