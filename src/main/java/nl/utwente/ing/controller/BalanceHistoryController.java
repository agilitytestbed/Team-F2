package nl.utwente.ing.controller;


import com.google.gson.JsonParseException;
import nl.utwente.ing.controller.database.DBConnection;
import nl.utwente.ing.model.BalanceHistory;
import nl.utwente.ing.model.Transaction;
import nl.utwente.ing.model.Type;
import org.apache.commons.dbutils.DbUtils;
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
                transactions.add(new Transaction(resultSet.getInt("id"), resultSet.getString("date"), resultSet
                        .getLong("amount"), resultSet.getString("external_iban"), Type.valueOf(resultSet.getString
                        ("type")), null, null));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DbUtils.closeQuietly(connection, preparedStatement, resultSet);
        }

        try {
            return parseTransactions(transactions, intervals, interval);
        } catch (JsonParseException e) {
            response.setStatus(405);
            return null;
        }
    }

    private static List<BalanceHistory> parseTransactions(List<Transaction> transactions, int intervals, String
            interval)
            throws JsonParseException {

        List<DateTime> dateSplit = new ArrayList<>();

        DateTime beginTime = new DateTime();

        switch (interval.toLowerCase()) {
            case "hour"  :
                for (int i = intervals; i >= 0; i--) {
                    dateSplit.add(new DateTime(beginTime).minusHours(i));
                }
                break;
            case "day"   :
                for (int i = intervals; i >= 0; i--) {
                    dateSplit.add(new DateTime(beginTime).minusDays(i));
                }
                break;
            case "week"  :
                for (int i = intervals; i >= 0; i--) {
                    dateSplit.add(new DateTime(beginTime).minusWeeks(i));
                }
                break;
            case "month" :
                for (int i = intervals; i >= 0; i--) {
                    dateSplit.add(new DateTime(beginTime).minusMonths(i));
                }
                break;
            case "year"  :
                for (int i = intervals; i >= 0; i--) {
                    dateSplit.add(new DateTime(beginTime).minusYears(i));
                }
                break;
            default      :
                throw new JsonParseException("intervals not of valid format");
        }

        List<List<Transaction>> result = new ArrayList<>();

        for (DateTime date : dateSplit) {
            List<Transaction> currentResult = new ArrayList<>();

            while (!transactions.isEmpty() && DateTime.parse(transactions.get(0).getDate()).isBefore(date)) {
                currentResult.add(transactions.get(0));
                transactions.remove(0);
            }

            result.add(currentResult);
        }

        List<List<Long>> balanceHistories = getBalanceHistories(result);
        List<BalanceHistory> balanceHistoryList = new ArrayList<>();

        for (int i = 0; i < balanceHistories.size(); i++) {
            List<Long> balanceHistory = balanceHistories.get(i);
            balanceHistoryList.add(new BalanceHistory(balanceHistory.get(0), balanceHistory.get(1), balanceHistory
                    .get(2), balanceHistory.get(3), balanceHistory.get(4), dateSplit.get(i)));
        }

        return balanceHistoryList;
    }

    private static List<List<Long>> getBalanceHistories(List<List<Transaction>> transactionList) {

        long balance = 0;

        for (Transaction transaction : transactionList.get(0)) {
            if (transaction.getType().equals(Type.deposit)) {
                balance += transaction.getAmount();
            } else {
                balance -= transaction.getAmount();
            }
        }

        transactionList.remove(0);

        List<List<Long>> result = new ArrayList<>();

        for (List<Transaction> currentTransactions : transactionList) {
            List<Long> currentResult = new ArrayList<>();
            long open = balance;
            long low = balance;
            long high = balance;
            long volume = 0;

            for (Transaction transaction : currentTransactions) {
                long amount = transaction.getAmount();
                if (transaction.getType().equals(Type.deposit)) {
                    balance += amount;
                    if (balance > high) {
                        high = balance;
                    }
                } else {
                    balance -= amount;
                    if (balance < low) {
                        low = balance;
                    }
                }
                volume += amount;
            }

            long close = balance;
            currentResult.add(open);
            currentResult.add(close);
            currentResult.add(high);
            currentResult.add(low);
            currentResult.add(volume);
            result.add(currentResult);
        }
        return result;
    }
}
