package nl.scanfie.example;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

public abstract class AbstractIntegrationTest {

    @BeforeSuite(alwaysRun = true)
    public void startIntegrationTestEnvironment() {
        IntegrationTestEnvironment.start();
    }

    @AfterSuite(alwaysRun = true)
    public void stopIntegrationTestEnvironment() {
        IntegrationTestEnvironment.stop();
    }

    protected String baseUrl() {
        return IntegrationTestEnvironment.baseUrl();
    }
}