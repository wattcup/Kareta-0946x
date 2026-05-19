import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.gr0946x.db.DatabaseConfig;
import ru.gr0946x.db.service.MessageService;
import ru.gr0946x.db.service.UserService;
import ru.gr0946x.net.Server;

import java.sql.SQLException;

void main() throws SQLException {
    var context = new AnnotationConfigApplicationContext(DatabaseConfig.class);

    UserService userService = context.getBean(UserService.class);
    MessageService messageService = context.getBean(MessageService.class);

    org.h2.tools.Server.createWebServer("-web", "-webPort", "8082").start();

    var s = new Server(9460, userService, messageService);

}