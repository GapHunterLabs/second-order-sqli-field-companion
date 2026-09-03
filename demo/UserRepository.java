import java.sql.Statement;

class UserRepository {
    void find(Statement stmt, UserRecord record) throws Exception {
        stmt.executeQuery("SELECT * FROM users WHERE name = '" + record.getUsername() + "'");
    }
}
