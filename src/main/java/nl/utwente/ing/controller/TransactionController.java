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

import com.google.gson.*;
import nl.utwente.ing.controller.database.DBConnection;
import nl.utwente.ing.controller.database.DBUtil;
import nl.utwente.ing.model.Category;
import nl.utwente.ing.model.Transaction;
import nl.utwente.ing.model.Type;
import org.apache.commons.dbutils.DbUtils;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
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
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public String getAllTransactions(@RequestHeader(value = "X-session-id", required = false) String headerSessionID,
                                     @RequestParam(value = "session_id", required = false) String paramSessionID,
                                     @RequestParam(value = "offset", defaultValue = "0") int offset,
                                     @RequestParam(value = "limit", defaultValue = "20") int limit,
                                     @RequestParam(value = "category", required = false) String category,
                                     HttpServletResponse response) {

        String sessionID = headerSessionID != null ? headerSessionID : paramSessionID;
        String transactionsQuery = "SELECT transaction_id, date, amount, external_iban, type, description, " +
                "transactions.category_id, name\n" +
                "FROM transactions\n" +
                "INNER JOIN categories ON categories.category_id = transactions.category_id WHERE " +
                "transactions.session_id = ?";
        if (category != null) {
            transactionsQuery += "AND categories.name = ?";
        }
        transactionsQuery += "LIMIT ? OFFSET ?;";

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        List<Transaction> transactions = new ArrayList<>();

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(transactionsQuery);
            preparedStatement.setString(1, sessionID);

            if (category != null) {
                preparedStatement.setString(2, category);
                preparedStatement.setInt(3, limit);
                preparedStatement.setInt(4, offset);
            } else {
                preparedStatement.setInt(2, limit);
                preparedStatement.setInt(3, offset);
            }

            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Category resultCategory = null;
                int categoryId = resultSet.getInt("category_id");
                if (!resultSet.wasNull()) {
                    resultCategory = new Category(categoryId, resultSet.getString("name"));
                }

                transactions.add(new Transaction(resultSet.getInt("transaction_id"),
                        resultSet.getString("date"),
                        Money.ofMinor(CurrencyUnit.EUR, resultSet.getLong("amount")),
                        resultSet.getString("external_iban"),
                        Type.valueOf(resultSet.getString("type")),
                        resultCategory,
                        resultSet.getString("description")
                        ));
            }

            response.setStatus(200);
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Transaction.class, new TransactionAdapter());
            return gsonBuilder.create().toJson(transactions);
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(connection, preparedStatement, resultSet);
        }
    }

    /**
     * Creates a new transaction that is linked to the current sessions id.
     * @param response to edit the status code of the response
     */
    @SuppressWarnings("Duplicates")
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
    public String createTransaction(@RequestHeader(value = "X-session-id", required = false) String headerSessionID,
                                    @RequestParam(value = "session_id", required = false) String paramSessionID,
                                    @RequestBody String body,
                                    HttpServletResponse response) {
        String sessionID = headerSessionID == null ? paramSessionID : headerSessionID;

        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Transaction.class, new TransactionAdapter());
            Gson gson = gsonBuilder.create();

            Transaction transaction = gson.fromJson(body, Transaction.class);

            if (transaction.getDate() == null || transaction.getAmount() == null || transaction.getExternalIBAN() ==
                    null || transaction.getType() == null) {
                throw new JsonSyntaxException("Transaction is missing attributes");
            }

            if (DBUtil.checkCategorySession(sessionID, transaction.getCategory())) {
                String query = "INSERT INTO transactions (date, amount, external_iban, category_id, type, session_id) VALUES " +
                        "(?, ?, ?, ?, ?, ?);";
                String resultQuery = "SELECT last_insert_rowid();";
                String categoryRuleQuery = "SELECT description, iBAN, type, category_id FROM category_rules WHERE session_id = ? " +
                        "ORDER BY datetime(creation_date_time) DESC;";

                Connection connection = null;
                PreparedStatement preparedStatement = null;
                PreparedStatement resultPreparedStatement = null;
                PreparedStatement categoryRulesPreparedStatement = null;
                ResultSet resultSet = null;
                ResultSet categoryRulesResultSet = null;

                try {
                    connection = DBConnection.instance.getConnection();
                    resultPreparedStatement = connection.prepareStatement(resultQuery);
                    preparedStatement = connection.prepareStatement(query);
                    categoryRulesPreparedStatement = connection.prepareStatement(categoryRuleQuery);

                    preparedStatement.setString(1, transaction.getDate());
                    preparedStatement.setLong(2, transaction.getAmount().getAmountMinorLong());
                    preparedStatement.setString(3, transaction.getExternalIBAN());
                    if (transaction.getCategory() != null) {
                        preparedStatement.setInt(4, transaction.getCategory().getId());
                    } else {
                        categoryRulesPreparedStatement.setString(1, sessionID);

                        categoryRulesResultSet = categoryRulesPreparedStatement.executeQuery();

                        while(categoryRulesResultSet.next()) {
                            String categoryRuleDescription = categoryRulesResultSet.getString(1);
                            String categoryRuleIBAN = categoryRulesResultSet.getString(2);
                            String categoryRuleType = categoryRulesResultSet.getString(3);
                            if ((categoryRuleDescription.equals("") || categoryRuleDescription.equals(transaction.getDescription())) &&
                                    (categoryRuleIBAN.equals("") || categoryRuleIBAN.equals(transaction.getExternalIBAN())) &&
                                    (categoryRuleType.equals("") || categoryRuleType.equals(transaction.getType().toString()))) {
                                CategoryController categoryController = new CategoryController();
                                transaction.setCategory(categoryController.getCategory(headerSessionID, paramSessionID, categoryRulesResultSet.getInt(4), response));
                            }
                        }
                    }

                    preparedStatement.setString(5, transaction.getType().toString().toLowerCase());
                    preparedStatement.setString(6, sessionID);

                    if (preparedStatement.executeUpdate() != 1) {
                        response.setStatus(405);
                        return null;
                    }

                    resultSet = resultPreparedStatement.executeQuery();

                    if (resultSet.next()) {
                        connection.commit();
                        transaction.setId(resultSet.getInt(1));
                        response.setStatus(201);
                        return gson.toJson(transaction);
                    }
                    response.setStatus(405);
                    return null;

                } catch (SQLException e) {
                    e.printStackTrace();
                    response.setStatus(500);
                    return null;
                } finally {
                    DBUtil.executeCommit(connection);
                    DbUtils.closeQuietly(categoryRulesResultSet);
                    DbUtils.closeQuietly(categoryRulesPreparedStatement);
                    DbUtils.closeQuietly(preparedStatement);
                    DbUtils.closeQuietly(connection, resultPreparedStatement, resultSet);
                }
            } else {
                response.setStatus(404);
                return null;
            }
        } catch (NumberFormatException | JsonParseException e) {
            e.printStackTrace();
            response.setStatus(405);
            return null;
        }
    }

    /**
     * Returns a specific transaction corresponding to the transaction id.
     * @param response to edit the status code of the response
     */
    @RequestMapping(value = "/{transactionId}", method = RequestMethod.GET, produces = "application/json")
    public String getTransaction(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                 @RequestParam(value = "session_id", required = false) String querySessionID,
                                 @PathVariable("transactionId") int transactionId,
                                 HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;

        String query = "SELECT DISTINCT transaction_id, date, amount, external_iban, type, description, transactions.category_id, name\n" +
                "FROM transactions\n" +
                "INNER JOIN categories on transactions.category_id = categories.category_id\n" +
                "WHERE transactions.session_id = ? AND transactions.transaction_id = ?;";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);

            preparedStatement.setString(1, sessionID);
            preparedStatement.setInt(2, transactionId);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                response.setStatus(200);

                Category category = null;
                int categoryId = resultSet.getInt("category_id");
                if (!resultSet.wasNull()) {
                    category = new Category(categoryId, resultSet.getString("name"));
                }

                Transaction transaction = new Transaction(resultSet.getInt("transaction_id"),
                        resultSet.getString("date"),
                        Money.ofMinor(CurrencyUnit.EUR, resultSet.getLong("amount")),
                        resultSet.getString("external_iban"),
                        Type.valueOf(resultSet.getString("type")),
                        category,
                        resultSet.getString("description")
                );

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Transaction.class, new TransactionAdapter());
                return gsonBuilder.create().toJson(transaction);
            } else {
                response.setStatus(404);
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(connection, preparedStatement, resultSet);
        }
    }

    /**
     * Updates the given transaction corresponding to the transaction id.
     * @param response to edit the status code of the response
     */
    @RequestMapping(value = "/{transactionId}", method = RequestMethod.PUT, produces = "application/json")
    public String updateTransaction(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                    @RequestParam(value = "session_id", required = false) String querySessionID,
                                    @PathVariable("transactionId") int transactionId,
                                    @RequestBody String body,
                                    HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;

        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Transaction.class, new TransactionAdapter());
            Gson gson = gsonBuilder.create();

            Transaction transaction = gson.fromJson(body, Transaction.class);
            transaction.setId(transactionId);

            if (transaction.getDate() == null || transaction.getAmount() == null
                    || transaction.getExternalIBAN() == null || transaction.getType() == null) {
                throw new JsonSyntaxException("Transaction body is not formatted properly");
            }

            if (DBUtil.checkCategorySession(sessionID, transaction.getCategory())) {
                String query = "UPDATE transactions SET date = ?, amount = ?, external_iban = ?, type = ?, description = ? " +
                        "WHERE transaction_id = ? AND session_id = ?";
                Connection connection = null;
                PreparedStatement preparedStatement = null;

                try {
                    connection = DBConnection.instance.getConnection();
                    preparedStatement = connection.prepareStatement(query);

                    preparedStatement.setString(1, transaction.getDate());
                    preparedStatement.setLong(2, transaction.getAmount().getAmountMinorLong());
                    preparedStatement.setString(3, transaction.getExternalIBAN());
                    preparedStatement.setString(4, transaction.getType().toString().toLowerCase());
                    preparedStatement.setString(5, transaction.getDate());
                    preparedStatement.setInt(6, transactionId);
                    preparedStatement.setString(7, sessionID);

                    if (preparedStatement.executeUpdate() == 1) {
                        response.setStatus(200);
                        connection.commit();
                        return gson.toJson(transaction);
                    } else {
                        response.setStatus(404);
                        return null;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    response.setStatus(500);
                    return null;
                } finally {
                    DBUtil.executeCommit(connection);
                    DbUtils.closeQuietly(preparedStatement);
                    DbUtils.closeQuietly(connection);
                }
            } else {
                    response.setStatus(404);
                    return null;
            }
        } catch (JsonParseException | NumberFormatException e) {
            e.printStackTrace();
            response.setStatus(405);
            return null;
        }
    }

    /**
     * Deletes the transaction corresponding to the given transaction id.
     * @param response to edit the status code of the response
     */
    @RequestMapping(value = "/{transactionId}", method = RequestMethod.DELETE)
    public void deleteTransaction(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                  @RequestParam(value = "session_id", required = false) String querySessionID,
                                  @PathVariable("transactionId") int transactionId,
                                  HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;
        String query = "DELETE FROM transactions WHERE transaction_id = ? AND session_id = ?";
        DBUtil.executeDelete(response, query, transactionId, sessionID);
    }

    /**
     * Assigns a category to the specified transaction corresponding to the transaction id.
     * @param response to edit the status code of the response.
     */
    @RequestMapping(value = "/{transactionId}/category", method = RequestMethod.PATCH, produces = "application/json")
    public String assignCategoryToTransaction(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                              @RequestParam(value = "session_id", required = false) String querySessionID,
                                              @PathVariable("transactionId") int transactionId,
                                              @RequestBody String body,
                                              HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;

        int categoryId;

        try {
            categoryId = new Gson().fromJson(body, JsonObject.class).get("category_id").getAsInt();
        } catch (NullPointerException | NumberFormatException e) {
            // Body was not formatted according to API specification, treat as if no ID was specified.
            response.setStatus(404);
            return null;
        }

        String query = "UPDATE transactions SET category_id = ? WHERE transaction_id = ? AND session_id = ?";
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, categoryId);
            preparedStatement.setInt(2, transactionId);
            preparedStatement.setString(3, sessionID);
            if (preparedStatement.executeUpdate() == 1) {
                return getTransaction(headerSessionID, querySessionID, transactionId, response);
            } else {
                response.setStatus(404);
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();

            // Since the error code is not set for this SQLException the message has to be used in order to find
            // out what the error was, in case of a foreign key constraint a 404 should be thrown instead of a 500.
            if (e.getMessage().startsWith("[SQLITE_CONSTRAINT]")) {
                response.setStatus(404);
                return null;
            }

            response.setStatus(500);
            return null;
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(preparedStatement);
            DbUtils.closeQuietly(connection);
        }
    }
}

class TransactionAdapter implements JsonDeserializer<Transaction>, JsonSerializer<Transaction> {

    @Override
    public Transaction deserialize(JsonElement json, java.lang.reflect.Type type,
                                   JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        if (!jsonObject.has("date") || !jsonObject.has("amount") || !jsonObject.has("externalIBAN") || !jsonObject
                .has("type") || !jsonObject.has("description")) {
            throw new JsonParseException("Transaction does not have a valid format");
        }

        String date = jsonObject.get("date").getAsString();
        Money amount = Money.of(CurrencyUnit.EUR, jsonObject.get("amount").getAsBigDecimal());
        String externalIBAN = jsonObject.get("externalIBAN").getAsString();
        Type transactionType = Type.valueOf(jsonObject.get("type").getAsString());
        Category category = null;
        String description = jsonObject.get("description").getAsString();

        if (json.getAsJsonObject().has("category")) {
            category = new Category(jsonObject.get("category").getAsJsonObject().get("id").getAsInt(),
                    jsonObject.get("category").getAsJsonObject().get("name").getAsString());
        }

        return new Transaction(null, date, amount, externalIBAN, transactionType, category, description);
    }

    @Override
    public JsonElement serialize(Transaction transaction, java.lang.reflect.Type type,
                                 JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();

        object.addProperty("id", transaction.getId());
        object.addProperty("date", transaction.getDate());
        object.addProperty("amount", transaction.getAmount().getAmount());
        object.addProperty("externalIBAN", transaction.getExternalIBAN());
        object.addProperty("type", transaction.getType().toString());

        if (transaction.getCategory() != null) {
            JsonObject categoryObject = new JsonObject();
            categoryObject.addProperty("id", transaction.getCategory().getId());
            categoryObject.addProperty("name", transaction.getCategory().getName());
            object.add("category", categoryObject);
        }

        return object;
    }
}
