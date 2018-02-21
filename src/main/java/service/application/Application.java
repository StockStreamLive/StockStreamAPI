package service.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import service.application.spring.AppContext;

@Slf4j
public class Application {

    private static AnnotationConfigWebApplicationContext initApplicationContext(final int port) throws Throwable {
        final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(AppContext.class);
        context.refresh();

        final APIService sparkServer = context.getBean(APIService.class);
        sparkServer.startServer(port);

        return context;
    }

    public static void main(final String[] args) throws Throwable {
        log.info("Application initialized");

        final int port = Integer.valueOf(System.getenv("PORT"));

        final AnnotationConfigWebApplicationContext context = initApplicationContext(port);
    }

}
