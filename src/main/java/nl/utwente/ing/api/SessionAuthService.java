package nl.utwente.ing.api;

import io.advantageous.qbit.reactive.Callback;
import nl.utwente.ing.controller.database.DBConnection;

import java.sql.*;

public class SessionAuthService implements AuthorizationService {

    private static final String VALID_SESSION_CHECK = "SELECT iban FROM sessions WHERE session_id = ?";

    @Override
    public void validSession(final Callback<Boolean> callback, final String pathSessionID, final String bodySessionID) {
        if (pathSessionID.isEmpty() && bodySessionID.isEmpty()) {
            callback.accept(false);
        } else {
            try {
                String sessionID = (pathSessionID.isEmpty()) ? bodySessionID : pathSessionID;
                Connection connection = new DBConnection().getConnection();
                PreparedStatement statement = connection.prepareStatement(VALID_SESSION_CHECK);
                statement.setString(1, sessionID);
                ResultSet resultSet = statement.executeQuery();
                callback.accept(resultSet.next());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
