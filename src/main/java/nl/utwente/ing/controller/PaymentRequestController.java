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
package nl.utwente.ing.controller;

import com.google.gson.*;
import nl.utwente.ing.controller.database.DBConnection;
import nl.utwente.ing.controller.database.DBUtil;
import nl.utwente.ing.model.PaymentRequest;
import nl.utwente.ing.model.SavingGoal;
import nl.utwente.ing.model.Transaction;
import nl.utwente.ing.model.Type;
import org.apache.commons.dbutils.DbUtils;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/paymentRequests")
public class PaymentRequestController {
    @RequestMapping(value = "", method = RequestMethod.GET)
    public String getAllPaymentRequests(@RequestHeader(value = "X-session-id", required = false) String headerSessionID,
                                        @RequestParam(value = "session_id", required = false) String paramSessionID,
                                        HttpServletResponse response) {
        String sessionID = headerSessionID == null ? paramSessionID : headerSessionID;
        GsonBuilder transactionGsonBuilder = new GsonBuilder();
        transactionGsonBuilder.registerTypeAdapter(Transaction.class, new TransactionAdapter());
        List<Transaction> transactionList = Arrays.asList(
                transactionGsonBuilder.create().fromJson(new TransactionController().getAllTransactions(headerSessionID,
                paramSessionID, 0, 0, null, response), Transaction[].class));

        List<PaymentRequest> paymentRequests = new ArrayList<>();

        String query  = "SELECT payment_request_id AS id, description, due_date, amount, number_of_requests FROM " +
                "payment_requests WHERE session_id = ? ORDER BY datetime(creation_date) ASC;";
        String transactionQuery = "SELECT t.transaction_id, t.date, t.amount, t.external_iban, t.type, t.description\n" +
                "FROM transactions t\n" +
                "    NATURAL LEFT OUTER JOIN categories\n" +
                "WHERE t.session_id = ?;";

        Connection connection = null;
        PreparedStatement transactionPreparedStatement = null;
        PreparedStatement preparedStatement = null;
        ResultSet transactionResultSet = null;
        ResultSet resultSet = null;

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);
            transactionPreparedStatement = connection.prepareStatement(transactionQuery);
            transactionPreparedStatement.setString(1, sessionID);

            transactionResultSet = transactionPreparedStatement.executeQuery();

            while (transactionResultSet.next()) {
                System.out.println("yes");
            }

            preparedStatement.setString(1, sessionID);

            resultSet = preparedStatement.executeQuery();


            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String description = resultSet.getString("description");
                DateTime dueDate = DateTime.parse(resultSet.getString("due_date"));
                Money amount = Money.ofMinor(CurrencyUnit.EUR, resultSet.getLong("amount"));
                int numberOfRequests = resultSet.getInt("number_of_requests");

                paymentRequests.add(new PaymentRequest(id, description, dueDate, amount, numberOfRequests, false));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DbUtils.closeQuietly(connection, preparedStatement, resultSet);
        }
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PaymentRequest.class, new PaymentRequestAdapter());
        response.setStatus(200);
        return gsonBuilder.create().toJson(parseTransactions(paymentRequests, transactionList));
    }

    private List<PaymentRequest> parseTransactions(List<PaymentRequest> paymentRequests,
                                                   List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            if (transaction.getType().equals(Type.deposit)) {
                for (PaymentRequest paymentRequest : paymentRequests) {
                    if (!paymentRequest.getFilled() &&
                            paymentRequest.getDueDate().isBefore(DateTime.parse(transaction.getDate())) &&
                            paymentRequest.getAmount().isEqual(transaction.getAmount())) {
                        paymentRequest.addTransaction(transaction);
                        if (paymentRequest.getTransactions().size() == paymentRequest.getNumberOfRequests()) {
                            paymentRequest.setFilled(true);
                        }
                        break;
                    }
                }
            }
        }
        return paymentRequests;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    @SuppressWarnings("Duplicates")
    public String createPaymentRequest(@RequestHeader(value = "X-session-id", required = false) String
                                               headerSessionID,
                                               @RequestParam(value = "session_id", required = false) String paramSessionID,
                                               @RequestBody String body,
                                               HttpServletResponse response) {
        String sessionID = headerSessionID == null ? paramSessionID : headerSessionID;
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(PaymentRequest.class, new PaymentRequestAdapter());

            Gson gson = gsonBuilder.create();
            PaymentRequest paymentRequest = gson.fromJson(body, PaymentRequest.class);

            String insertQuery = "INSERT INTO payment_requests (description, due_date, amount, " +
                    "number_of_requests, session_id, creation_date) VALUES (?, ?, ?, ?, ?, ?);";
            String idQuery = "SELECT last_insert_rowid()";
            String timeQuery = "SELECT date FROM transactions WHERE session_id = ? ORDER BY datetime(date) DESC LIMIT" +
                    " 1";

            Connection connection = null;
            PreparedStatement preparedStatement = null;
            PreparedStatement idPreparedStatement = null;
            PreparedStatement timePreparedStatement = null;
            ResultSet resultSet = null;
            ResultSet timeResultSet = null;

            try {
                connection = DBConnection.instance.getConnection();
                timePreparedStatement = connection.prepareStatement(timeQuery);
                timePreparedStatement.setString(1, sessionID);
                timeResultSet = timePreparedStatement.executeQuery();
                DateTime dateTime = DateTime.now();
                if (timeResultSet.next()) {
                    dateTime = DateTime.parse(timeResultSet.getString("date"));
                }

                preparedStatement = connection.prepareStatement(insertQuery);

                preparedStatement.setString(1, paymentRequest.getDescription());
                preparedStatement.setString(2, dateTimeFormatter.print(paymentRequest.getDueDate()));
                preparedStatement.setInt(3, paymentRequest.getAmount().getAmountMinorInt());
                preparedStatement.setInt(4, paymentRequest.getNumberOfRequests());
                preparedStatement.setString(5, sessionID);
                preparedStatement.setString(6, dateTimeFormatter.print(dateTime));

                if (preparedStatement.executeUpdate() != 1) {
                    response.setStatus(405);
                    return null;
                }

                idPreparedStatement = connection.prepareStatement(idQuery);
                resultSet = idPreparedStatement.executeQuery();

                if (resultSet.next()) {
                    connection.commit();
                    int id = resultSet.getInt(1);
                    response.setStatus(201);
                    paymentRequest.setId(id);
                    return gson.toJson(paymentRequest);
                }

                response.setStatus(500);
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(500);
                return null;
            } finally {
                DBUtil.executeCommit(connection);
                DbUtils.closeQuietly(preparedStatement);
                DbUtils.closeQuietly(timeResultSet);

                DbUtils.closeQuietly(timePreparedStatement);
                DbUtils.closeQuietly(connection, idPreparedStatement, resultSet);
            }
        } catch (JsonParseException e) {
            e.printStackTrace();
            response.setStatus(405);
            return null;
        }
    }
}

class PaymentRequestAdapter implements JsonSerializer<PaymentRequest>, JsonDeserializer<PaymentRequest> {

    @Override
    public PaymentRequest deserialize(JsonElement json, java.lang.reflect.Type type,
                                  JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            String description = jsonObject.get("description").getAsString();
            DateTime dueDate = DateTime.parse(jsonObject.get("due_date").getAsString());
            Money amount = Money.of(CurrencyUnit.EUR, jsonObject.get("amount").getAsBigDecimal());
            Integer numberOfRequests = jsonObject.get("number_of_requests").getAsInt();
            return new PaymentRequest(null, description, dueDate, amount, numberOfRequests, false);
        } catch (NullPointerException | NumberFormatException e) {
            throw new JsonParseException("PaymentRequest is not of valid format");
        }
    }

    @Override
    public JsonElement serialize(PaymentRequest paymentRequest, java.lang.reflect.Type type,
                                 JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Transaction.class, new TransactionAdapter());

        object.addProperty("id", paymentRequest.getId());
        object.addProperty("description", paymentRequest.getDescription());
        object.addProperty("due_date", dateTimeFormatter.print(paymentRequest.getDueDate()));
        object.addProperty("amount", paymentRequest.getAmount().getAmount());
        object.addProperty("number_of_requests", paymentRequest.getNumberOfRequests());
        object.addProperty("filled", paymentRequest.getFilled());

        JsonArray transactionsObject =
                new JsonParser().parse(gsonBuilder.create().toJson(paymentRequest.getTransactions())).getAsJsonArray();
        object.add("transactions", transactionsObject);

        return object;
    }

}
