package org.opencdmp.deposit.dataverse.service.dataverse;

import org.opencdmp.depositbase.repository.DepositConfiguration;
import org.opencdmp.depositbase.repository.PlanDepositModel;

public interface DataverseDepositService {
	String deposit(PlanDepositModel planDepositModel);

	/**
	 * Same as {@link #deposit(PlanDepositModel)} but the target Dataverse collection is
	 * given explicitly (per-tenant routing), overriding the plan-derived identifier.
	 * A null/blank override falls back to the default resolution.
	 */
	String deposit(PlanDepositModel planDepositModel, String collectionAliasOverride);

	DepositConfiguration getConfiguration();

	String authenticate(String code);

	String getLogo();
}
