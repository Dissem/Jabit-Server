package ch.dissem.bitmessage.server;

import ch.dissem.bitmessage.BitmessageContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.networking.DefaultNetworkHandler;
import ch.dissem.bitmessage.ports.MemoryNodeRegistry;
import ch.dissem.bitmessage.repository.JdbcAddressRepository;
import ch.dissem.bitmessage.repository.JdbcConfig;
import ch.dissem.bitmessage.repository.JdbcInventory;
import ch.dissem.bitmessage.repository.JdbcMessageRepository;
import ch.dissem.bitmessage.security.bc.BouncySecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Set;

import static ch.dissem.bitmessage.server.Constants.*;
import static java.util.stream.Collectors.toSet;

@Configuration
public class JabitServerConfig {
    public static final int SHORTLIST_SIZE = 5;

    @Value("${bitmessage.port}")
    private int port;
    @Value("${bitmessage.connection.ttl.hours}")
    private int connectionTTL;
    @Value("${bitmessage.connection.limit}")
    private int connectionLimit;

    @Bean
    public BitmessageContext bitmessageContext() {
        JdbcConfig config = new JdbcConfig("jdbc:h2:file:./jabit;AUTO_SERVER=TRUE", "sa", null);
        return new BitmessageContext.Builder()
                .addressRepo(new JdbcAddressRepository(config))
                .inventory(new JdbcInventory(config))
                .messageRepo(new JdbcMessageRepository(config))
                .nodeRegistry(new MemoryNodeRegistry())
                .networkHandler(new DefaultNetworkHandler())
                .objectListener(new ServerObjectListener(admins(), clients(), whitelist(), shortlist(), blacklist()))
                .security(new BouncySecurity())
                .port(port)
                .connectionLimit(connectionLimit)
                .connectionTTL(connectionTTL)
                .build();
    }

    @Bean
    public Set<BitmessageAddress> admins() {
        return Utils.readOrCreateList(
                ADMIN_LIST,
                "# Admins can send commands to the server.\n"
        ).stream().map(BitmessageAddress::new).collect(toSet());
    }

    @Bean
    public Set<BitmessageAddress> clients() {
        return Utils.readOrCreateList(
                CLIENT_LIST,
                "# Clients may send incomplete objects for proof of work.\n"
        ).stream().map(BitmessageAddress::new).collect(toSet());
    }

    @Bean
    public Set<String> whitelist() {
        return Utils.readOrCreateList(
                WHITELIST,
                "# If there are any Bitmessage addresses in the whitelist, only those will be shown.\n" +
                        "# blacklist.conf will be ignored, but shortlist.conf will be applied to whitelisted addresses.\n"
        );
    }

    @Bean
    public Set<String> shortlist() {
        return Utils.readOrCreateList(
                SHORTLIST,
                "# Broadcasts of these addresses will be restricted to the last " + SHORTLIST_SIZE + " entries.\n\n" +
                        "# Time Service:\n" +
                        "BM-BcbRqcFFSQUUmXFKsPJgVQPSiFA3Xash\n\n" +
                        "# Q's Aktivlist:\n" +
                        "BM-GtT7NLCCAu3HrT7dNTUTY9iDns92Z2ND\n"
        );
    }

    @Bean
    public Set<String> blacklist() {
        return Utils.readOrCreateList(
                BLACKLIST,
                "# Bitmessage addresses in this file are being ignored and their broadcasts won't be returned.\n"
        );
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .build();
    }
}
