package com.example.tenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

@Service
@Slf4j
public class SchemaProvisioningService {

    private final DataSource rawInvoiceDs;
    private final RestTemplate restTemplate;

    @Value("${spring.datasource.url}")
    private String invoiceDbUrl;

    @Value("${spring.datasource.username}")
    private String invoiceDbUser;

    @Value("${spring.datasource.password}")
    private String invoiceDbPassword;

    // Customer DB connection details (must be in application.properties)
    @Value("${customer.datasource.url}")
    private String customerDbUrl;

    @Value("${customer.datasource.username}")
    private String customerDbUser;

    @Value("${customer.datasource.password}")
    private String customerDbPassword;

    // Internal endpoints of other services
    @Value("${customer.service.internal.url:http://localhost:5679}")
    private String customerServiceUrl;

    @Value("${invoice.service.internal.url:http://localhost:5671}")
    private String invoiceServiceUrl;

    public SchemaProvisioningService(
            @Qualifier("rawDataSource") DataSource rawInvoiceDs,
            RestTemplate restTemplate) {
        this.rawInvoiceDs = rawInvoiceDs;
        this.restTemplate = restTemplate;
    }

    /**
     * Called after successful company registration.
     * Creates schemas in all databases and triggers table creation in each service.
     */
    public void provisionTenantSchema(String companyDomain) {
        String schemaName = TenantContext.toSchemaName(companyDomain);
        log.info("Provisioning tenant schema '{}' for domain '{}'", schemaName, companyDomain);

        // 1. Create schema in Invoice DB and create Login-service tables
        createSchema(rawInvoiceDs, schemaName);
        createTablesInSchema(buildTenantDs(invoiceDbUrl, invoiceDbUser, invoiceDbPassword, schemaName),
                "com.example.entity", schemaName);

        // 2. Create schema in Customer DB (trigger Customer-Service to create its tables)
        try (HikariDataSource customerRawDs = buildTenantDs(customerDbUrl, customerDbUser, customerDbPassword, null)) {
            createSchema(customerRawDs, schemaName);
        } catch (Exception e) {
            log.error("Failed to create schema in Customer DB for tenant {}: {}", schemaName, e.getMessage());
        }

        // 3. Tell Customer-Service to initialise its tables in the new schema
        notifyService(customerServiceUrl + "/internal/provision-schema/" + schemaName, "Customer-Service");

        // 4. Tell Invoice-Service to initialise its tables in the new schema
        notifyService(invoiceServiceUrl + "/internal/provision-schema/" + schemaName, "Invoice-Service");

        log.info("Tenant schema '{}' provisioned successfully.", schemaName);
    }

    private void createSchema(DataSource ds, String schemaName) {
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
            log.info("Schema '{}' created (or already exists).", schemaName);
        } catch (Exception e) {
            log.error("Could not create schema '{}': {}", schemaName, e.getMessage());
            throw new RuntimeException("Schema creation failed for: " + schemaName, e);
        }
    }

    private void createTablesInSchema(HikariDataSource tenantDs, String entityPackage, String schemaName) {
        try {
            LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
            emfBean.setDataSource(tenantDs);
            emfBean.setPackagesToScan(entityPackage);

            HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
            adapter.setGenerateDdl(true);
            emfBean.setJpaVendorAdapter(adapter);

            Properties props = new Properties();
            props.setProperty("hibernate.hbm2ddl.auto", "update");
            props.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            props.setProperty("hibernate.show_sql", "false");
            emfBean.setJpaProperties(props);

            emfBean.afterPropertiesSet();
            if (emfBean.getObject() != null) emfBean.getObject().close();

            log.info("Tables created in schema '{}' for package '{}'", schemaName, entityPackage);
        } catch (Exception e) {
            log.error("Table creation failed in schema '{}': {}", schemaName, e.getMessage());
        } finally {
            tenantDs.close();
        }
    }

    private void notifyService(String url, String serviceName) {
        try {
            restTemplate.postForEntity(url, null, String.class);
            log.info("{} schema provisioned via {}", serviceName, url);
        } catch (Exception e) {
            log.warn("{} provisioning call failed (service may be down): {}", serviceName, e.getMessage());
        }
    }

    private HikariDataSource buildTenantDs(String url, String user, String pass, String schemaName) {
        HikariConfig config = new HikariConfig();
        String finalUrl = schemaName != null
                ? (url.contains("?") ? url + "&currentSchema=" + schemaName : url + "?currentSchema=" + schemaName)
                : url;
        config.setJdbcUrl(finalUrl);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(2);
        config.setPoolName("ProvisionPool-" + (schemaName != null ? schemaName : "raw"));
        return new HikariDataSource(config);
    }
}
