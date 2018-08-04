package nl.utwente.ing.controller;


import com.google.gson.JsonParseException;
import nl.utwente.ing.controller.database.DBConnection;
import nl.utwente.ing.controller.database.DBUtil;
import nl.utwente.ing.model.BalanceHistory;
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
@RequestMapping("/api/v1/balance/history")
public class BalanceHistoryController {

    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<BalanceHistory> getBalanceHistory(@RequestHeader(value = "X-session-id", required = false) String
                                                         headerSessionID,
                                                  @RequestParam(value = "session_id", required = false) String paramSessionID,
                                                  @RequestParam(value = "interval", defaultValue = "month") String interval,
                                                  @RequestParam(value = "intervals", defaultValue = "24") int intervals,
                                                  HttpServletResponse response) {
        String sessionID = headerSessionID == null ? paramSessionID : headerSessionID;
        List<DateTime> timeIntervals;
        try {
            timeIntervals = parseTimeIntervals(interval, intervals);
        } catch (JsonParseException e) {
            e.printStackTrace();
            response.setStatus(405);
            return null;
        }

        List<Transaction> transactions = new ArrayList<>();
        List<SavingGoal> savingGoals = new ArrayList<>();

        String transactionsQuery = "SELECT transaction_id AS id, amount, date, external_iban, type, description FROM " +
                "transactions WHERE session_id = ? ORDER BY datetime(date);";
        String savingGoalsQuery = "SELECT goal, save_per_month AS spm, minimum_balance_required AS mbr FROM " +
                "saving_goals WHERE session_id = ? ORDER BY datetime(creation_date_time);";
        Connection connection = null;
        PreparedStatement transactionsPreparedStatement = null;
        PreparedStatement savingGoalsPreparedStatement = null;
        ResultSet transactionsResultSet = null;
        ResultSet savingGoalsResultSet = null;
        try {
            connection = DBConnection.instance.getConnection();
            transactionsPreparedStatement = connection.prepareStatement(transactionsQuery);
            transactionsPreparedStatement.setString(1, sessionID);

            transactionsResultSet = transactionsPreparedStatement.executeQuery();

            while (transactionsResultSet.next()) {
                transactions.add(new Transaction(transactionsResultSet.getInt("id"), transactionsResultSet.getString("date"),
                        Money.ofMinor(CurrencyUnit.EUR, transactionsResultSet
                        .getLong("amount")), transactionsResultSet.getString("external_iban"), Type.valueOf(transactionsResultSet.getString
                        ("type")), null, null));
            }

            savingGoalsPreparedStatement = connection.prepareStatement(savingGoalsQuery);
            savingGoalsPreparedStatement.setString(1, sessionID);

            savingGoalsResultSet = savingGoalsPreparedStatement.executeQuery();

            while (savingGoalsResultSet.next()) {
                savingGoals.add(new SavingGoal(null, null,
                        Money.ofMinor(CurrencyUnit.EUR, savingGoalsResultSet.getLong("goal")),
                        Money.ofMinor(CurrencyUnit.EUR, savingGoalsResultSet.getLong("spm")),
                        Money.ofMinor(CurrencyUnit.EUR, savingGoalsResultSet.getLong("mbr")),
                        Money.parse("EUR 0.00")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(savingGoalsResultSet);
            DbUtils.closeQuietly(savingGoalsPreparedStatement);
            DbUtils.closeQuietly(connection, transactionsPreparedStatement, transactionsResultSet);
        }

        return getBalanceHistories(parseTransactions(transactions, timeIntervals), timeIntervals, savingGoals);
    }

    private static List<DateTime> parseTimeIntervals(String interval, int intervals) throws JsonParseException {
        List<DateTime> timeIntervals = new ArrayList<>();

        DateTime now = new DateTime();

        switch (interval.toLowerCase()) {
            case "hour"  :
                for (int i = intervals; i >= 0; i--) {
                    timeIntervals.add(new DateTime(now).minusHours(i));
                }
                break;
            case "day"   :
                for (int i = intervals; i >= 0; i--) {
                    timeIntervals.add(new DateTime(now).minusDays(i));
                }
                break;
            case "week"  :
                for (int i = intervals; i >= 0; i--) {
                    timeIntervals.add(new DateTime(now).minusWeeks(i));
                }
                break;
            case "month" :
                for (int i = intervals; i >= 0; i--) {
                    timeIntervals.add(new DateTime(now).minusMonths(i));
                }
                break;
            case "year"  :
                for (int i = intervals; i >= 0; i--) {
                    timeIntervals.add(new DateTime(now).minusYears(i));
                }
                break;
            default      :
                throw new JsonParseException("intervals not of valid format");
        }
        return timeIntervals;
    }

    private static List<List<Transaction>> parseTransactions(List<Transaction> transactions, List<DateTime>
            timeIntervals) {
        List<List<Transaction>> transactionsInTimeIntervals = new ArrayList<>();

        for (DateTime endTime : timeIntervals) {
            List<Transaction> currentTimeIntervalTransactions = new ArrayList<>();

            while (!transactions.isEmpty() && DateTime.parse(transactions.get(0).getDate()).isBefore(endTime)) {
                currentTimeIntervalTransactions.add(transactions.get(0));
                transactions.remove(0);
            }

            transactionsInTimeIntervals.add(currentTimeIntervalTransactions);
        }
        return transactionsInTimeIntervals;
    }

    private static List<BalanceHistory> getBalanceHistories(List<List<Transaction>> transactions, List<DateTime>
            dateIntervals, List<SavingGoal> savingGoals) {
        SavingGoalController savingGoalController = new SavingGoalController();
        DateTime systemTime = null;

        Money balance = Money.parse("EUR 0.00");

        for (int i = 0; i < transactions.get(0).size(); i++) {
            Transaction transaction = transactions.get(0).get(i);
            DateTime transactionTime = DateTime.parse(transaction.getDate());
            if (transaction.getType().equals(Type.deposit)) {
                balance = balance.plus(transaction.getAmount());
            } else {
                balance = balance.minus(transaction.getAmount());
            }

            if (i != 0) {
                balance = savingGoalController.calculateNewBalances(savingGoals, balance, systemTime,
                        transactionTime);
            }
            systemTime = DateTime.parse(transaction.getDate());
        }

        transactions.remove(0);
        List<BalanceHistory> balanceHistories = new ArrayList<>();

        for (int i = 0; i < transactions.size(); i++) {
            List<Transaction> currentTransactions = transactions.get(i);

            Money open = balance.abs();
            Money low = balance.abs();
            Money high = balance.abs();
            Money volume = Money.parse("EUR 0.00");

            for (int j = 0; j < currentTransactions.size(); j++) {
                Transaction transaction = currentTransactions.get(j);

                DateTime transactionTime = DateTime.parse(transaction.getDate());
                balance = savingGoalController.calculateNewBalances(savingGoals, balance, systemTime,
                        transactionTime);

                if (j == 0) {
                    open = balance.abs();
                    low = balance.abs();
                    high = balance.abs();
                }

                Money amount = transaction.getAmount();
                if (transaction.getType().equals(Type.deposit)) {
                    balance = balance.plus(amount);
                    if (balance.isGreaterThan(high)) {
                        high = balance.abs();
                    }
                } else {
                    balance = balance.minus(amount);
                    if (balance.isLessThan(low)) {
                        low = balance.abs();
                    }
                }
                volume = volume.plus(amount);
                systemTime = transactionTime;
            }

            Money close = balance.abs();

            balanceHistories.add(new BalanceHistory(open, close, high, low, volume, dateIntervals.get(i)));
        }
        return balanceHistories;
    }
}
