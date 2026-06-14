package nl.scanfie.example;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

final class IntegrationTestEnvironment {

    private static final Network network = Network.newNetwork();

    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withNetwork(network)
            .withNetworkAliases("mysql")
            .withDatabaseName("example_test")
            .withUsername("test")
            .withPassword("test");

    private static final GenericContainer<?> tomcat = new GenericContainer<>("tomcat:10.1-jdk17")
            .withNetwork(network)
            .withExposedPorts(8080)
            .dependsOn(mysql)
            .withCopyFileToContainer(
                    MountableFile.forHostPath("target/circleci-testcontainers-example-0.0.1-SNAPSHOT.war"),
                    "/usr/local/tomcat/webapps/integrationtests.war"
            )
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

    private IntegrationTestEnvironment() {
    }

    static synchronized void start() {
        if (!mysql.isRunning()) {
            mysql.start();
        }

        if (!tomcat.isRunning()) {
            tomcat.start();
        }
    }

    static synchronized void stop() {
        if (tomcat.isRunning()) {
            tomcat.stop();
        }

        if (mysql.isRunning()) {
            mysql.stop();
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