package nl.utwente.ing.controller;

import com.google.gson.*;
import nl.utwente.ing.controller.database.DBConnection;
import nl.utwente.ing.controller.database.DBUtil;
import nl.utwente.ing.model.Category;
import nl.utwente.ing.model.CategoryRule;
import org.apache.commons.dbutils.DbUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categoryRules")
public class CategoryRuleController {
    @RequestMapping(value = "", method = RequestMethod.GET)
    public String getAllCategoryRules(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                      @RequestParam(value = "session_id", required = false) String querySessionID,
                                      HttpServletResponse response) {

        String sessionID = headerSessionID != null ? headerSessionID : querySessionID;

        List<CategoryRule> categoryRules = new ArrayList<>();

        String categoryRuleQuery = "SELECT category_rule_id, description, iBAN, type, category_id, apply_on_history " +
                "FROM category_rules " +
                "WHERE session_id = ?;";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(categoryRuleQuery);
            preparedStatement.setString(1, sessionID);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                categoryRules.add(new CategoryRule(resultSet.getInt(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        new Category(resultSet.getInt(5), null),
                        resultSet.getBoolean(6)
                        ));
            }
            response.setStatus(200);
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(CategoryRule.class, new CategoryRuleAdapter());
            return gsonBuilder.create().toJson(categoryRules);
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
    public String createCategoryRule(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                           @RequestParam(value = "session_id", required = false) String querySessionID,
                                           @RequestBody String body,
                                           HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(CategoryRule.class, new CategoryRuleAdapter());
            Gson gson = gsonBuilder.create();
            CategoryRule categoryRule = gson.fromJson(body, CategoryRule.class);

            if (categoryRule.getApplyOnHistory() == null || categoryRule.getCategory().getId() == null || categoryRule.getType()
                    == null || categoryRule.getiBAN() == null || categoryRule.getDescription() == null) {
                throw new JsonSyntaxException("CategoryRule is missing attributes");
            }

            if (DBUtil.checkCategorySession(sessionID, categoryRule.getCategory())) {
                String query = "INSERT INTO category_rules (description, iBAN, type, category_id, apply_on_history, session_id) VALUES (?, ?, ?, ?, ?, ?)";
                String resultQuery = "SELECT last_insert_rowid() FROM category_rules LIMIT 1;";

                Connection connection = null;
                PreparedStatement preparedStatement = null;
                PreparedStatement resultPreparedStatement = null;
                ResultSet resultSet = null;

                try {
                    connection = DBConnection.instance.getConnection();
                    preparedStatement = connection.prepareStatement(query);
                    resultPreparedStatement = connection.prepareStatement(resultQuery);

                    preparedStatement.setString(1, categoryRule.getDescription());
                    preparedStatement.setString(2, categoryRule.getiBAN());
                    preparedStatement.setString(3, categoryRule.getType());
                    preparedStatement.setInt(4, categoryRule.getCategory().getId());
                    preparedStatement.setBoolean(5, categoryRule.getApplyOnHistory());
                    preparedStatement.setString(6, sessionID);

                    if (preparedStatement.executeUpdate() != 1) {
                        response.setStatus(405);
                        return null;
                    }

                    resultSet = resultPreparedStatement.executeQuery();

                    if (resultSet.next()) {
                        categoryRule.setId(resultSet.getInt(1));
                    } else {
                        response.setStatus(405);
                        return null;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    response.setStatus(500);
                    return null;
                } finally {
                    DBUtil.executeCommit(connection);
                    DbUtils.closeQuietly(preparedStatement);
                    DbUtils.closeQuietly(connection, resultPreparedStatement, resultSet);
                }
                if (categoryRule.getApplyOnHistory()) {
                    applyCategoryRuleOnHistory(categoryRule, sessionID);
                }

                String resultCategoryRule = getCategoryRule(headerSessionID, querySessionID, categoryRule.getId(), response);
                response.setStatus(201);
                return resultCategoryRule;
            } else {
                response.setStatus(404);
                return null;
            }
        } catch (JsonSyntaxException | NumberFormatException e) {
            e.printStackTrace();
            response.setStatus(405);
            return null;
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String getCategoryRule(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                        @RequestParam(value = "session_id", required = false) String querySessionID,
                                        @PathVariable("id") int id,
                                        HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;
        String query = "SELECT category_rule_id, description, iBAN, type, category_id, apply_on_history " +
                "FROM category_rules WHERE session_id = ? AND category_rule_id = ?";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;


        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, sessionID);
            preparedStatement.setInt(2, id);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                response.setStatus(200);
                CategoryRule categoryRule = new CategoryRule(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), new Category(resultSet.getInt(5), null), resultSet.getBoolean(6));

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(CategoryRule.class, new CategoryRuleAdapter());
                return gsonBuilder.create().toJson(categoryRule);

            } else {
                response.setStatus(404);
                return null;
            }
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
    public String putCategoryRule(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                    @RequestParam(value = "session_id", required = false) String querySessionID,
                                    @PathVariable("id") int id,
                                    @RequestBody String body,
                                    HttpServletResponse response) {

        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;

        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(CategoryRule.class, new CategoryRuleAdapter());
            Gson gson = gsonBuilder.create();
            CategoryRule categoryRule = gson.fromJson(body, CategoryRule.class);
            categoryRule.setId(id);

            if (categoryRule.getDescription() == null || categoryRule.getiBAN() == null ||
                    categoryRule.getType() == null || categoryRule.getApplyOnHistory() == null ||
                    categoryRule.getCategory() == null) {
                throw new JsonSyntaxException("CategoryRule is missing elements");
            }

            if (DBUtil.checkCategorySession(sessionID, categoryRule.getCategory())) {
                String query = "UPDATE category_rules SET description = ?, iBAN = ?, type = ?, category_id = ? WHERE category_rule_id = ? AND session_id = ?";
                Connection connection = null;
                PreparedStatement preparedStatement = null;

                try {
                    connection = DBConnection.instance.getConnection();
                    preparedStatement = connection.prepareStatement(query);

                    preparedStatement.setString(1, categoryRule.getDescription());
                    preparedStatement.setString(2, categoryRule.getiBAN());
                    preparedStatement.setString(3, categoryRule.getType());
                    preparedStatement.setInt(4, categoryRule.getCategory().getId());
                    preparedStatement.setInt(5, id);
                    preparedStatement.setString(6, sessionID);

                    if (preparedStatement.executeUpdate() == 1) {
                        connection.commit();
                        return getCategoryRule(headerSessionID, querySessionID, id, response);
                    } else {
                        response.setStatus(404);
                        return null;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    response.setStatus(500);
                    return null;
                } finally {
                    DBUtil.executeCommit(connection);
                    DbUtils.closeQuietly(preparedStatement);
                    DbUtils.closeQuietly(connection);
                }

            } else {
                response.setStatus(404);
                return null;
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            response.setStatus(405);
            return null;
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteCategoryRule(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                                   @RequestParam(value = "session_id", required = false) String querySessionID,
                                   @PathVariable("id") int id,
                                   HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;
        String query = "DELETE FROM category_rules WHERE category_rule_id = ? AND session_id = ?";
        DBUtil.executeDelete(response, query, id, sessionID);

    }

    private void applyCategoryRuleOnHistory(CategoryRule categoryRule, String sessionID) {
        String query = "UPDATE transactions SET category_id = ? WHERE session_id = ?" +
                "AND (? = '' OR description = ?)" +
                "AND (? = '' OR external_iban = ?)" +
                "AND (? = '' OR type = ?);";
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DBConnection.instance.getConnection();
            preparedStatement = connection.prepareStatement(query);

            preparedStatement.setInt(1, categoryRule.getCategory().getId());
            preparedStatement.setString(2, sessionID);
            preparedStatement.setString(3, categoryRule.getDescription());
            preparedStatement.setString(4, categoryRule.getDescription());
            preparedStatement.setString(5, categoryRule.getiBAN());
            preparedStatement.setString(6, categoryRule.getiBAN());
            preparedStatement.setString(7, categoryRule.getType());
            preparedStatement.setString(8, categoryRule.getType());

            if (preparedStatement.executeUpdate() == -1) {
                throw new SQLException("Update went wrong");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.executeCommit(connection);
            DbUtils.closeQuietly(preparedStatement);
            DbUtils.closeQuietly(connection);
        }
    }
}

class CategoryRuleAdapter implements JsonDeserializer<CategoryRule>, JsonSerializer<CategoryRule> {

    @Override
    public CategoryRule deserialize(JsonElement json, java.lang.reflect.Type type,
                                   JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        
        

        String description = jsonObject.has("description") ? jsonObject.get("description").getAsString() : null;
        String iBAN = jsonObject.has("iBAN") ? jsonObject.get("iBAN").getAsString() : null;
        String transactionType = jsonObject.has("type") ? jsonObject.get("type").getAsString() : null;
        Category category = new Category(jsonObject.has("category_id") ? jsonObject.get("category_id").getAsInt() : null, null);
        Boolean applyOnHistory = jsonObject.has("applyOnHistory") ? jsonObject.get("applyOnHistory").getAsBoolean() : null;

        return new CategoryRule(null, description, iBAN, transactionType, category, applyOnHistory);
    }

    @Override
    public JsonElement serialize(CategoryRule categoryRule, java.lang.reflect.Type type,
                                 JsonSerializationContext jsonSerializationContext) {

        JsonObject object = new JsonObject();

        object.addProperty("id", categoryRule.getId());
        object.addProperty("description", categoryRule.getDescription());
        object.addProperty("iBAN", categoryRule.getiBAN());
        object.addProperty("type", (categoryRule.getType()));
        object.addProperty("category_id", categoryRule.getCategory().getId());
        object.addProperty("applyOnHistory", categoryRule.getApplyOnHistory());

        return object;
    }
}
