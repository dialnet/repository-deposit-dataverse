package org.opencdmp.deposit.dataverse.model.builder;

import gr.cite.tools.logging.LoggerService;
import org.opencdmp.commonmodels.enums.PlanAccessType;
import org.opencdmp.commonmodels.enums.PlanUserRole;
import org.opencdmp.commonmodels.models.PlanUserModel;
import org.opencdmp.commonmodels.models.description.*;
import org.opencdmp.commonmodels.models.descriptiotemplate.DefinitionModel;
import org.opencdmp.commonmodels.models.descriptiotemplate.FieldSetModel;
import org.opencdmp.commonmodels.models.descriptiotemplate.fielddata.RadioBoxDataModel;
import org.opencdmp.commonmodels.models.descriptiotemplate.fielddata.SelectDataModel;
import org.opencdmp.commonmodels.models.plan.PlanBlueprintValueModel;
import org.opencdmp.commonmodels.models.plan.PlanContactModel;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.commonmodels.models.planblueprint.SectionModel;
import org.opencdmp.commonmodels.models.planreference.PlanReferenceModel;
import org.opencdmp.commonmodels.models.reference.ReferenceFieldModel;
import org.opencdmp.commonmodels.models.reference.ReferenceModel;
import org.opencdmp.commonmodels.models.referencetype.ReferenceTypeFieldModel;
import org.opencdmp.commonmodels.models.referencetype.ReferenceTypeModel;
import org.opencdmp.deposit.dataverse.configuration.SemanticsProperties;
import org.opencdmp.deposit.dataverse.model.*;
import org.opencdmp.deposit.dataverse.service.dataverse.DataverseDepositServiceImpl;
import org.opencdmp.deposit.dataverse.service.dataverse.DataverseServiceProperties;
import org.opencdmp.deposit.dataverse.service.descriptiontemplatesearcher.TemplateFieldSearcherService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DataverseBuilder {
    private static final LoggerService logger = new LoggerService(LoggerFactory.getLogger(DataverseDepositServiceImpl.class));
    private final TemplateFieldSearcherService templateFieldSearcherService;

    private static final String TERMS_OF_USE = "CC0 - Public Domain Dedication";
    private static final String SUBJECT_OTHER = "Other";
    private static final String CONTRIBUTOR_TYPE_RESEARCHER = "Researcher";
    private static final String CONTRIBUTOR_TYPE_FUNDER = "Funder";
    private static final String CONTRIBUTOR_TYPE_EDITOR = "Editor";
    private static final String CONTRIBUTOR_TYPE_MEMBER = "Project Member";
    private static final String FIELD_TYPE_CLASS_PRIMITIVE = "primitive";
    private static final String FIELD_TYPE_CLASS_COMPOUND = "compound";
    private static final String FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY = "controlledVocabulary";

    private static final String SEMANTIC_DATAVERSE_IDENTIFIER = "dataverse.identifier";
    private static final String SEMANTIC_DATAVERSE_SUBJECT = "dataverse.dataset.subject";
    private static final String SEMANTIC_DATAVERSE_DESCRIPTION = "dataverse.dataset.description";
    private static final String SEMANTIC_DATAVERSE_ALTERNATIVE_TITLE = "dataverse.dataset.alternative_title";
    private static final String SEMANTIC_DATAVERSE_SUBTITLE = "dataverse.dataset.subtitle";
    private static final String SEMANTIC_DATAVERSE_ALTERNATIVE_URL = "dataverse.dataset.alternative_url";
    private static final String SEMANTIC_DATAVERSE_DATE_OF_DEPOSIT = "dataverse.dataset.date_of_deposit";
    private static final String SEMANTIC_DATAVERSE_NOTES = "dataverse.dataset.notes";

    private static final String SEMANTIC_DATAVERSE_AUTHOR_NAME = "dataverse.author.name";
    private static final String SEMANTIC_DATAVERSE_AUTHOR_AFFILIATION = "dataverse.author.affiliation";

    private static final String SEMANTIC_DATAVERSE_CONTACT_NAME = "dataverse.point_of_contact.name";
    private static final String SEMANTIC_DATAVERSE_CONTACT_EMAIL = "dataverse.point_of_contact.email";
    private static final String SEMANTIC_DATAVERSE_CONTACT_AFFILIATION = "dataverse.point_of_contact.affiliation";

    private static final String SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE = "dataverse.related_publication.identifier_type";
    private static final String SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER = "dataverse.related_publication.identifier";
    private static final String SEMANTIC_DATAVERSE_RELATED_PUBLICATION_RELATION_TYPE = "dataverse.related_publication.relation_type";
    private static final String SEMANTIC_DATAVERSE_RELATED_PUBLICATION_CITATION = "dataverse.related_publication.citation";
    private static final String SEMANTIC_DATAVERSE_RELATED_PUBLICATION_URL = "dataverse.related_publication.url";

    private static final String SEMANTIC_DATAVERSE_KEYWORD_TERM = "dataverse.keyword.term";
    private static final String SEMANTIC_DATAVERSE_KEYWORD_TERM_URI = "dataverse.keyword.term_uri";
    private static final String SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_NAME = "dataverse.keyword.controlled_vocabulary_name";
    private static final String SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_URL = "dataverse.keyword.controlled_vocabulary_url";

    private static final String SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_AGENCY = "dataverse.other_identifier.agency";
    private static final String SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_IDENTIFIER = "dataverse.other_identifier.identifier";

    private final DataverseServiceProperties dataverseServiceProperties;
    private final SemanticsProperties semanticsProperties;

    @Autowired
    public DataverseBuilder(TemplateFieldSearcherService templateFieldSearcherService, DataverseServiceProperties dataverseServiceProperties, SemanticsProperties semanticsProperties){
        this.templateFieldSearcherService = templateFieldSearcherService;
        this.dataverseServiceProperties = dataverseServiceProperties;
        this.semanticsProperties = semanticsProperties;
    }

    public DataverseDataset build(PlanModel planModel) {
        DataverseDataset dataset = new DataverseDataset();

        if (planModel == null) return dataset;
        DatasetVersion version = new DatasetVersion();
        DataSetMetadataBlock metadataBlock = new DataSetMetadataBlock();
        Citation citation = new Citation();
        List<CitationField> fields = new ArrayList<>();
        fields.add(new CitationField("title", FIELD_TYPE_CLASS_PRIMITIVE, false, planModel.getLabel()));
        fields.add(new CitationField("dsDescription", FIELD_TYPE_CLASS_COMPOUND, true, this.buildDescriptionFields(planModel)));
        fields.add(new CitationField("alternativeTitle", FIELD_TYPE_CLASS_PRIMITIVE, true, this.buildListStringValue(planModel, SEMANTIC_DATAVERSE_ALTERNATIVE_TITLE)));

        List<String> subtitle = this.buildListStringValue(planModel, SEMANTIC_DATAVERSE_SUBTITLE);
        if (!subtitle.isEmpty()) fields.add(new CitationField("subtitle", FIELD_TYPE_CLASS_PRIMITIVE, false, subtitle.getFirst()));

        List<String> notes = this.buildListStringValue(planModel, SEMANTIC_DATAVERSE_NOTES);
        if (!notes.isEmpty()) fields.add(new CitationField("notesText", FIELD_TYPE_CLASS_PRIMITIVE, false, notes.getFirst()));

        List<String> subjectFields = this.buildListStringValue(planModel, SEMANTIC_DATAVERSE_SUBJECT);
        if (subjectFields.isEmpty()) subjectFields.add(SUBJECT_OTHER);
        fields.add(new CitationField("subject", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, true, subjectFields));

        List<String> alternativeUrl = this.buildListStringValue(planModel, SEMANTIC_DATAVERSE_ALTERNATIVE_URL);
        String url = null;
        if (alternativeUrl.isEmpty() && planModel.getAccessType().equals(PlanAccessType.Public)) {
            url = dataverseServiceProperties.getDomain() + "explore-plans/overview/public/" + planModel.getId().toString();
        } else if (!alternativeUrl.isEmpty()) {
            url = alternativeUrl.getFirst();
        }
        if (url != null) fields.add(new CitationField("alternativeURL", FIELD_TYPE_CLASS_PRIMITIVE, false, url));

        List<String> dateOfDeposit = this.buildListStringValue(planModel, SEMANTIC_DATAVERSE_DATE_OF_DEPOSIT);
        try {
            if (!dateOfDeposit.isEmpty()) {
                LocalDate.parse(dateOfDeposit.getFirst(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                fields.add(new CitationField("dateOfDeposit", FIELD_TYPE_CLASS_PRIMITIVE, false, dateOfDeposit.getFirst()));
            }
        } catch (Exception e) {
            if (!dateOfDeposit.isEmpty()) {
                logger.warn("Wrong date of deposit input:" + dateOfDeposit.getFirst());
            }
        }

        fields.add(new CitationField("author", FIELD_TYPE_CLASS_COMPOUND, true, this.buildAuthorsFields(planModel)));
        fields.add(new CitationField("datasetContact", FIELD_TYPE_CLASS_COMPOUND, true, this.buildContactFields(planModel)));
        fields.add(new CitationField("contributor", FIELD_TYPE_CLASS_COMPOUND, true, this.buildContributorsFields(planModel)));
        fields.add(new CitationField("funding", FIELD_TYPE_CLASS_COMPOUND, true, this.buildFundingFields(planModel)));
        fields.add(new CitationField("publication", FIELD_TYPE_CLASS_COMPOUND, true, this.buildRelatedPublicationFields(planModel)));
        fields.add(new CitationField("keyword", FIELD_TYPE_CLASS_COMPOUND, true, this.buildKeywordsFields(planModel)));
        fields.add(new CitationField("otherIdentifier", FIELD_TYPE_CLASS_COMPOUND, true, this.buildOtherIdentifierFields(planModel)));

        citation.setFields(fields);
        metadataBlock.setCitation(citation);
        version.setMetadataBlocks(metadataBlock);
        version.setTermsOfUse(TERMS_OF_USE);
        dataset.setDatasetVersion(version);

        return dataset;
    }

    private List<Map<String, CitationField>> buildDescriptionFields(PlanModel planModel) {
        List<Map<String, CitationField>> citationFields = new ArrayList<>();
        if (planModel == null) return citationFields;

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){
                List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fields = this.findSchematicValues(SEMANTIC_DATAVERSE_DESCRIPTION, descriptionModel.getDescriptionTemplate().getDefinition());
                for (org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field : fields) {
                    if (field.getData() == null) continue;
                    List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(),  descriptionModel.getProperties());

                    for (FieldModel valueField : valueFields) {
                        String value = this.extractSchematicSingleValue(field, valueField);
                        if (value != null) {
                            citationFields.add(Map.of("dsDescriptionValue", new CitationField("dsDescriptionValue", FIELD_TYPE_CLASS_PRIMITIVE, false, value)));
                        }
                    }
                }

            }
        }

        List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> blueprintFieldsWithSemantic = this.getFieldOfSemantic(planModel, SEMANTIC_DATAVERSE_DESCRIPTION);
        for (org.opencdmp.commonmodels.models.planblueprint.FieldModel field: blueprintFieldsWithSemantic) {
            PlanBlueprintValueModel planBlueprintValueModel = this.getPlanBlueprintValue(planModel, field.getId());
            String value = null;
            if (planBlueprintValueModel != null) {
                if (planBlueprintValueModel.getValue() != null && !planBlueprintValueModel.getValue().isBlank()) value = planBlueprintValueModel.getValue();
                else if (planBlueprintValueModel.getNumberValue() != null) value = planBlueprintValueModel.getNumberValue().toString();
                else if (planBlueprintValueModel.getDateValue() != null) value = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(planBlueprintValueModel.getDateValue());

                if (value != null) {
                    citationFields.add(Map.of("dsDescriptionValue", new CitationField("dsDescriptionValue", FIELD_TYPE_CLASS_PRIMITIVE, false, value)));
                }
            }
        }
        if (planModel.getDescription() != null && !planModel.getDescription().isBlank())
            citationFields.add(Map.of("dsDescriptionValue", new CitationField("dsDescriptionValue", FIELD_TYPE_CLASS_PRIMITIVE, false, planModel.getDescription())));

        return citationFields;
    }

    private List<Map<String, CitationField>> buildAuthorsFields(PlanModel planModel){
        List<Map<String, CitationField>> fields = new ArrayList<>();
        if (planModel == null || planModel.getUsers() == null) return fields;

        List<String> organizations = new ArrayList<>();
        List<ReferenceModel> planOrganizations = this.getReferenceModelOfType(planModel, dataverseServiceProperties.getOrganizationReferenceCode());
        if (!planOrganizations.isEmpty()) organizations = planOrganizations.stream().map(ReferenceModel::getLabel).toList();

        for (PlanUserModel planUser: planModel.getUsers()) {
            Map<String, CitationField> map = new HashMap<>();
            map.put("authorName", new CitationField("authorName", FIELD_TYPE_CLASS_PRIMITIVE, false, planUser.getUser().getName()));

            if (!organizations.isEmpty()) {
                map.put("authorAffiliation", new CitationField("authorAffiliation", FIELD_TYPE_CLASS_PRIMITIVE, false, String.join(", ", organizations)));
            }
            fields.add(map);
        }

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){

                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), List.of(
                        SEMANTIC_DATAVERSE_AUTHOR_NAME, SEMANTIC_DATAVERSE_AUTHOR_AFFILIATION))) {
                    List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetValue(fieldSet, descriptionModel.getProperties());
                    for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                        Map<String, CitationField> map = new HashMap<>();

                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_AUTHOR_NAME, "authorName", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_AUTHOR_AFFILIATION, "authorAffiliation", FIELD_TYPE_CLASS_PRIMITIVE);
                        fields.add(map);
                    }
                }

            }
        }

        return fields;
    }

    private List<Map<String, CitationField>> buildContributorsFields(PlanModel planModel){
        List<Map<String, CitationField>> citationFields = new ArrayList<>();
        if (planModel == null) return citationFields;

        if (planModel.getUsers() != null) {
            for (PlanUserModel planUser: planModel.getUsers()) {
                Map<String, CitationField> map = new HashMap<>();
                map.put("contributorName", new CitationField("contributorName", FIELD_TYPE_CLASS_PRIMITIVE, false, planUser.getUser().getName()));
                map.put("contributorType", new CitationField("contributorType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, planUser.getRole().equals(PlanUserRole.Owner) ? CONTRIBUTOR_TYPE_EDITOR : CONTRIBUTOR_TYPE_MEMBER));
                citationFields.add(map);
            }
        }

        List<ReferenceModel> planResearchers = this.getReferenceModelOfType(planModel, dataverseServiceProperties.getResearcherReferenceCode());
        for (ReferenceModel researcher: planResearchers) {
            Map<String, CitationField> map = new HashMap<>();
            map.put("contributorName", new CitationField("contributorName", FIELD_TYPE_CLASS_PRIMITIVE, false, researcher.getLabel()));
            map.put("contributorType", new CitationField("contributorType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, CONTRIBUTOR_TYPE_RESEARCHER));
            citationFields.add(map);
        }

        List<ReferenceModel> planFunders = this.getReferenceModelOfType(planModel, dataverseServiceProperties.getFunderReferenceCode());
        for (ReferenceModel funder: planFunders) {
            Map<String, CitationField> map = new HashMap<>();
            map.put("contributorName", new CitationField("contributorName", FIELD_TYPE_CLASS_PRIMITIVE, false, funder.getLabel()));
            map.put("contributorType", new CitationField("contributorType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, CONTRIBUTOR_TYPE_FUNDER));
            citationFields.add(map);
        }

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){
                for (SemanticsProperties.Contributor contributorType: this.semanticsProperties.getContributorType()){
                    List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fields = this.findSchematicValues(contributorType.getCode(), descriptionModel.getDescriptionTemplate().getDefinition());
                    for (org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field : fields) {
                        if (field.getData() == null) continue;
                        List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(),  descriptionModel.getProperties());

                        for (FieldModel valueField : valueFields) {
                            Map<String, CitationField> map = new HashMap<>();
                            map.put("contributorType", new CitationField("contributorType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, contributorType.getValue()));

                            String value = this.extractSchematicSingleValue(field, valueField);
                            if (value != null) {
                                map.put("contributorName", new CitationField("contributorName", FIELD_TYPE_CLASS_PRIMITIVE, false, value));
                                citationFields.add(map);
                            }
                        }
                    }
                }
            }
        }
        for (SemanticsProperties.Contributor contributorType: this.semanticsProperties.getContributorType()){
            List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> blueprintFieldsWithSemantic = this.getFieldOfSemantic(planModel, contributorType.getCode());
            for (org.opencdmp.commonmodels.models.planblueprint.FieldModel field: blueprintFieldsWithSemantic) {
                PlanBlueprintValueModel planBlueprintValueModel = this.getPlanBlueprintValue(planModel, field.getId());
                Map<String, CitationField> map = new HashMap<>();
                if (planBlueprintValueModel != null) {
                    map.put("contributorType", new CitationField("contributorType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, contributorType.getValue()));
                    if (planBlueprintValueModel.getValue() != null && !planBlueprintValueModel.getValue().isBlank()) {
                        map.put("contributorName", new CitationField("contributorName", FIELD_TYPE_CLASS_PRIMITIVE, false, planBlueprintValueModel.getValue()));
                        citationFields.add(map);
                    }
                }

            }
        }

        return citationFields;
    }

    private List<Map<String, CitationField>> buildFundingFields(PlanModel planModel){
        List<Map<String, CitationField>> fields = new ArrayList<>();
        if (planModel == null) return fields;

        List<ReferenceModel> planFunders = this.getReferenceModelOfType(planModel, dataverseServiceProperties.getFunderReferenceCode());

        for (ReferenceModel funder: planFunders) {
            Map<String, CitationField> map = new HashMap<>();
            map.put("fundingAgency", new CitationField("fundingAgency", FIELD_TYPE_CLASS_PRIMITIVE, false, funder.getLabel()));
            map.put("fundingAgencyGrantNumber", new CitationField("fundingAgencyGrantNumber", FIELD_TYPE_CLASS_PRIMITIVE, false, funder.getReference()));
            fields.add(map);
        }

        return fields;
    }

    private List<Map<String, CitationField>> buildContactFields(PlanModel planModel){
        List<Map<String, CitationField>> fields = new ArrayList<>();
        if (planModel == null || planModel.getProperties() == null || planModel.getProperties().getContacts() == null) return fields;

        for (PlanContactModel planContactModel: planModel.getProperties().getContacts()) {
            Map<String, CitationField> map = new HashMap<>();
            map.put("datasetContactName", new CitationField("datasetContactName", FIELD_TYPE_CLASS_PRIMITIVE, false, planContactModel.getFirstName() + " " + planContactModel.getLastName()));
            map.put("datasetContactEmail", new CitationField("datasetContactEmail", FIELD_TYPE_CLASS_PRIMITIVE, false, planContactModel.getEmail()));
            fields.add(map);
        }

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){

                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), List.of(
                        SEMANTIC_DATAVERSE_CONTACT_NAME, SEMANTIC_DATAVERSE_CONTACT_EMAIL, SEMANTIC_DATAVERSE_CONTACT_AFFILIATION))) {
                    List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetValue(fieldSet, descriptionModel.getProperties());
                    for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                        Map<String, CitationField> map = new HashMap<>();

                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_CONTACT_NAME, "datasetContactName", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_CONTACT_EMAIL, "datasetContactEmail", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_CONTACT_AFFILIATION, "datasetContactAffiliation", FIELD_TYPE_CLASS_PRIMITIVE);
                        fields.add(map);
                    }
                }

            }
        }
        return fields;
    }

    private List<Map<String, CitationField>> buildRelatedPublicationFields(PlanModel planModel) {
        List<Map<String, CitationField>> citationFields = new ArrayList<>();
        if (planModel == null) return citationFields;

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){
                for (String identifierType: this.semanticsProperties.getIdentifierType()){
                    List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fields = this.findSchematicValues(identifierType, descriptionModel.getDescriptionTemplate().getDefinition());
                    for (org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field : fields) {
                        if (field.getData() == null) continue;
                        List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(),  descriptionModel.getProperties());

                        for (FieldModel valueField : valueFields) {
                            Map<String, CitationField> map = new HashMap<>();
                            map.put("publicationIDType", new CitationField("publicationIDType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, identifierType.split("\\.")[3]));

                            String value = this.extractSchematicSingleValue(field, valueField);
                            if (value != null) map.put("publicationIDNumber", new CitationField("publicationIDNumber", FIELD_TYPE_CLASS_PRIMITIVE, false, value));

                            if (field.getSemantics() != null) {
                                field.getSemantics().stream().filter(this.semanticsProperties.getRelationType()::contains).findFirst()
                                        .ifPresent(relationType -> map.put("publicationRelationType", new CitationField("publicationRelationType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, relationType.split("\\.")[3])));
                            }
                            if (value != null) citationFields.add(map);
                        }
                    }
                }

                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), List.of(
                        SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_RELATION_TYPE, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER,
                        SEMANTIC_DATAVERSE_RELATED_PUBLICATION_URL, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_CITATION))) {
                    List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetValue(fieldSet, descriptionModel.getProperties());
                    for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                        Map<String, CitationField> map = new HashMap<>();

                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE, "publicationIDType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_RELATION_TYPE, "publicationRelationType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_CITATION, "publicationCitation", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER, "publicationIDNumber", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_RELATED_PUBLICATION_URL, "publicationURL", FIELD_TYPE_CLASS_PRIMITIVE);
                        citationFields.add(map);
                    }
                }

                for (DescriptionModel description : planModel.getDescriptions()) {
                    PropertyDefinitionModel properties = description.getProperties();
                    if (properties == null) continue;

                    Map<String, PropertyDefinitionFieldSetModel> fieldSets = properties.getFieldSets();
                    if (fieldSets == null) continue;

                    for (PropertyDefinitionFieldSetModel fieldSet : fieldSets.values()) {
                        List<PropertyDefinitionFieldSetItemModel> items = fieldSet.getItems();
                        if (items == null) continue;

                        for (PropertyDefinitionFieldSetItemModel item : items) {
                            Map<String, FieldModel> definitionFields = item.getFields();
                            if (definitionFields == null) continue;

                            for (FieldModel field : definitionFields.values()) {
                                List<ReferenceModel> references = field.getReferences();
                                if (references == null) continue;

                                for (ReferenceModel reference : references) {
                                    List<ReferenceTypeFieldModel> semanticReferences = new ArrayList<>();

                                    ReferenceTypeModel type = reference.getType();
                                    if (type == null) continue;

                                    if (type.getDefinition() == null) continue;
                                    List<ReferenceTypeFieldModel> refTypeFields = type.getDefinition().getFields();

                                    if (refTypeFields == null) continue;

                                    for (ReferenceTypeFieldModel refField : refTypeFields) {
                                        if (refField.getSemantics() == null) continue;

                                        for (String semantics : refField.getSemantics()) {
                                            if (semantics.equals(SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER))
                                                semanticReferences.add(refField);
                                            if (semantics.equals(SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE))
                                                semanticReferences.add(refField);

                                        }
                                    }

                                    if (!semanticReferences.isEmpty() && reference.getDefinition() != null && reference.getDefinition().getFields() != null) {
                                        for (ReferenceFieldModel defField : reference.getDefinition().getFields()) {
                                            if (defField == null || defField.getCode() == null) continue;
                                            Map<String, CitationField> map = new HashMap<>();

                                            for (ReferenceTypeFieldModel semanticRef : semanticReferences) {
                                                if (semanticRef != null && semanticRef.getCode() != null && defField.getCode().equals(semanticRef.getCode())) {
                                                    if(semanticRef.getSemantics().contains(SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER)) map.put("publicationIDNumber", new CitationField("publicationIDNumber", FIELD_TYPE_CLASS_PRIMITIVE, false, defField.getValue()));
                                                    if(semanticRef.getSemantics().contains(SEMANTIC_DATAVERSE_RELATED_PUBLICATION_IDENTIFIER_TYPE)) map.put("publicationIDType",new CitationField("publicationIDType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, defField.getValue()));
                                                }
                                            }
                                            citationFields.add(map);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


            }
        }
        for (String identifierType: this.semanticsProperties.getIdentifierType()){
            List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> blueprintFieldsWithSemantic = this.getFieldOfSemantic(planModel, identifierType);
            for (org.opencdmp.commonmodels.models.planblueprint.FieldModel field: blueprintFieldsWithSemantic) {
                PlanBlueprintValueModel planBlueprintValueModel = this.getPlanBlueprintValue(planModel, field.getId());
                Map<String, CitationField> map = new HashMap<>();
                String value = null;
                if (planBlueprintValueModel != null) {
                    map.put("publicationIDType", new CitationField("publicationIDType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, identifierType.split("\\.")[3]));
                    if (planBlueprintValueModel.getValue() != null && !planBlueprintValueModel.getValue().isBlank()) value = planBlueprintValueModel.getValue();
                    else if (planBlueprintValueModel.getNumberValue() != null) value = planBlueprintValueModel.getNumberValue().toString();
                    else if (planBlueprintValueModel.getDateValue() != null) value = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(planBlueprintValueModel.getDateValue());

                    if (value != null) map.put("publicationIDNumber", new CitationField("publicationIDNumber", FIELD_TYPE_CLASS_PRIMITIVE, false, value));
                }

                if(field.getSemantics() != null) {
                    field.getSemantics().stream().filter(this.semanticsProperties.getRelationType()::contains).findFirst()
                            .ifPresent(relationType -> map.put("publicationRelationType", new CitationField("publicationRelationType", FIELD_TYPE_CLASS_CONTROLLED_VOCABULARY, false, relationType.split("\\.")[3])));
                }

                if (value != null) citationFields.add(map);
            }
        }

        return citationFields;
    }

    private List<Map<String, CitationField>> buildKeywordsFields(PlanModel planModel) {
        List<Map<String, CitationField>> citationFields = new ArrayList<>();
        if (planModel == null) return citationFields;

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){

                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), List.of(
                        SEMANTIC_DATAVERSE_KEYWORD_TERM, SEMANTIC_DATAVERSE_KEYWORD_TERM_URI,
                        SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_NAME, SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_URL))) {
                    List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetValue(fieldSet, descriptionModel.getProperties());
                    for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                        Map<String, CitationField> map = new HashMap<>();

                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_TERM, "keywordValue", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_TERM_URI, "keywordTermURI", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_NAME, "keywordVocabulary", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_KEYWORD_VOCABULARY_URL, "keywordVocabularyURI", FIELD_TYPE_CLASS_PRIMITIVE);
                        citationFields.add(map);
                    }
                }

            }
        }

        return citationFields;
    }

    private List<Map<String, CitationField>> buildOtherIdentifierFields(PlanModel planModel) {
        List<Map<String, CitationField>> citationFields = new ArrayList<>();
        if (planModel == null) return citationFields;

        if (planModel.getDescriptions() != null) {
            for (DescriptionModel descriptionModel: planModel.getDescriptions()){

                for (FieldSetModel fieldSet : this.templateFieldSearcherService.searchFieldSetsBySemantics(descriptionModel.getDescriptionTemplate(), List.of(
                        SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_AGENCY, SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_IDENTIFIER))) {
                    List<PropertyDefinitionFieldSetItemModel> propertyDefinitionFieldSetItemModels = this.findFieldSetValue(fieldSet, descriptionModel.getProperties());
                    for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetItemModels) {
                        Map<String, CitationField> map = new HashMap<>();

                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_AGENCY, "otherIdentifierAgency", FIELD_TYPE_CLASS_PRIMITIVE);
                        this.buildCitationFieldFromFieldSetSemantic(map, descriptionModel, fieldSet, propertyDefinitionFieldSetItemModel, SEMANTIC_DATAVERSE_OTHER_IDENTIFIER_IDENTIFIER, "otherIdentifierValue", FIELD_TYPE_CLASS_PRIMITIVE);
                        citationFields.add(map);
                    }
                }

            }
        }

        return citationFields;
    }

    private void buildCitationFieldFromFieldSetSemantic(Map<String, CitationField> map, DescriptionModel descriptionModel, FieldSetModel fieldSet,
                                                        PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel,
                                                        String semantic, String citationFieldName, String typeClass) {
        FieldModel fieldValue = this.findValueFieldBySemantic(fieldSet, propertyDefinitionFieldSetItemModel, semantic);
        if (fieldValue != null) {
            org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field = descriptionModel.getDescriptionTemplate().getDefinition().getAllField().stream().filter(x-> x.getId().equals(fieldValue.getId())).findFirst().orElse(null);
            String value = this.extractSchematicSingleValue(field, fieldValue);
            if (value != null) map.put(citationFieldName, new CitationField(citationFieldName, typeClass, false, value));
        }
    }

    private List<String> buildListStringValue(PlanModel planModel, String semantic){
        List<String> fields = new ArrayList<>();
        if (planModel == null) return fields;

        //plan blueprint semantics
        List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> blueprintFieldsWithSemantic = this.getFieldOfSemantic(planModel, semantic);
        for (org.opencdmp.commonmodels.models.planblueprint.FieldModel field: blueprintFieldsWithSemantic) {
            PlanBlueprintValueModel planBlueprintValueModel = this.getPlanBlueprintValue(planModel, field.getId());
            if (planBlueprintValueModel != null) {
                if (planBlueprintValueModel.getValue() != null && !planBlueprintValueModel.getValue().isBlank() && !fields.contains(planBlueprintValueModel.getValue())) fields.add(planBlueprintValueModel.getValue());
                if (planBlueprintValueModel.getDateValue() != null) fields.add(DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(planBlueprintValueModel.getDateValue()));
                if (planBlueprintValueModel.getNumberValue() != null) fields.add(planBlueprintValueModel.getNumberValue().toString());
            }
        }

        //description template
        for (DescriptionModel descriptionModel: planModel.getDescriptions()) {
            List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fieldsWithSemantics = this.findSchematicValues(semantic, descriptionModel.getDescriptionTemplate().getDefinition());
            Set<String> values = extractSchematicValues(fieldsWithSemantics, descriptionModel.getProperties());
            //description tags from semantic
            for (String value: values){
                if (!fields.contains(value)) fields.add(value);
            }
        }

        return fields;
    }

    public String buildDataverseIdentifier(PlanModel planModel){
        if (planModel == null) return dataverseServiceProperties.getAlias();

        //plan blueprint semantics
        List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> blueprintFieldsWithSemantic = this.getFieldOfSemantic(planModel, SEMANTIC_DATAVERSE_IDENTIFIER);
        for (org.opencdmp.commonmodels.models.planblueprint.FieldModel field: blueprintFieldsWithSemantic) {
            PlanBlueprintValueModel planBlueprintValueModel = this.getPlanBlueprintValue(planModel, field.getId());
            if (planBlueprintValueModel != null) {
                if (planBlueprintValueModel.getValue() != null && !planBlueprintValueModel.getValue().isBlank()) return planBlueprintValueModel.getValue();
            }
        }

        //description template
        for (DescriptionModel descriptionModel: planModel.getDescriptions()) {
            List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fieldsWithSemantics = this.findSchematicValues(SEMANTIC_DATAVERSE_IDENTIFIER, descriptionModel.getDescriptionTemplate().getDefinition());
            Set<String> values = extractSchematicValues(fieldsWithSemantics, descriptionModel.getProperties());
            String value = values.stream().findFirst().orElse(null);
            if (value != null) return value;
        }

        return dataverseServiceProperties.getAlias();
    }


    //region  plan blueprint
    private List<ReferenceModel> getReferenceModelOfType(PlanModel planModel, String code){
        List<ReferenceModel> response = new ArrayList<>();
        if (planModel.getReferences() == null) return response;
        for (PlanReferenceModel planReferenceModel : planModel.getReferences()){
            if (planReferenceModel.getReference() != null && planReferenceModel.getReference().getType() != null && planReferenceModel.getReference().getType().getCode() != null  && planReferenceModel.getReference().getType().getCode().equals(code)){
                response.add(planReferenceModel.getReference());
            }
        }
        return response;
    }


    private List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> getFieldOfSemantic(PlanModel plan, String semanticKey){
        List<org.opencdmp.commonmodels.models.planblueprint.FieldModel> fields = new ArrayList<>();

        if (plan == null || plan.getPlanBlueprint() == null || plan.getPlanBlueprint().getDefinition() == null || plan.getPlanBlueprint().getDefinition().getSections() == null) return fields;
        for (SectionModel sectionModel : plan.getPlanBlueprint().getDefinition().getSections()){
            if (sectionModel.getFields() != null){
                org.opencdmp.commonmodels.models.planblueprint.FieldModel fieldModel = sectionModel.getFields().stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(semanticKey)).findFirst().orElse(null);
                if (fieldModel != null) fields.add(fieldModel);
            }
        }
        return fields;
    }

    private PlanBlueprintValueModel getPlanBlueprintValue(PlanModel plan, UUID id){
        if (plan == null || plan.getProperties() == null || plan.getProperties().getPlanBlueprintValues() == null) return null;
        return plan.getProperties().getPlanBlueprintValues().stream().filter(x-> x.getFieldId().equals(id)).findFirst().orElse(null);
    }
    //endregion

    //region description template

    private List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> findSchematicValues(String semantic, DefinitionModel definitionModel){
        return definitionModel.getAllField().stream().filter(x-> x.getSemantics() != null && x.getSemantics().contains(semantic)).toList();
    }

    private Set<String> extractSchematicValues(List<org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel> fields, PropertyDefinitionModel propertyDefinition) {
        Set<String> values = new HashSet<>();
        for (org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field : fields) {
            if (field.getData() == null) continue;
            List<FieldModel> valueFields = this.findValueFieldsByIds(field.getId(), propertyDefinition);
            for (FieldModel valueField : valueFields) {
                switch (field.getData().getFieldType()) {
                    case FREE_TEXT, TEXT_AREA, RICH_TEXT_AREA -> {
                        if (valueField.getTextValue() != null && !valueField.getTextValue().isBlank()) values.add(valueField.getTextValue());
                    }
                    case BOOLEAN_DECISION, CHECK_BOX -> {
                        if (valueField.getBooleanValue() != null) values.add(valueField.getBooleanValue().toString());
                    }
                    case DATE_PICKER -> {
                        if (valueField.getDateValue() != null) values.add(DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(valueField.getDateValue()));
                    }
                    case DATASET_IDENTIFIER, VALIDATION -> {
                        if (valueField.getExternalIdentifier() != null && valueField.getExternalIdentifier().getIdentifier() != null && !valueField.getExternalIdentifier().getIdentifier().isBlank()) {
                            values.add(valueField.getExternalIdentifier().getIdentifier());
                        }
                    }
                    case TAGS -> {
                        if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                            values.addAll(valueField.getTextListValue());
                        }
                    }
                    case SELECT -> {
                        if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                            SelectDataModel selectDataModel = (SelectDataModel)field.getData();
                            if (selectDataModel != null && selectDataModel.getOptions() != null && !selectDataModel.getOptions().isEmpty()){
                                for (SelectDataModel.OptionModel option : selectDataModel.getOptions()){
                                    if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel())) values.add(option.getLabel());
                                }
                            }
                        }
                    }
                    case RADIO_BOX -> {
                        if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                            RadioBoxDataModel radioBoxModel = (RadioBoxDataModel)field.getData();
                            if (radioBoxModel != null && radioBoxModel.getOptions() != null && !radioBoxModel.getOptions().isEmpty()){
                                for (RadioBoxDataModel.RadioBoxOptionModel option : radioBoxModel.getOptions()){
                                    if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel())) values.add(option.getLabel());
                                }
                            }
                        }
                    }
                    case REFERENCE_TYPES -> {
                        if (valueField.getReferences() != null && !valueField.getReferences().isEmpty()) {
                            for (ReferenceModel referenceModel : valueField.getReferences()) {
                                if (referenceModel == null
                                        || referenceModel.getType() == null || referenceModel.getType().getCode() == null || referenceModel.getType().getCode().isBlank()
                                        || referenceModel.getDefinition() == null || referenceModel.getDefinition().getFields() == null || referenceModel.getDefinition().getFields().isEmpty()) continue;
                                if (referenceModel.getReference() != null && !referenceModel.getReference().isBlank()) {
                                    values.add(referenceModel.getReference());
                                }
                            }
                        }
                    }
                }
            }
        }
        return values;
    }

    private String extractSchematicSingleValue(org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field, FieldModel valueField) {
            if (field == null || field.getData() == null) return null;

        switch (field.getData().getFieldType()) {
            case FREE_TEXT, TEXT_AREA, RICH_TEXT_AREA -> {
                if (valueField.getTextValue() != null && !valueField.getTextValue().isBlank()) return valueField.getTextValue();
            }
            case BOOLEAN_DECISION, CHECK_BOX -> {
                if (valueField.getBooleanValue() != null) return valueField.getBooleanValue().toString();
            }
            case DATE_PICKER -> {
                if (valueField.getDateValue() != null) return (DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(valueField.getDateValue()));
            }
            case DATASET_IDENTIFIER, VALIDATION -> {
                if (valueField.getExternalIdentifier() != null && valueField.getExternalIdentifier().getIdentifier() != null && !valueField.getExternalIdentifier().getIdentifier().isBlank()) {
                    return (valueField.getExternalIdentifier().getIdentifier());
                }
            }
            case TAGS -> {
                if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                    return String.join(", ", valueField.getTextListValue());
                }
            }
            case SELECT -> {
                if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                    SelectDataModel selectDataModel = (SelectDataModel)field.getData();
                    if (selectDataModel != null && selectDataModel.getOptions() != null && !selectDataModel.getOptions().isEmpty()){
                        for (SelectDataModel.OptionModel option : selectDataModel.getOptions()){
                            if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel()))  return (option.getValue());
                        }
                    }
                }
            }
            case RADIO_BOX -> {
                if (valueField.getTextListValue() != null && !valueField.getTextListValue().isEmpty()) {
                    RadioBoxDataModel radioBoxModel = (RadioBoxDataModel)field.getData();
                    if (radioBoxModel != null && radioBoxModel.getOptions() != null && !radioBoxModel.getOptions().isEmpty()){
                        for (RadioBoxDataModel.RadioBoxOptionModel option : radioBoxModel.getOptions()){
                            if (valueField.getTextListValue().contains(option.getValue()) || valueField.getTextListValue().contains(option.getLabel()))  return (option.getValue());
                        }
                    }
                }
            }
            case REFERENCE_TYPES -> {
                if (valueField.getReferences() != null && !valueField.getReferences().isEmpty()) {
                    for (ReferenceModel referenceModel : valueField.getReferences()) {
                        if (referenceModel == null
                                || referenceModel.getType() == null || referenceModel.getType().getCode() == null || referenceModel.getType().getCode().isBlank()
                                || referenceModel.getDefinition() == null || referenceModel.getDefinition().getFields() == null || referenceModel.getDefinition().getFields().isEmpty()) continue;
                        if (referenceModel.getReference() != null && !referenceModel.getReference().isBlank()) {
                            return (referenceModel.getReference());
                        }
                    }
                }
            }
        }

        return null;
    }

    private List<FieldModel> findValueFieldsByIds(String fieldId, PropertyDefinitionModel definitionModel){
        List<FieldModel> models = new ArrayList<>();
        if (definitionModel == null || definitionModel.getFieldSets() == null || definitionModel.getFieldSets().isEmpty()) return models;
        for (PropertyDefinitionFieldSetModel propertyDefinitionFieldSetModel : definitionModel.getFieldSets().values()){
            if (propertyDefinitionFieldSetModel == null ||propertyDefinitionFieldSetModel.getItems() == null || propertyDefinitionFieldSetModel.getItems().isEmpty()) continue;
            for (PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel : propertyDefinitionFieldSetModel.getItems()){
                if (propertyDefinitionFieldSetItemModel == null ||propertyDefinitionFieldSetItemModel.getFields() == null || propertyDefinitionFieldSetItemModel.getFields().isEmpty()) continue;
                for (Map.Entry<String, FieldModel> entry : propertyDefinitionFieldSetItemModel.getFields().entrySet()){
                    if (entry == null || entry.getValue() == null) continue;
                    if (entry.getKey().equalsIgnoreCase(fieldId)) models.add(entry.getValue());
                }
            }
        }
        return models;
    }

    private FieldModel findValueFieldBySemantic(FieldSetModel fieldSet, PropertyDefinitionFieldSetItemModel propertyDefinitionFieldSetItemModel, String semantic){
        org.opencdmp.commonmodels.models.descriptiotemplate.FieldModel field = this.templateFieldSearcherService.findFieldBySemantic(fieldSet, semantic);
        return field != null ? propertyDefinitionFieldSetItemModel.getFields().getOrDefault(field.getId(), null) : null;
    }

    private List<PropertyDefinitionFieldSetItemModel> findFieldSetValue(FieldSetModel fieldSetModel, PropertyDefinitionModel descriptionTemplateModel){
        List<PropertyDefinitionFieldSetItemModel> items = new ArrayList<>();
        if (fieldSetModel == null || descriptionTemplateModel == null || descriptionTemplateModel.getFieldSets() == null) return items;
        PropertyDefinitionFieldSetModel propertyDefinitionFieldSetModel =  descriptionTemplateModel.getFieldSets().getOrDefault(fieldSetModel.getId(), null);
        if (propertyDefinitionFieldSetModel != null && propertyDefinitionFieldSetModel.getItems() != null) return propertyDefinitionFieldSetModel.getItems();
        return items;
    }

    //endregion

}

