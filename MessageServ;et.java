import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/message")
public class MessageServlet extends HttpServlet {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private final MessageValidator validator = MessageValidator.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null) {
            response.setStatus(403);
            return;
        }

        try (Connection conn = dbManager.getConnection()) {

            String userCheckSQL = "SELECT * FROM users WHERE username = ? AND password = ?";
            try (PreparedStatement userStmt = conn.prepareStatement(userCheckSQL)) {
                userStmt.setString(1, username);
                userStmt.setString(2, password);
                ResultSet userRs = userStmt.executeQuery();
                if (!userRs.next()) {
                    response.setStatus(403);
                    return;
                }
            }

            String sql = "SELECT message FROM messages WHERE receiver = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                List<String> messages = new ArrayList<>();
                while (rs.next()) {
                    messages.add(rs.getString("message"));
                }

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getWriter(), messages);
            }
        } catch (Exception e) {
            response.setStatus(500);
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String receiver = request.getParameter("username");
        String message = request.getParameter("message");

        if (receiver == null || message == null || !validator.isValid(message)) {
            response.setStatus(403);
            return;
        }

        try (Connection conn = dbManager.getConnection()) {
            String userCheckSQL = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement userStmt = conn.prepareStatement(userCheckSQL)) {
                userStmt.setString(1, receiver);
                ResultSet userRs = userStmt.executeQuery();
                if (!userRs.next()) {
                    response.setStatus(403);
                    return;
                }
            }

            String insertSQL = "INSERT INTO messages (receiver, message) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                stmt.setString(1, receiver);
                stmt.setString(2, message);
                stmt.executeUpdate();
            }

            response.setStatus(200);
        } catch (Exception e) {
            response.setStatus(500);
            e.printStackTrace();
        }
    }
}
