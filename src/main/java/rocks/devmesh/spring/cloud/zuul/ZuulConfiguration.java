package rocks.devmesh.spring.cloud.zuul;

import com.netflix.appinfo.AbstractInstanceConfig;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.config.DynamicIntProperty;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.netty.common.channel.config.ChannelConfig;
import com.netflix.netty.common.channel.config.CommonChannelConfigKeys;
import com.netflix.netty.common.metrics.EventLoopGroupMetrics;
import com.netflix.netty.common.proxyprotocol.StripUntrustedProxyHeadersHandler;
import com.netflix.netty.common.status.ServerStatusManager;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Registry;
import com.netflix.zuul.BasicFilterUsageNotifier;
import com.netflix.zuul.FilterFactory;
import com.netflix.zuul.FilterLoader;
import com.netflix.zuul.FilterUsageNotifier;
import com.netflix.zuul.context.SessionContextDecorator;
import com.netflix.zuul.context.ZuulSessionContextDecorator;
import com.netflix.zuul.filters.FilterRegistry;
import com.netflix.zuul.filters.ZuulFilter;
import com.netflix.zuul.groovy.GroovyCompiler;
import com.netflix.zuul.netty.ratelimiting.NullChannelHandlerProvider;
import com.netflix.zuul.netty.server.BaseServerStartup;
import com.netflix.zuul.netty.server.ClientConnectionsShutdown;
import com.netflix.zuul.netty.server.Server;
import com.netflix.zuul.netty.server.ZuulDependencyKeys;
import com.netflix.zuul.netty.server.ZuulServerChannelInitializer;
import com.netflix.zuul.origins.BasicNettyOriginManager;
import com.netflix.zuul.origins.OriginManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class ZuulConfiguration {

    @Bean
    public Server server(DiscoveryClient discoveryClient, Registry registry, FilterLoader filterLoader, FilterUsageNotifier usageNotifier,
                         EventLoopGroupMetrics eventLoopGroupMetrics, ServerStatusManager serverStatusManager, SessionContextDecorator sessionContextDecorator) {
        ChannelConfig channelDeps = new ChannelConfig();

        ChannelGroup clientChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        ClientConnectionsShutdown clientConnectionsShutdown = new ClientConnectionsShutdown(clientChannels,
                GlobalEventExecutor.INSTANCE, discoveryClient);

        channelDeps.set(ZuulDependencyKeys.registry, registry);
        channelDeps.set(ZuulDependencyKeys.eventLoopGroupMetrics, eventLoopGroupMetrics);
        channelDeps.set(ZuulDependencyKeys.filterLoader, filterLoader);
        channelDeps.set(ZuulDependencyKeys.filterUsageNotifier, usageNotifier);
        channelDeps.set(ZuulDependencyKeys.sessionCtxDecorator, sessionContextDecorator);
        channelDeps.set(ZuulDependencyKeys.rateLimitingChannelHandlerProvider, new NullChannelHandlerProvider());
        channelDeps.set(ZuulDependencyKeys.sslClientCertCheckChannelHandlerProvider, new NullChannelHandlerProvider());

        return new Server(choosePortsAndChannels(clientChannels, channelDeps), serverStatusManager, clientConnectionsShutdown, eventLoopGroupMetrics);
    }

    private Map<Integer, ChannelInitializer> choosePortsAndChannels(ChannelGroup clientChannels, ChannelConfig channelDeps) {
        Map<Integer, ChannelInitializer> portsToChannels = new HashMap<>();

        int port = new DynamicIntProperty("zuul.server.port.main", 7001).get();

        ChannelConfig channelConfig = BaseServerStartup.defaultChannelConfig();

        channelConfig.set(CommonChannelConfigKeys.allowProxyHeadersWhen, StripUntrustedProxyHeadersHandler.AllowWhen.ALWAYS);
        channelConfig.set(CommonChannelConfigKeys.preferProxyProtocolForClientIp, false);
        channelConfig.set(CommonChannelConfigKeys.isSSlFromIntermediary, false);
        channelConfig.set(CommonChannelConfigKeys.withProxyProtocol, false);

        portsToChannels.put(port, new ZuulServerChannelInitializer(port, channelConfig, channelDeps, clientChannels));
        return portsToChannels;
    }

    @Bean
    public OriginManager originManager(Registry registry) {
        return new BasicNettyOriginManager(registry);
    }

    @Bean
    public SessionContextDecorator sessionContextDecorator(OriginManager originManager) {
        return new ZuulSessionContextDecorator(originManager);
    }

    @Bean
    public Registry registry() {
        return new DefaultRegistry();
    }

    @Bean
    public FilterLoader filterLoader(FilterFactory filterFactory, List<ZuulFilter> zuulFilters) {
        FilterLoader filterLoader = new FilterLoader(new FilterRegistry(), new GroovyCompiler(), filterFactory);
        zuulFilters.forEach(filter -> {
            try {
                filterLoader.putFilterForClassName(filter.getClass().getCanonicalName());
            } catch (Exception e) {
                throw new IllegalArgumentException("Can not init filter [" + filter.getClass().getName() + "]");
            }
        });
        return filterLoader;
    }

    @Bean
    public FilterFactory filterFactory(ApplicationContext applicationContext) {
        return new SpringFilterFactory(applicationContext);
    }

    @Bean
    public EurekaClientConfig eurekaClientConfig() {
        return new DefaultEurekaClientConfig();
    }

    @Bean
    public DiscoveryClient discoveryClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config) {
        return new DiscoveryClient(applicationInfoManager, config);
    }

    @Bean
    public EventLoopGroupMetrics eventLoopGroupMetrics(Registry registry) {
        return new EventLoopGroupMetrics(registry);
    }

    @Bean
    public ApplicationInfoManager.OptionalArgs optionalArgs() {
        return new ApplicationInfoManager.OptionalArgs();
    }

    @Bean
    public ApplicationInfoManager applicationInfoManager(EurekaInstanceConfig config, ApplicationInfoManager.OptionalArgs optionalArgs) {
        return new ApplicationInfoManager(config, optionalArgs);
    }

    @Bean
    public ServerStatusManager serverStatusManager(ApplicationInfoManager applicationInfoManager, DiscoveryClient discoveryClient) {
        return new ServerStatusManager(applicationInfoManager, discoveryClient);
    }

    @Bean
    public FilterUsageNotifier usageNotifier() {
        return new BasicFilterUsageNotifier();
    }

    @Bean
    public EurekaInstanceConfig eurekaInstanceConfig() {
        return new AbstractInstanceConfig() {

            @Override
            public Map<String, String> getMetadataMap() {
                return Collections.emptyMap();
            }

            @Override
            public String getInstanceId() {
                return "zuul-1";
            }

            @Override
            public String getAppname() {
                return "zuul";
            }

            @Override
            public String getAppGroupName() {
                return "infra";
            }

            @Override
            public String getStatusPageUrlPath() {
                return "localhost:8080";
            }

            @Override
            public String getStatusPageUrl() {
                return "localhost:8080";
            }

            @Override
            public String getHomePageUrlPath() {
                return "localhost:8080";
            }

            @Override
            public String getHomePageUrl() {
                return "localhost:8080";
            }

            @Override
            public String getHealthCheckUrlPath() {
                return "localhost:8080";
            }

            @Override
            public String getHealthCheckUrl() {
                return "localhost:8080";
            }

            @Override
            public String getSecureHealthCheckUrl() {
                return "localhost:8080";
            }

            @Override
            public String[] getDefaultAddressResolutionOrder() {
                return new String[0];
            }

            @Override
            public String getNamespace() {
                return "zuul";
            }
        };
    }
}