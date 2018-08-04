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
import nl.utwente.ing.model.SavingGoal;
import nl.utwente.ing.model.Transaction;
import nl.utwente.ing.model.Type;
import org.apache.commons.dbutils.DbUtils;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/savingGoals")
public class SavingGoalController {

    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<SavingGoal> getAllSavingGoals(@RequestHeader(value = "X-session-id", required = false) String
                                                          headerSessionID,
                                              @RequestParam(value = "session_id", required = false) String paramSessionID,
                                              HttpServletResponse response) {
        String sessionID = headerSessionID == null ? paramSessionID : headerSessionID;

        List<SavingGoal> savingGoals = new ArrayList<>();
        List<Transaction> transactions = new ArrayList<>();

        String savingGoalQuery = "SELECT saving_goal_id AS id, name, goal, save_per_month AS spm, minimum_balance_required AS " +
                "mbr FROM saving_goals WHERE session_id = ? ORDER BY datetime(creation_date_time) ASC;";
        String transactionQuery = "SELECT amount, date, type FROM transactions WHERE session_id = ? ORDER BY datetime" +
                "(date) ASC;";
        Connection connection = null;
        PreparedStatement savingGoalPreparedStatement = null;
        PreparedStatement transactionPreparedStatement = null;
        ResultSet savingGoalResultSet = null;
        ResultSet transactionResultSet = null;
        try {
            connection = DBConnection.instance.getConnection();
            savingGoalPreparedStatement = connection.prepareStatement(savingGoalQuery);
            savingGoalPreparedStatement.setString(1, sessionID);
            savingGoalResultSet = savingGoalPreparedStatement.executeQuery();

            while (savingGoalResultSet.next()) {
                SavingGoal savingGoal = new SavingGoal(
                        savingGoalResultSet.getInt("id"),
                        savingGoalResultSet.getString("name"),
                        Money.ofMinor(CurrencyUnit.EUR, savingGoalResultSet.getLong("goal")),
                        Money.ofMinor(CurrencyUnit.EUR, savingGoalResultSet.getLong("spm")),
                        Money.ofMinor(CurrencyUnit.EUR, savingGoalResultSet.getLong("mbr")),
                        Money.parse("EUR 0.00"));
                savingGoals.add(savingGoal);
            }

            transactionPreparedStatement = connection.prepareStatement(transactionQuery);
            transactionPreparedStatement.setString(1, sessionID);
            transactionResultSet = transactionPreparedStatement.executeQuery();

            while (transactionResultSet.next()) {
                Transaction transaction = new Transaction(
                        null,
                        transactionResultSet.getString("date"),
                        Money.ofMinor(CurrencyUnit.EUR, transactionResultSet.getLong("amount")),
                        null,
                        Type.valueOf(transactionResultSet.getString("type")),
                        null,
                        null);
                transactions.add(transaction);
            }
            response.setStatus(200);
            return calculateSavingGoalBalances(savingGoals, transactions);
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(transactionResultSet);
            DbUtils.closeQuietly(transactionPreparedStatement);
            DbUtils.closeQuietly(connection, savingGoalPreparedStatement, savingGoalResultSet);
        }
    }

    private List<SavingGoal> calculateSavingGoalBalances(List<SavingGoal> savingGoals, List<Transaction> transactions) {
        DateTime systemTime;
        Money balance;

        if (!transactions.isEmpty()) {
            Transaction firstTransaction = transactions.get(0);
            transactions.remove(0);
            systemTime = DateTime.parse(firstTransaction.getDate());
            balance = firstTransaction.getType() == Type.deposit ? firstTransaction.getAmount() :
                    Money.of(CurrencyUnit.EUR, firstTransaction.getAmount().getAmount().negate());
            for (Transaction transaction : transactions) {
                DateTime transactionTime = DateTime.parse(transaction.getDate());

                if (transaction.getType().equals(Type.deposit)) {
                    balance = balance.plus(transaction.getAmount());
                } else {
                    balance = balance.minus(transaction.getAmount());
                }

                balance = calculateNewBalances(savingGoals, balance, systemTime, transactionTime);
                systemTime = DateTime.parse(transaction.getDate());
            }
        }
        return savingGoals;
    }

    Money calculateNewBalances(List<SavingGoal> savingGoals, Money balance, DateTime systemTime,
                               DateTime transactionTime) {
        int applicationTimes = getAmountOfMonthsBetweenDates(systemTime, transactionTime);
        for (int i = 0; i < applicationTimes; i++) {
            for (SavingGoal savingGoal : savingGoals) {
                Money savingGoalBalance = Money.of(CurrencyUnit.EUR, savingGoal.getBalance());
                Money savingGoalMBR = Money.of(CurrencyUnit.EUR, savingGoal.getMinimumBalanceRequired());
                Money savingGoalGoal = Money.of(CurrencyUnit.EUR, savingGoal.getGoal());
                Money savingGoalSPM = Money.of(CurrencyUnit.EUR, savingGoal.getSavePerMonth());

                if (savingGoalMBR.isLessThan(balance) && savingGoalBalance.isLessThan(savingGoalGoal)) {
                    if (savingGoalGoal.isLessThan(savingGoalBalance.plus(savingGoalSPM))) {
                        savingGoalSPM = savingGoalGoal.minus(savingGoalBalance);
                    }

                    savingGoal.setBalance(savingGoalBalance.plus(savingGoalSPM));
                    balance = balance.minus(savingGoalSPM);
                }
            }
        }
        return balance;
    }

    private int getAmountOfMonthsBetweenDates(DateTime startDate, DateTime endDate) {
        int amount = 0;
        startDate = startDate.withTimeAtStartOfDay().withDayOfMonth(1).plusMonths(1);

        while (startDate.isBefore(endDate)) {
            amount += 1;
            startDate = startDate.plusMonths(1);
        }

        return amount;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SavingGoal createSavingGoal(@RequestHeader(value = "X-session-id", required = false) String
                                                   headerSessionID,
                                       @RequestParam(value = "session_id", required = false) String paramSessionID,
                                       @RequestBody String body,
                                       HttpServletResponse response) {
        String sessionID = headerSessionID == null ? paramSessionID : headerSessionID;

        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(SavingGoal.class, new SavingGoalAdapter());

            Gson gson = gsonBuilder.create();
            SavingGoal savingGoal = gson.fromJson(body, SavingGoal.class);

            if (savingGoal.containsNullElements()) {
                throw new JsonParseException("Saving goal is not of valid format");
            }

            String insertQuery = "INSERT INTO saving_goals (name, goal, save_per_month, minimum_balance_required, " +
                    "session_id) VALUES (?, ?, ?, ?, ?);";
            String idQuery = "SELECT last_insert_rowid()";

            Connection connection = null;
            PreparedStatement preparedStatement = null;
            PreparedStatement idPreparedStatement = null;
            ResultSet resultSet = null;

            try {
                connection = DBConnection.instance.getConnection();
                preparedStatement = connection.prepareStatement(insertQuery);

                preparedStatement.setString(1, savingGoal.getName());
                preparedStatement.setLong(2, Money.of(CurrencyUnit.EUR, savingGoal.getGoal()).getAmountMinorLong());
                preparedStatement.setLong(3, Money.of(CurrencyUnit.EUR, savingGoal.getSavePerMonth()).getAmountMinorLong());
                preparedStatement.setLong(4, Money.of(CurrencyUnit.EUR, savingGoal.getMinimumBalanceRequired()).getAmountMinorLong());
                preparedStatement.setString(5, sessionID);

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
                    savingGoal.setId(id);
                    return savingGoal;
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
                DbUtils.closeQuietly(connection, idPreparedStatement, resultSet);
            }
        } catch (JsonParseException e) {
            e.printStackTrace();
            response.setStatus(405);
            return null;
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteSavingGoal(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                 @RequestParam(value = "session_id", required = false) String querySessionID,
                                 @PathVariable("id") int id,
                                 HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;
        String query = "DELETE FROM saving_goals WHERE saving_goal_id = ? AND session_id = ?";


        DBUtil.executeDelete(response, query, id, sessionID);
    }
}

class SavingGoalAdapter implements JsonDeserializer<SavingGoal> {

    @Override
    public SavingGoal deserialize(JsonElement json, java.lang.reflect.Type type,
                                   JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            String name = jsonObject.get("name").getAsString();
            Money goal = Money.ofMajor(CurrencyUnit.EUR, jsonObject.get("goal").getAsLong());
            Money savePerMonth = Money.ofMajor(CurrencyUnit.EUR, jsonObject.get("savePerMonth").getAsLong());
            Money minimumBalanceRequired = Money.ofMajor(CurrencyUnit.EUR,
                    jsonObject.get("minBalanceRequired").getAsLong());
            return new SavingGoal(null, name, goal, savePerMonth, minimumBalanceRequired, Money.parse("EUR 0.00"));
        } catch (NullPointerException | NumberFormatException e) {
            throw new JsonParseException("SavingGoal is not of valid format");
        }
    }
}
