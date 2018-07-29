package nl.utwente.ing.controller;


import com.google.gson.JsonParseException;
import nl.utwente.ing.controller.database.DBConnection;
import nl.utwente.ing.model.BalanceHistory;
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

        String transactionsQuery = "SELECT transaction_id AS id, amount, date, external_iban, type, description FROM " +
                "transactions WHERE session_id = ? ORDER BY datetime(date);";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<Transaction> transactions = new ArrayList<>();
        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(transactionsQuery);
            preparedStatement.setString(1, sessionID);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                transactions.add(new Transaction(resultSet.getInt("id"), resultSet.getString("date"),
                        Money.ofMinor(CurrencyUnit.EUR, resultSet
                        .getLong("amount")), resultSet.getString("external_iban"), Type.valueOf(resultSet.getString
                        ("type")), null, null));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DbUtils.closeQuietly(connection, preparedStatement, resultSet);
        }

        return getBalanceHistories(parseTransactions(transactions, timeIntervals), timeIntervals);
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

    private static List<BalanceHistory> getBalanceHistories(List<List<Transaction>> transactionList, List<DateTime>
            dateIntervals) {

        Money balance = Money.parse("EUR 0.00");

        for (Transaction transaction : transactionList.get(0)) {
            if (transaction.getType().equals(Type.deposit)) {
                balance = balance.plus(transaction.getAmount());
            } else {
                balance = balance.minus(transaction.getAmount());
            }
        }

        transactionList.remove(0);
        List<BalanceHistory> balanceHistoryList = new ArrayList<>();

        for (int i = 0; i < transactionList.size(); i++) {
            List<Transaction> currentTransactions = transactionList.get(i);

            Money open = balance.abs();
            Money low = balance.abs();
            Money high = balance.abs();
            Money volume = Money.parse("EUR 0.00");

            for (Transaction transaction : currentTransactions) {
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
            }

            Money close = balance.abs();

            balanceHistoryList.add(new BalanceHistory(open, close, high, low, volume, dateIntervals.get(i)));
        }
        return balanceHistoryList;
    }
}
