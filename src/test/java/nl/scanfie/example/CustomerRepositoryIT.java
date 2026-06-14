package nl.scanfie.example;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class CustomerRepositoryIT {

    private static final Network network = Network.newNetwork();

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withNetwork(network)
            .withNetworkAliases("mysql")
            .withDatabaseName("example_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> tomcat = new GenericContainer<>("tomcat:10.1-jdk17")
            .withNetwork(network)
            .withExposedPorts(8080)
            .dependsOn(mysql)
            .withCopyFileToContainer(
                    MountableFile.forHostPath("target/circleci-testcontainers-example-0.0.1-SNAPSHOT.war"),
                    "/usr/local/tomcat/webapps/integrationtests.war"
            )
            .withEnv("SPRING_PROFILES_ACTIVE", "it")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql:3306/example_test")
            .withEnv("SPRING_DATASOURCE_USERNAME", "test")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "test")
            .withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "com.mysql.cj.jdbc.Driver")
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "create-drop")
            .withEnv("SPRING_JPA_PROPERTIES_HIBERNATE_HBM2DDL_AUTO", "create-drop")
            .withEnv("SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", "org.hibernate.dialect.MySQLDialect")
            .waitingFor(
                    Wait.forHttp("/integrationtests")
                            .forStatusCodeMatching(statusCode -> statusCode < 500)
                            .withStartupTimeout(Duration.ofMinutes(2))
            );

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void shouldStartApplicationInTomcatWithMysql() throws Exception {
        String baseUrl = "http://" + tomcat.getHost() + ":" + tomcat.getMappedPort(8080) + "/integrationtests";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isLessThan(500);
    }
}