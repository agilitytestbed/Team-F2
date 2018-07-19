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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import nl.utwente.ing.controller.database.DBConnection;
import nl.utwente.ing.controller.database.DBUtil;
import nl.utwente.ing.model.Category;
import org.apache.commons.dbutils.DbUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<Category> getAllCategories(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                           @RequestParam(value = "session_id", required = false) String querySessionID,
                                           HttpServletResponse response) {

        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;
        String query = "SELECT c.category_id, c.name FROM categories c WHERE c.session_id = ?;";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, sessionID);
            resultSet = preparedStatement.executeQuery();

            List<Category> results = new ArrayList<>();

            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                results.add(new Category(id, name));
            }

            return results;
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(connection, preparedStatement, resultSet);
        }
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public Category createCategory(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                   @RequestParam(value = "session_id", required = false) String querySessionID,
                                   @RequestBody String body,
                                   HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;

        try {
            Gson gson = new Gson();
            Category category = gson.fromJson(body, Category.class);

            if (category.getName() == null) {
                throw new JsonSyntaxException("Category is missing name element");
            }

            String insertQuery = "INSERT INTO categories (name, session_id) VALUES (?, ?);";
            String idQuery = "SELECT last_insert_rowid()";

            Connection connection = null;
            PreparedStatement preparedStatement = null;
            PreparedStatement idPreparedStatement = null;
            ResultSet resultSet = null;

            try {
                connection = DBConnection.instance.getConnection();
                preparedStatement = connection.prepareStatement(insertQuery);

                preparedStatement.setString(1, category.getName());
                preparedStatement.setString(2, sessionID);

                if (preparedStatement.executeUpdate() != 1) {
                    response.setStatus(405);
                    return null;
                }

                idPreparedStatement = connection.prepareStatement(idQuery);
                resultSet = idPreparedStatement.executeQuery();
                int id = resultSet.getInt(1);
                connection.commit();
                Category resultCategory = getCategory(headerSessionID, querySessionID, id, response);
                response.setStatus(201);
                return resultCategory;
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(500);
                return null;
            } finally {
                DBUtil.executeCommit(connection);
                DbUtils.closeQuietly(preparedStatement);
                DbUtils.closeQuietly(connection, idPreparedStatement, resultSet);
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            response.setStatus(405);
            return null;
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Category getCategory(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                @RequestParam(value = "session_id", required = false) String querySessionID,
                                @PathVariable("id") int id,
                                HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;
        String query = "SELECT c.category_id, c.name FROM categories c WHERE category_id = ? AND session_id = ?";

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);
            preparedStatement.setString(2, sessionID);
            resultSet = preparedStatement.executeQuery();

            Category category = null;
            response.setStatus(404);
            if (resultSet.next()) {
                response.setStatus(200);
                category = new Category(id, resultSet.getString(2));
            }

            return category;
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(connection, preparedStatement, resultSet);
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public Category putCategory(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                @RequestParam(value = "session_id", required = false) String querySessionID,
                                @PathVariable("id") int id,
                                @RequestBody String body,
                                HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;

        try {
            Gson gson = new Gson();
            Category category = gson.fromJson(body, Category.class);
            category.setId(id);

            if (category.getName() == null) {
                throw new JsonSyntaxException("Category is missing name element");
            }

            String query = "UPDATE categories SET name = ? WHERE category_id = ? AND session_id = ?";
            Connection connection = null;
            PreparedStatement preparedStatement = null;

            try {
                connection = DBConnection.instance.getConnection();
                preparedStatement = connection.prepareStatement(query);

                preparedStatement.setString(1, category.getName());
                preparedStatement.setInt(2, id);
                preparedStatement.setString(3, sessionID);

                Category result = null;
                response.setStatus(404);
                if (preparedStatement.executeUpdate() == 1) {
                    response.setStatus(200);
                    result = category;
                }

                return result;
            } catch (SQLException e) {
                e.printStackTrace();
                response.setStatus(500);
                return null;
            } finally {
                DBUtil.executeCommit(connection);
                DbUtils.closeQuietly(preparedStatement);
                DbUtils.closeQuietly(connection);
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            response.setStatus(405);
            return null;
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteCategory(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                               @RequestParam(value = "session_id", required = false) String querySessionID,
                               @PathVariable("id") int id,
                               HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;
        String query = "DELETE FROM categories WHERE category_id = ? AND session_id = ?";
        DBUtil.executeDelete(response, query, id, sessionID);
    }
}
