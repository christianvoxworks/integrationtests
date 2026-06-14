package nl.scanfie.example;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

final class IntegrationTestEnvironment {

    private static final Network network = Network.newNetwork();

    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("example_test")
            .withUsername("test")
            .withPassword("test");

    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7")
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(6379)
            .waitingFor(
                    Wait.forListeningPort()
                            .withStartupTimeout(Duration.ofMinutes(1))
            );

    private static final GenericContainer<?> tomcat = new GenericContainer<>("tomcat:10.1-jdk17")
            .withNetwork(network)
            .withExposedPorts(8080)
            .dependsOn(postgres)
            .withCopyFileToContainer(
                    MountableFile.forHostPath("target/circleci-testcontainers-example-0.0.1-SNAPSHOT.war"),
                    "/usr/local/tomcat/webapps/integrationtests.war"
            )
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/example_test")
            .withEnv("SPRING_DATASOURCE_USERNAME", "test")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "test")
            .withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.postgresql.Driver")
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "create-drop")
            .withEnv("SPRING_JPA_PROPERTIES_HIBERNATE_HBM2DDL_AUTO", "create-drop")
            .withEnv("SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", "org.hibernate.dialect.PostgreSQLDialect")
            .withEnv("SPRING_DATA_REDIS_HOST", "redis")
            .withEnv("SPRING_DATA_REDIS_PORT", "6379")
            .waitingFor(
                    Wait.forHttp("/integrationtests")
                            .forStatusCodeMatching(statusCode -> statusCode < 500)
                            .withStartupTimeout(Duration.ofMinutes(2))
            );

    private IntegrationTestEnvironment() {
    }

    static synchronized void start() {
        if (!postgres.isRunning()) {
            postgres.start();
        }

        if (!redis.isRunning()) {
            redis.start();
        }

        if (!tomcat.isRunning()) {
            tomcat.start();
        }
    }

    static synchronized void stop() {
        if (tomcat.isRunning()) {
            tomcat.stop();
        }

        if (redis.isRunning()) {
            redis.stop();
        }

        if (postgres.isRunning()) {
            postgres.stop();
        }

        network.close();
    }

    static String baseUrl() {
        if (!tomcat.isRunning()) {
            throw new IllegalStateException("Tomcat container is not running");
        }

        return "http://" + tomcat.getHost() + ":" + tomcat.getMappedPort(8080) + "/integrationtests";
    }
}