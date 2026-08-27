package org.opencdmp.deposit.controller;

import gr.cite.tools.auditing.AuditService;
import gr.cite.tools.logging.LoggerService;
import gr.cite.tools.logging.MapLogEntry;
import org.opencdmp.commonmodels.models.plan.PlanModel;
import org.opencdmp.deposit.dataverse.audit.AuditableAction;
import org.opencdmp.deposit.dataverse.service.dataverse.DataverseDepositService;
import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.opencdmp.depositbase.repository.PlanDepositModel;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Multi-collection deposit endpoint.
 *
 * OpenCDMP builds the deposit call as {source.url} + "/api/deposit". By configuring the
 * per-tenant deposit source url as {@code https://<plugin-host>/t/<collectionAlias>}, the
 * {@code targetAlias} path segment selects the Dataverse collection to deposit into,
 * overriding the plan-derived identifier. Everything else behaves like {@link DepositController},
 * which stays available at {@code /api/deposit} for the default (plan-derived) collection.
 */
@RestController
@RequestMapping("/t/{targetAlias}/api/deposit")
public class TenantScopedDepositController {
    private static final LoggerService logger = new LoggerService(LoggerFactory.getLogger(TenantScopedDepositController.class));

    // A Dataverse collection alias is a URL-safe short name; reject anything else so it
    // cannot be used to tamper with the outgoing Dataverse request path.
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final DataverseDepositService depositClient;

    private final AuditService auditService;

    @Autowired
    public TenantScopedDepositController(DataverseDepositService depositClient, AuditService auditService) {
        this.depositClient = depositClient;
        this.auditService = auditService;
    }

    @PostMapping
    public String deposit(@PathVariable("targetAlias") String targetAlias, @RequestBody PlanDepositModel planDepositModel) {
        if (targetAlias == null || !ALIAS_PATTERN.matcher(targetAlias).matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid target alias");

        logger.debug(new MapLogEntry("deposit " + PlanModel.class.getSimpleName()).And("targetAlias", targetAlias).And("planDepositModel", planDepositModel));

        String doiId = this.depositClient.deposit(planDepositModel, targetAlias);

        this.auditService.track(AuditableAction.Deposit_Deposit, Map.ofEntries(
                new AbstractMap.SimpleEntry<String, Object>("targetAlias", targetAlias),
                new AbstractMap.SimpleEntry<String, Object>("planDepositModel", planDepositModel)
        ));
        return doiId;
    }

    @GetMapping("/authenticate")
    public String authenticate(@RequestParam("authToken") String code) {
        logger.debug(new MapLogEntry("authenticate " + PlanModel.class.getSimpleName()));

        String token = this.depositClient.authenticate(code);

        this.auditService.track(AuditableAction.Deposit_Authenticate);

        return token;
    }

    @GetMapping("/configuration")
    public DepositConfiguration getConfiguration() {
        logger.debug(new MapLogEntry("getConfiguration " + PlanModel.class.getSimpleName()));

        DepositConfiguration configuration = this.depositClient.getConfiguration();

        this.auditService.track(AuditableAction.Deposit_GetConfiguration);

        return configuration;
    }

    @GetMapping("/logo")
    public String getLogo() {
        logger.debug(new MapLogEntry("getLogo " + PlanModel.class.getSimpleName()));

        String logo = this.depositClient.getLogo();

        this.auditService.track(AuditableAction.Deposit_GetLogo);

        return logo;
    }
}
