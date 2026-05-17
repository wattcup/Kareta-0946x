import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.gr0946x.db.DatabaseConfig;
import ru.gr0946x.net.Server;

void main() {
    var context = new AnnotationConfigApplicationContext(DatabaseConfig.class);
    var s = new Server(9460);
}