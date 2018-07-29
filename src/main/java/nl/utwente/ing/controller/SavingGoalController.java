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

import nl.utwente.ing.controller.database.DBUtil;
import nl.utwente.ing.model.SavingGoal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/savingGoals")
public class SavingGoalController {

    @RequestMapping(value = "", method = RequestMethod.GET)
    public SavingGoal getAllSavingGoals(@RequestHeader(value = "X-session-id", required = false) String
                                                    headerSessionID,
                                        @RequestParam(value = "session_id", required = false) String paramSessionID,
                                        HttpServletResponse response) {
        String sessionID = headerSessionID == null ? paramSessionID : headerSessionID;



        return null;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SavingGoal createSavingGoal(@RequestHeader(value = "X-session-id", required = false) String
                                                   headerSessionID,
                                       @RequestParam(value = "session_id", required = false) String paramSessionID,
                                       @RequestBody String body,
                                       HttpServletResponse response) {
        String sessionID = headerSessionID == null ? paramSessionID : headerSessionID;
        return null;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteCategory(@RequestHeader(value = "X-session-ID", required = false) String headerSessionID,
                               @RequestParam(value = "session_id", required = false) String querySessionID,
                               @PathVariable("id") int id,
                               HttpServletResponse response) {
        String sessionID = headerSessionID == null ? querySessionID : headerSessionID;
        String query = "DELETE FROM saving_goals WHERE saving_goal_id = ? AND session_id = ?";
        DBUtil.executeDelete(response, query, id, sessionID);
    }
}
