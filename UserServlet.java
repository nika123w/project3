package com.messenger;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/user")
public class UserServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}

@WebServlet("/message")
public class MessageServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String message = request.getParameter("message");

        if (username == null || message == null || username.isEmpty() || message.isEmpty() || message.contains("\n")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement checkUser = conn.prepareStatement("SELECT username FROM users WHERE username = ?");
             PreparedStatement insertMessage = conn.prepareStatement("INSERT INTO messages (receiver, message) VALUES (?, ?)");) {
            checkUser.setString(1, username);
            ResultSet rs = checkUser.executeQuery();
            if (!rs.next()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            insertMessage.setString(1, username);
            insertMessage.setString(2, message);
            insertMessage.executeUpdate();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement userCheck = conn.prepareStatement("SELECT username FROM users WHERE username = ? AND password = ?");
             PreparedStatement getMessages = conn.prepareStatement("SELECT message FROM messages WHERE receiver = ?");) {
            userCheck.setString(1, username);
            userCheck.setString(2, password);
            ResultSet userRs = userCheck.executeQuery();
            if (!userRs.next()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            getMessages.setString(1, username);
            ResultSet msgRs = getMessages.executeQuery();
            List<String> messages = new ArrayList<>();
            while (msgRs.next()) {
                messages.add(msgRs.getString("message"));
            }
            response.setContentType("application/json");
            new ObjectMapper().writeValue(response.getOutputStream(), messages);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
