import javax.persistence.Entity;

@Entity
class UserRecord {
    private String username;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
