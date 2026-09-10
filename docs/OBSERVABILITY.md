# OBSERVABILITY.md

## 1. Backend

- Spring Boot Actuator
- Micrometer
- structured logging

## 2. Metrics candidates

- request count/latency/error rate
- DB query latency
- booking conflict count
- booking create failure
- auth failure
- notification failure
- payment record failure

## 3. Monitoring

향후:
```text
Micrometer
→ Prometheus
→ Grafana
```

## 4. Logging

권장 context:
- traceId
- endpoint
- userId where safe
- studioId
- result/error code

금지:
- raw OAuth token
- JWT
- password
- OTP
- full customer phone when unnecessary

## 5. Alerts

운영 단계에서:
- 5xx spike
- DB connectivity
- booking failure spike
- auth failure spike
- notification provider failure
