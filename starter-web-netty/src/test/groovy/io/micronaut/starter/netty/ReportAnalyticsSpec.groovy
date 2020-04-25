package io.micronaut.starter.netty

import edu.umd.cs.findbugs.annotations.NonNull
import io.micronaut.context.annotation.Property
import io.micronaut.context.env.Environment
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.ServiceHttpClientConfiguration
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.starter.analytics.Generated
import io.micronaut.starter.options.BuildTool
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(
        name = "micronaut.http.services.analytics.url",
        value =  "http://localhost:8080/analytics/report")
class ReportAnalyticsSpec extends Specification {

    @Inject
    @Shared
    Environment environment

    @Inject
    @Shared
    EmbeddedServer embeddedServer

    @Inject
    CreateControllerSpec.CreateClient client

    @Inject
    AnalyticsController controller

    def setupSpec() {
        environment.addPropertySource("test", [
                "micronaut.http.services.analytics.url": "$embeddedServer.URL/analytics/report"
        ])
    }

    void "test report analytics"() {
        when:
        client.createApp("test", Collections.emptyList(), BuildTool.MAVEN, null, null)
        PollingConditions conditions = new PollingConditions()

        then:
        conditions.eventually {
            controller.generated.buildTool == BuildTool.MAVEN
        }
    }


    @Controller('/')
    @Singleton
    static class AnalyticsController {
        Generated generated
        @Post("/analytics/report")
        CompletableFuture<HttpStatus> applicationGenerated(@NonNull @Body Generated generated) {
            this.generated = generated
            return CompletableFuture.completedFuture(HttpStatus.OK)
        }
    }

    @Singleton
    static class ServiceConfigurer implements BeanCreatedEventListener<ServiceHttpClientConfiguration> {

        final Provider<EmbeddedServer> embeddedServer

        ServiceConfigurer(Provider<EmbeddedServer> embeddedServer) {
            this.embeddedServer = embeddedServer
        }

        @Override
        ServiceHttpClientConfiguration onCreated(BeanCreatedEvent<ServiceHttpClientConfiguration> event) {
            def config = event.getBean()
            if (config.serviceId == 'analytics') {
                config.setUrl(embeddedServer.get().URI)
            }
            return config
        }
    }
}
