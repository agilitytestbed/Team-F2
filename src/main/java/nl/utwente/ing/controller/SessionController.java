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

import nl.utwente.ing.controller.database.DBConnection;
import nl.utwente.ing.controller.database.DBUtil;
import org.apache.commons.dbutils.DbUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    @RequestMapping(value = "", method = RequestMethod.POST)
    public String getSession(HttpServletResponse response) {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DBConnection.instance.getConnection();
            String sessionId = UUID.randomUUID().toString();
            while (checkSessionExists(sessionId)) {
                sessionId = UUID.randomUUID().toString();
            }

            String query = "INSERT INTO sessions (session_id) VALUES (?);";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, sessionId);
            preparedStatement.executeUpdate();

            response.setStatus(201);

            return String.format("{\"id\": \"%s\"}", sessionId);
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(preparedStatement);
            DbUtils.closeQuietly(connection);
        }
    }

    public static boolean isValidSession(HttpServletResponse response, String sessionID) {
        if (sessionID == null) {
            response.setStatus(401);
            return false;
        }

        try {
            if (SessionController.checkSessionExists(sessionID)) {
                return true;
            } else {
                response.setStatus(401);
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return false;
        }
    }

    private static boolean checkSessionExists(String sessionId) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = DBConnection.instance.getConnection();
            String query = "SELECT * FROM sessions WHERE session_id = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1,  sessionId);
            resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(connection, preparedStatement, resultSet);
        }
    }
}
