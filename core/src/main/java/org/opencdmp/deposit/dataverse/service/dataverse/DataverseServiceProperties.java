package org.opencdmp.deposit.dataverse.service.dataverse;

import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dataverse")
public class DataverseServiceProperties {

    private String logo;

    private String domain;

    private String alias;

    private DepositConfiguration depositConfiguration;

    private String licenseReferenceCode;

    private String organizationReferenceCode;

    private String funderReferenceCode;

    private String researcherReferenceCode;

    private int maxInMemorySizeInBytes;

    private String datasetType;

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public DepositConfiguration getDepositConfiguration() {
        return depositConfiguration;
    }

    public void setDepositConfiguration(DepositConfiguration depositConfiguration) {
        this.depositConfiguration = depositConfiguration;
    }

    public String getLicenseReferenceCode() {
        return licenseReferenceCode;
    }

    public void setLicenseReferenceCode(String licenseReferenceCode) {
        this.licenseReferenceCode = licenseReferenceCode;
    }

    public String getOrganizationReferenceCode() {
        return organizationReferenceCode;
    }

    public void setOrganizationReferenceCode(String organizationReferenceCode) {
        this.organizationReferenceCode = organizationReferenceCode;
    }

    public String getFunderReferenceCode() {
        return funderReferenceCode;
    }

    public void setFunderReferenceCode(String funderReferenceCode) {
        this.funderReferenceCode = funderReferenceCode;
    }

    public String getResearcherReferenceCode() {
        return researcherReferenceCode;
    }

    public void setResearcherReferenceCode(String researcherReferenceCode) {
        this.researcherReferenceCode = researcherReferenceCode;
    }

    public int getMaxInMemorySizeInBytes() {
        return maxInMemorySizeInBytes;
    }

    public void setMaxInMemorySizeInBytes(int maxInMemorySizeInBytes) {
        this.maxInMemorySizeInBytes = maxInMemorySizeInBytes;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }
}
