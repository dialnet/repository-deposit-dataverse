# Repository Deposit Dataverse for OpenCDMP

**repository-deposit-dataverse** is an implementation of the [repository-deposit-base](https://github.com/OpenCDMP/repository-deposit-base) package that enables the deposition of **OpenCDMP Plans** into the [Dataverse](https://dataverse.org/) repository, automatically minting a **Digital Object Identifier (DOI)** for each deposited plan. Built as a Spring Boot microservice for seamless integration with OpenCDMP.

## Overview

The **Dataverse** repository is a widely-used open-access research repository. By using the **repository-deposit-dataverse** service, OpenCDMP users can directly deposit their DMPs into Dataverse, making the plans citeable and publicly available. The service supports both **system-based** and **user-based** depositions depending on the configuration.

**Supported Dataverse version:** 6.9

**Supported operations:**
- ✅ Deposit plans to Dataverse
- ✅ Automatic DOI minting
- ✅ System-based and user-based depositions

---
## Quick start

This service implements the following endpoints as per `DepositController`

### API endpoints

- `POST /deposit` - Deposit a plan to Dataverse
- `GET /configuration` - Get repository configuration
- `GET /logo` - Get Dataverse logo (base64)

### Example

- **Deposit with System Access Token**
```bash
 # Uses a system-wide access token configured by the service.
 # No user action is required.

 POST /deposit
  {
    "planModel": {...},
    "authInfo": {
        "authToken": "system_access_token"
    } 
  }
```

- **Deposit with user access token (from OpenCDMP profile settings)**
```bash
 # The user has stored their Dataverse access token in their OpenCDMP profile settings (see https://opencdmp.github.io/user-guide/profile-settings/#external-plugin-settings).
 # The token is retrieved from the saved user credential.
 # This token is persistent and remains valid until the user updates it in their profile.

 POST /deposit
  {
    "planModel": {...},
    "authInfo": {
        "authToken": null
        "authFields": [
            {
                "code": "dataverse-access-token"
                "textValue": "user_access_token"
            }
        ]
    } 
  }
```


- **Deposit a new version of an existing plan**
```bash
 # Same as case 1 (system token).
 # previousDOI is mandatory to indicate that this is a new version of an existing deposit in this repository.

 POST /deposit
  {
    "planModel": {
        "id": "plan-uuid",
        "title": "My Research Plan",
        "description": "Plan content",
        "previousDOI": "doi"
        // more
    },
    "authInfo": {
        "authToken": "system_access_token"
    } 
  }
```
---

## Integration with OpenCDMP

To integrate this service with your OpenCDMP deployment, configure the deposit plugin in the OpenCDMP admin interface.

For detailed integration instructions, see see the [Dataverse Configuration](https://opencdmp.github.io/getting-started/configuration/backend/deposit/#dataverse) and the [OpenCDMP Deposit Service Authentication](https://opencdmp.github.io/getting-started/configuration/backend/#deposit-service-authentication).

---

## See Also

For complete documentation on configuration, integration, and usage:

- **Deposit Service Overview**: https://opencdmp.github.io/optional-services/deposit-services/
- **User Guide**: [Depositing Plans](https://opencdmp.github.io/user-guide/plans/deposit-a-plan/)
- **Developer Guide**: [Building Custom Deposit Services](https://opencdmp.github.io/developers/plugins/deposit/)

---

## License

This repository is licensed under the [EUPL 1.2 License](LICENSE).

### Contact

For questions, support, or feedback:

- **Email**: opencdmp at cite.gr
- **GitHub Issues**: https://github.com/OpenCDMP/repository-deposit-dataverse/issues
---

*This service is part of the OpenCDMP ecosystem. For general OpenCDMP documentation, visit [opencdmp.github.io](https://opencdmp.github.io).*
