package rocks.devmesh.spring.cloud.zuul.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rocks.devmesh.spring.cloud.zuul.filters.Healthcheck;
import rocks.devmesh.spring.cloud.zuul.filters.Routes;
import rocks.devmesh.spring.cloud.zuul.filters.ZuulResponseFilter;

@Configuration
public class ZuulFilterConfiguration {

    @Bean
    public Healthcheck healthcheck() {
        return new Healthcheck();
    }

    @Bean
    public Routes routes() {
        return new Routes();
    }

    @Bean
    public ZuulResponseFilter zuulResponseFilter() {
        return new ZuulResponseFilter();
    }
}
