# eidas-idporten-connector

eIDAS spesific connector for ID-porten/Norway

## How to run

When running full stack or directly use acr eidas-loa-high

Minimum dependencies:

* demoland
* freg-gateway
* eidas-connector
* teknisk testklient

Test using the technical test client. example properties below

```
oidc-test-client:
  application-url: http://localhost:${server.port}${server.servlet.context-path}
  redirect-uri: ${oidc-test-client.application-url}authorize/response
  post-logout-redirect-uri: ${oidc-test-client.application-url}endsession/response
  openid-integrations:
    - id: eidas
      description: Integrasjon mot eidas Authentication localhost
      openid-provider:
        issuer: http://localhost:8088
      openid-client:
        client-id: testclient
        client-secret: password
        token-endpoint-auth-method: client_secret_basic
        redirect-uri: ${oidc-test-client.redirect-uri}
        post-logout-redirect-uri: ${oidc-test-client.post-logout-redirect-uri}
```  

## matching service

to use Nobid matching service, set the following properties: eidas.nobid.enabled:true and make sure
eidas.freg-gw.enabled: false
To use F-REG matching service, set the following properties: eidas.freg-gw.enabled: true and make sure
eidas.nobid.enabled: false

## Sequence diagrams

```mermaid  
sequenceDiagram
    autonumber
    participant IL as ID-Porten
    box lightpink 
        participant EIC as eidas-idporten-connector
    end
    participant RED as redis
    participant EC as eidas-connector
    participant FRG as F-REG Gateway
    participant IDP as IDP(foreign eidas proxy)
    
    IL ->> EIC: OIDC authorize
    EIC ->> EIC: select country
    EIC ->> EIC: map to LightProtocol request
    EIC ->> RED: store LightProtocol request
    EIC ->> EC: light token
    EC ->> RED: get LightProtocol request
    RED ->> IDP: Communicate with IDP (SAML)
    IDP ->> RED: Response (SAML)
    EC ->> RED: store LightProtocol response
    EC ->> EIC: light token
    EIC ->> RED: get LightProtocol response
    note over EIC: feature flag for freg enabled
    EIC ->> FRG: match identity
    FRG -->> EIC: match result
    EIC -->> IL: OIDC code
    IL ->> EIC: getToken

```    

### Matching with Nobid (PAR + callback). Internal workings

```mermaid
sequenceDiagram
    autonumber
    box lightpink eidas-idporten-connector
        participant CRC as ConnectorResponseController
        participant SCS as SpecificConnectorService
        participant NCC as NobidCallbackController
        participant NMC as NobidMatchingServiceClient
        participant AS as AuthorizationResponseService
    end
    participant NOB as Nobid Matching Service
    note over CRC: Eidas login response (see sequence diagram above)
    CRC ->> CRC: Extract lightprotocol response
    CRC ->> SCS: Match user (EidasUser)
    SCS ->> SCS: getEidasUser
    note over SCS: feature flag for nobid enabled
    SCS ->> NMC: Match user (EidasUser)
    NMC ->> NOB: send PAR request (back channel)
    NOB ->> NOB: store PAR request and return request_uri
    NOB -->> NMC: par response with request_uri
    NMC ->> NMC: matching required
    NMC -->> SCS: UserMatchResult: UserMatchRedirect
    SCS -->> CRC: UserMatchRedirect
    CRC ->> NOB: redirect to Nobid with request_uri (front channel)
    NOB ->> NOB: match user
    NOB -->> NCC: /callback/nobid?code=...
    NCC ->> NMC: getClaims
    NMC ->> NOB: get token
    NOB -->> NMC: token response w/ID-token
    NMC ->> NMC: extract user claims
    NMC -->> NCC: return result
    NCC ->> AS: generateAuthorizationResponse
    AS -->> NCC: authorizationResponse
    note over NCC: return authorization code to IDPorten


```
