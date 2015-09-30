package ch.dissem.bitmessage.server;

import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.networking.DefaultNetworkHandler;
import ch.dissem.bitmessage.ports.MemoryNodeRegistry;
import ch.dissem.bitmessage.repository.JdbcAddressRepository;
import ch.dissem.bitmessage.repository.JdbcConfig;
import ch.dissem.bitmessage.repository.JdbcInventory;
import ch.dissem.bitmessage.repository.JdbcMessageRepository;
import ch.dissem.bitmessage.security.bc.BouncySecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@EnableAutoConfiguration
public class JabitServerApplication {
    private BitmessageContext ctx;

    @RequestMapping("status")
    public String status() {
        return ctx.status().toString();
    }

    @RequestMapping("read/{broadcastAddress}")
    public List<Plaintext> read(@PathVariable String broadcastAddress) {
        BitmessageAddress broadcaster = ctx.addresses().getAddress(broadcastAddress);
        if (broadcaster == null) {
            broadcaster = new BitmessageAddress(broadcastAddress);
        }
        if (!broadcaster.isSubscribed()) {
            ctx.addSubscribtion(broadcaster);
        }
        return ctx.messages().findMessages(broadcaster);
    }

    public JabitServerApplication() {
        JdbcConfig config = new JdbcConfig();
        ctx = new BitmessageContext.Builder()
                .addressRepo(new JdbcAddressRepository(config))
                .inventory(new JdbcInventory(config))
                .messageRepo(new JdbcMessageRepository(config))
                .nodeRegistry(new MemoryNodeRegistry())
                .networkHandler(new DefaultNetworkHandler())
                .security(new BouncySecurity())
                .port(8445)
                .build();
        ctx.startup(plaintext -> {
        });
    }

    public static void main(String[] args) {
        SpringApplication.run(JabitServerApplication.class, args);
    }
}
