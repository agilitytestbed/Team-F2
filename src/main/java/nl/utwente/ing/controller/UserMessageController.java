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
import nl.utwente.ing.model.Transaction;
import nl.utwente.ing.model.Type;
import nl.utwente.ing.model.UserMessage;
import nl.utwente.ing.model.UserMessageType;
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
import java.util.List;

@RestController
@RequestMapping("api/v1/messages")
public class UserMessageController {
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public String getAllUserMessages(@RequestHeader(value = "X-session-id", required = false) String headerSessionID,
                                     @RequestParam(value = "session_id", required = false) String paramSessionID,
                                     HttpServletResponse response) {
        String sessionID = headerSessionID == null ? paramSessionID : headerSessionID;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(UserMessage.class, new UserMessageAdapter());
        Gson gson = gsonBuilder.create();

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String query = "SELECT user_message_id, message, date, type FROM user_messages WHERE session_id = ? AND read " +
                "= 0 ORDER BY datetime(date)";

        List<UserMessage> userMessages = new ArrayList<>();

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);

            preparedStatement.setString(1, sessionID);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                userMessages.add(new UserMessage(resultSet.getInt("user_message_id"),
                        resultSet.getString("message"),
                        DateTime.parse(resultSet.getString("date")),
                        false,
                        UserMessageType.valueOf(resultSet.getString("type"))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(connection, preparedStatement, resultSet);
        }

        return gson.toJson(userMessages);
    }

    @RequestMapping(value = "/{userMessageId}", method = RequestMethod.PUT, produces = "application/json")
    public String updateUserMessage(@RequestHeader(value = "X-session-id", required = false) String headerSessionID,
                                    @RequestParam(value = "session_id", required = false) String paramSessionID,
                                    @PathVariable("userMessageId") int userMessageId,
                                    HttpServletResponse response) {
        String sessionID = headerSessionID == null ? paramSessionID : headerSessionID;

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String query = "UPDATE user_messages SET read = 1 WHERE user_message_id = ? AND session_id = ?;";

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);

            preparedStatement.setInt(1, userMessageId);
            preparedStatement.setString(2, sessionID);

            if (preparedStatement.executeUpdate() == 1) {
                response.setStatus(200);
                return null;
            } else {
                response.setStatus(404);
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(preparedStatement);
            DbUtils.closeQuietly(connection);
        }
        return null;
    }

    public static void ProcessTransactions(String sessionID) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String query = ";";

        List<Transaction> transactions = new ArrayList<>();

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                transactions.add(new Transaction(null, resultSet.getString("date"), Money.ofMinor(CurrencyUnit.EUR,
                        resultSet.getLong("amount")), null,
                        Type.valueOf(resultSet.getString("type")), null, null));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(connection, preparedStatement, resultSet);
        }

        boolean newHigh = checkNewHigh(transactions);
        boolean belowZero = checkBelowZero(transactions);
        List<UserMessage> paymentRequestUserMessages = checkPaymentRequests(sessionID);
        checkSavingGoals(sessionID);
    }

    private static boolean checkNewHigh(List<Transaction> transactions) {
        return false;
    }

    private static boolean checkBelowZero(List<Transaction> transactions) {
        return false;
    }

    private static List<UserMessage> checkPaymentRequests(String sessionID) {
        return new ArrayList<>();
    }

    private static List<UserMessage> checkSavingGoals(String sessionID) {
        return new ArrayList<>();
    }
}

class UserMessageAdapter implements JsonSerializer<UserMessage> {

    @Override
    public JsonElement serialize(UserMessage userMessage, java.lang.reflect.Type type,
                                 JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        object.addProperty("id", userMessage.getId());
        object.addProperty("message", userMessage.getMessage());
        object.addProperty("date", dateTimeFormatter.print(userMessage.getDate()));
        object.addProperty("read", userMessage.getRead());
        object.addProperty("type", userMessage.getType().toString());

        return object;
    }
}
