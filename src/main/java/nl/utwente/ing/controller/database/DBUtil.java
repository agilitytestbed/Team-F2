package nl.utwente.ing.controller.database;

import nl.utwente.ing.model.Category;
import org.apache.commons.dbutils.DbUtils;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBUtil {

    /**
     * Executes a delete on the database.
     * Expects the first parameter of the input query to be the ID of the object to delete and the second one
     * to be the session ID of the owner of this object.
     * Example: DELETE FROM transactions WHERE transaction_id = ? AND session_id = ?
     *
     * @param response the HttpServletResponse which will be updated with the result of the update
     * @param query the query which is to be executed, which should contain two parameters. The first parameter should
     *              be the ID of the object to delete, and the second parameter should be the session ID of the owner
     *              of this object.
     * @param id the ID of the object to delete
     * @param sessionID the session ID of the owner of the object to delete
     */
    public static void executeDelete(HttpServletResponse response, String query, int id, String sessionID) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);
            preparedStatement.setString(2, sessionID);
            if (preparedStatement.executeUpdate() == 1) {
                response.setStatus(204);
            } else {
                response.setStatus(404);
            }
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
        } finally {
            DbUtils.closeQuietly(preparedStatement);
            DbUtils.closeQuietly(connection);
        }
    }

    public static boolean checkCategorySession(String session_id, Category category) {
        if (category != null) {
            if (category.getId() != null) {
                String query = "SELECT category_id FROM categories WHERE session_id = ? AND category_id = ?;";
                Connection connection = null;
                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;
                try {
                    connection = DBConnection.instance.getConnection();
                    preparedStatement = connection.prepareStatement(query);

                    preparedStatement.setString(1, session_id);
                    preparedStatement.setInt(2, category.getId());

                    resultSet = preparedStatement.executeQuery();
                    return resultSet.next();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    DbUtils.closeQuietly(connection, preparedStatement, resultSet);
                }
            }
            return false;
        }
        return true;
    }

    public static void executeCommit(Connection connection) {
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException ignored) {
            }
        }
    }
}
