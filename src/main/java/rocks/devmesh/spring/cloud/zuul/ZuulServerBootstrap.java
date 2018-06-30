package rocks.devmesh.spring.cloud.zuul;

import com.netflix.zuul.netty.server.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ZuulServerBootstrap implements CommandLineRunner {

    @Autowired
    private Server server;

    @Override
    public void run(String... args) throws Exception {
        log.info("Zuul Sample: starting up.");
        long startTime = System.currentTimeMillis();
        int exitCode = 0;

        try {
            long startupDuration = System.currentTimeMillis() - startTime;
            log.info("Zuul Sample: finished startup. Duration = " + startupDuration + " ms");

            server.start(true);
        } catch (Throwable t) {
            log.error("Zuul Sample: initialization failed. Forcing shutdown now.", t);
            exitCode = 1;
        } finally {
            // server shutdown
            if (server != null) server.stop();

            System.exit(exitCode);
        }
    }
}