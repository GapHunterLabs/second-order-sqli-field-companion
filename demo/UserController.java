import org.springframework.web.bind.annotation.PostMapping;

class UserController {
    @PostMapping("/user")
    void create(String username) {
        UserRecord record = new UserRecord();
        record.setUsername(username);
    }
}
