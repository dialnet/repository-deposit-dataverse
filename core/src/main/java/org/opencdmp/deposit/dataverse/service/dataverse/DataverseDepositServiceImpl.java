package org.opencdmp.deposit.dataverse.service.dataverse;


import gr.cite.tools.exception.MyApplicationException;
import gr.cite.tools.logging.LoggerService;
import gr.cite.tools.logging.MapLogEntry;
import org.opencdmp.commonmodels.models.FileEnvelopeModel;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.commonmodels.models.plugin.PluginUserFieldModel;
import org.opencdmp.deposit.dataverse.model.DataverseDataset;
import org.opencdmp.deposit.dataverse.model.builder.DataverseBuilder;
import org.opencdmp.deposit.dataverse.service.storage.FileStorageService;
import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.opencdmp.depositbase.repository.PlanDepositModel;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

@Component
public class DataverseDepositServiceImpl implements DataverseDepositService {
    private static final LoggerService logger = new LoggerService(LoggerFactory.getLogger(DataverseDepositServiceImpl.class));

    public static final String CONFIGURATION_FIELD_ACCESS_TOKEN = "dataverse-access-token";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DataverseServiceProperties dataverseServiceProperties;
    private final DataverseBuilder dataverseBuilder;
    private final FileStorageService storageService;
    private final ResourceLoader resourceLoader;

    private byte[] logo;;
    
    @Autowired
    public DataverseDepositServiceImpl(DataverseServiceProperties dataverseServiceProperties, DataverseBuilder mapper, FileStorageService storageService, ResourceLoader resourceLoader){
        this.dataverseServiceProperties = dataverseServiceProperties;
        this.dataverseBuilder = mapper;
	    this.storageService = storageService;
        this.resourceLoader = resourceLoader;
        this.logo = null;
    }

    @Override
    public String deposit(PlanDepositModel planDepositModel) {
        return this.deposit(planDepositModel, null);
    }

    @Override
    public String deposit(PlanDepositModel planDepositModel, String collectionAliasOverride) {

        DepositConfiguration depositConfiguration = this.getConfiguration();

        if(depositConfiguration != null && planDepositModel != null && planDepositModel.getPlanModel() != null) {

            String token = null;
            if (planDepositModel.getAuthInfo() != null) {
                if (planDepositModel.getAuthInfo().getAuthToken() != null && !planDepositModel.getAuthInfo().getAuthToken().isBlank()) token = planDepositModel.getAuthInfo().getAuthToken();
                else if (planDepositModel.getAuthInfo().getAuthFields() != null && !planDepositModel.getAuthInfo().getAuthFields().isEmpty() && depositConfiguration.getUserConfigurationFields() != null) {
                    PluginUserFieldModel userFieldModel = planDepositModel.getAuthInfo().getAuthFields().stream().filter(x -> x.getCode().equals(CONFIGURATION_FIELD_ACCESS_TOKEN)).findFirst().orElse(null);
                    if (userFieldModel != null && userFieldModel.getTextValue() != null && !userFieldModel.getTextValue().isBlank()) token = userFieldModel.getTextValue();
                }
            }
            if (token == null || token.isBlank()) token = this.dataverseServiceProperties.getDepositConfiguration().getAccessToken();
            String previousDOI = planDepositModel.getPlanModel().getPreviousDOI();

            try {

                if (previousDOI == null) {
                    return depositFirst(planDepositModel.getPlanModel(), token, collectionAliasOverride);
                } else {
                    return depositNewVersion(planDepositModel.getPlanModel(), previousDOI, token);
                }

            } catch (HttpClientErrorException | HttpServerErrorException ex) {
                logger.error(ex.getMessage(), ex);
                Map<String, String> parsedException = objectMapper.readValue(ex.getResponseBodyAsString(), Map.class);
                try {
                    throw new IOException(parsedException.get("message"), ex);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        return null;

    }


    private String depositFirst(PlanModel planModel, String token, String collectionAliasOverride) {
        DataverseDataset dataset = this.dataverseBuilder.build(planModel);

        String collectionAlias = (collectionAliasOverride != null && !collectionAliasOverride.isBlank())
                ? collectionAliasOverride
                : this.dataverseBuilder.buildDataverseIdentifier(planModel);

        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "dataverses/" + collectionAlias + "/datasets?doNotValidate=true";

        Map<String, Object> response = this.getWebClient().post().uri(url).headers(httpHeaders -> {
                    httpHeaders.set("X-Dataverse-key", token);
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(dataset).exchangeToMono(mono ->
                        mono.statusCode().isError() ?
                                mono.createException().flatMap(Mono::error) :
                                mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();

        response = (Map<String, Object>) response.get("data");
        String doi = String.valueOf(response.get("persistentId"));

        this.uploadFiles(planModel, doi, token);
        this.publish(doi, token);
        return doi;
    }

    private void uploadFiles(PlanModel planModel, String doi, String token) {
        if (planModel.getPdfFile() != null) this.uploadFile(planModel.getPdfFile(), doi, token);
        if (planModel.getRdaJsonFile() != null) this.uploadFile(planModel.getRdaJsonFile(), doi, token);
        if (planModel.getSupportingFilesZip() != null) this.uploadFile(planModel.getSupportingFilesZip(), doi, token);
    }

    private void uploadFile(FileEnvelopeModel fileEnvelopeModel, String doi, String token) {

        if (fileEnvelopeModel == null) return;

        byte[] fileBytes = null;
        if (this.getConfiguration().isUseSharedStorage() && fileEnvelopeModel.getFileRef() != null && !fileEnvelopeModel.getFileRef().isBlank()) {
            fileBytes = this.storageService.readFile(fileEnvelopeModel.getFileRef());
        }
        if (fileBytes == null || fileBytes.length == 0){
            fileBytes = fileEnvelopeModel.getFile();
        }


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Dataverse-key", token);
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        ContentDisposition contentDisposition = ContentDisposition
                .builder("form-data")
                .name("file")
                .filename(sanitizeFilename(fileEnvelopeModel.getFilename()))
                .build();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(fileBytes, fileMap);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileEntity);
        body.add("jsonData", "{\"restrict\":\"false\", \"tabIngest\":\"false\"}");
        HttpEntity<MultiValueMap<String, Object>> requestEntity
                = new HttpEntity<>(body, headers);

        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "datasets/:persistentId/add?persistentId=" + doi;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Object> resp = restTemplate.postForEntity(url, requestEntity, Object.class);
    }

    /**
     * Dataverse validates the file label (FileMetadata) and rejects names containing
     * characters such as : / \ * ? " < > | ; # & ~ (the file name is derived from the plan
     * title, which may include them), causing a 400 "Failed to add file to dataset". Replace
     * any forbidden/control character with '_' so the upload always passes validation.
     */
    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "deposit-file";
        String sanitized = filename.replaceAll("[\\\\/:*?\"<>|;#&~\\p{Cntrl}]", "_").trim();
        return sanitized.isBlank() ? "deposit-file" : sanitized;
    }

    private void deleteFile(int fileId, String token){
        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "files/" + fileId;

        this.getWebClient().delete().uri(url).headers(httpHeaders -> {
                    httpHeaders.set("X-Dataverse-key", token);
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                }).exchangeToMono(mono ->
                mono.statusCode().isError() ?
                        mono.createException().flatMap(Mono::error) :
                        mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })).block();
    }

    private void publish(String doi, String token) {

        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "datasets/:persistentId/actions/:publish?persistentId=" + doi + "&type=major";

        Map<String, Object> publishResponse = this.getWebClient().post().uri(url).headers(httpHeaders -> {
            httpHeaders.set("X-Dataverse-key", token);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        }).exchangeToMono(mono ->
                mono.statusCode().isError() ?
                        mono.createException().flatMap(Mono::error) :
                        mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })).block();

        if (publishResponse == null) throw new UnsupportedOperationException("Failed to publish to Dataverse");
    }

    private String depositNewVersion(PlanModel planModel, String previousDOI, String token) {
        DataverseDataset dataset = this.dataverseBuilder.build(planModel);

        String url = this.dataverseServiceProperties.getDepositConfiguration().getRepositoryUrl() + "datasets/:persistentId/versions/:draft?persistentId=" + previousDOI;

        Map<String, Object> response = this.getWebClient().put().uri(url).headers(httpHeaders -> {
                    httpHeaders.set("X-Dataverse-key", token);
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(dataset.getDatasetVersion()).exchangeToMono(mono ->
                        mono.statusCode().isError() ?
                                mono.createException().flatMap(Mono::error) :
                                mono.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})).block();

        if (response == null) throw new MyApplicationException("cannot create new draft version");

        JsonNode jsonNode = objectMapper.convertValue(response, JsonNode.class);
        JsonNode files = jsonNode.get("data").get("files");

        if (files.isArray()) {
            for (JsonNode file : files) {
                int fileId = file.get("dataFile").get("id").asInt();
                this.deleteFile(fileId, token);
            }
        }

        this.uploadFiles(planModel, previousDOI, token);
        this.publish(previousDOI, token);
        return previousDOI;
    }


    @Override
    public DepositConfiguration getConfiguration() {
        return this.dataverseServiceProperties.getDepositConfiguration();
    }
    
    @Override
    public String authenticate(String code){
        return null;
    }

    @Override
    public String getLogo() {
        DepositConfiguration dataverseConfig = this.dataverseServiceProperties.getDepositConfiguration();
        if(dataverseConfig != null && dataverseConfig.isHasLogo() && this.dataverseServiceProperties.getLogo() != null && !this.dataverseServiceProperties.getLogo().isBlank()) {
            if (this.logo == null) {
                try {
                    Resource resource = resourceLoader.getResource(this.dataverseServiceProperties.getLogo());
                    if(!resource.isReadable()) return null;
                    try(InputStream inputStream = resource.getInputStream()) {
                        this.logo = inputStream.readAllBytes();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
            return (this.logo != null && this.logo.length != 0) ? Base64.getEncoder().encodeToString(this.logo) : null;
        }
        return null;
    }
    
    private WebClient getWebClient(){
        return WebClient.builder().filters(exchangeFilterFunctions -> {
            exchangeFilterFunctions.add(logRequest());
            exchangeFilterFunctions.add(logResponse());
        }).codecs(codecs -> codecs
                .defaultCodecs()
                .maxInMemorySize(this.dataverseServiceProperties.getMaxInMemorySizeInBytes())
        ).build();
    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.debug(new MapLogEntry("Request").And("method", clientRequest.method().toString()).And("url", clientRequest.url().toString()));
            return Mono.just(clientRequest);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.mutate().build().bodyToMono(String.class)
                    .flatMap(body -> {
                        logger.error(new MapLogEntry("Response").And("method", response.request().getMethod().toString()).And("url", response.request().getURI()).And("status", response.statusCode().toString()).And("body", body));
                        return Mono.just(response);
                    });
            }
            return Mono.just(response);
            
        });
    }
}
