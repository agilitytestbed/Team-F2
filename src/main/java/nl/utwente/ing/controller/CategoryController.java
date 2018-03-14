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
import nl.utwente.ing.model.Category;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    @RequestMapping(value = "", method = RequestMethod.GET)
    public List<Category> getCategories(@RequestHeader(value = "X-session-ID", required = false) Integer headerSessionID,
                                        @RequestParam(value = "session_id", required = false) Integer querySessionID,
                                        @RequestParam(value = "offset", required = false) Integer offset,
                                        @RequestParam(value = "limit", required = false) Integer limit,
                                        @RequestParam(value = "category", required = false) String category,
                                        HttpServletResponse response) {
        Integer sessionID = headerSessionID == null ? querySessionID : headerSessionID;
        if (sessionID == null) {
            response.setStatus(401);
            return null;
        }

        try (Connection connection = DBConnection.instance.getConnection()){
            String query = "SELECT c.category_id, c.name FROM session_categories s, categories c WHERE s.session_id = ? AND s.category_id = c.category_id;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, sessionID);
            ResultSet resultSet = preparedStatement.executeQuery();

            // TODO: Take offset, limit and category parameters into consideration.
            List<Category> results = new ArrayList<>();
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                results.add(new Category(id, name));
            }

            preparedStatement.close();
            return results;
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            return null;
        }
    }

}
