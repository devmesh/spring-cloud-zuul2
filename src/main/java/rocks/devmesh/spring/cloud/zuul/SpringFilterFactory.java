package rocks.devmesh.spring.cloud.zuul;

import com.netflix.zuul.FilterFactory;
import com.netflix.zuul.filters.ZuulFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;

@AllArgsConstructor
public class SpringFilterFactory implements FilterFactory {

    private final ApplicationContext applicationContext;

    @Override
    public ZuulFilter newInstance(Class clazz) throws Exception {
        return (ZuulFilter) applicationContext.getBean(clazz);
    }
}
