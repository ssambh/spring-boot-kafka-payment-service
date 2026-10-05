# Toyota Financial Services (TFS) - Enterprise System Design & Triage Playbook

This document outlines the comprehensive System Design of the Toyota Financial Services (TFS) application backend, mapped to real-world AWS infrastructure. It also serves as an engineering playbook for triaging the most complex distributed systems defects.

---

## 1. High-Level Enterprise Architecture

When a Toyota customer opens their mobile app to pay their monthly car loan, the request travels through a massive, highly resilient distributed system.

```mermaid
flowchart TD
    Client[Toyota Mobile App] -->|HTTPS POST| WAF[AWS WAF]
    WAF --> APIG[AWS API Gateway]
    
    subgraph Amazon EKS (Kubernetes Cluster)
        APIG -->|Validates JWT| PaymentSVC[Payment Service Pods]
        APIG --> LoanSVC[Loan Service Pods]
    end
    
    subgraph Persistence Layer
        PaymentSVC -->|Save Payment| PayDB[(Payment RDS - PostgreSQL)]
        LoanSVC -->|Update Balance| LoanDB[(Loan RDS - PostgreSQL)]
    end
    
    subgraph Event Streaming
        PaymentSVC -->|Publish Event| MSK[Amazon MSK - Kafka]
        MSK -->|Consume Event| LoanSVC
    end
    
    style APIG fill:#f96,stroke:#333
    style MSK fill:#69b,stroke:#333
    style PayDB fill:#4bd,stroke:#333
    style LoanDB fill:#4bd,stroke:#333
```

---

## 2. Component Deep Dive

### 2.1 The Front Door: AWS WAF & API Gateway
*   **Purpose:** Protect the internal network and route traffic.
*   **Security (AWS WAF):** Before the request even hits the Gateway, the Web Application Firewall inspects the payload for malicious SQL injection patterns and blocks known bot IPs.
*   **Rate Limiting (Redis):** Uses the Token Bucket algorithm (via Redis) to prevent DDoS attacks by capping users to, for example, 50 requests per minute.
*   **Authentication (JWT):** Natively integrates with an Identity Provider (like Amazon Cognito). It cryptographically verifies the user's JWT token signature before allowing the request into the Kubernetes cluster.

### 2.2 The Compute Layer: Amazon EKS (Kubernetes)
*   **Purpose:** Host our Spring Boot microservices (`payment-service`, `loan-service`).
*   **Statelessness:** The Spring Boot containers hold zero data in RAM. This allows Kubernetes to instantly spin up 100 new `payment-service` containers if Black Friday traffic spikes, without any data loss.
*   **Service Mesh / Internal DNS:** Containers find each other not by hardcoded IP addresses, but through Kubernetes' internal DNS (e.g., routing to `http://payment-service.default.svc.cluster.local`).

### 2.3 Data Persistence: Amazon RDS (PostgreSQL)
*   **Purpose:** Permanent, ACID-compliant storage.
*   **Database-per-Service Pattern:** The `payment-service` and `loan-service` do NOT share a database. This prevents a heavy query in the Loan module from locking the database and crashing the Payment module.
*   **Idempotency & Concurrency:** The Payment DB enforces Idempotency (preventing duplicate payments via unique ticket numbers). The Loan DB enforces Optimistic Locking (`@Version`) to prevent concurrent updates from overwriting each other.

### 2.4 Asynchronous Eventing: Amazon MSK (Kafka)
*   **Purpose:** Decoupling services to prevent cascading failures.
*   **Fire-and-Forget:** When `payment-service` saves a payment, it publishes a `PaymentProcessedEvent` to a Kafka Topic and immediately returns a `201 Created` to the user. It does not wait for the `loan-service` to respond.
*   **Resiliency:** If the `loan-service` crashes, the Kafka Topic safely holds the events. When the service boots back up, it resumes consuming exactly where it left off (using its Kafka Offset).

---

## 3. The Backend Engineer's Triage Playbook

In a distributed microservices architecture, bugs are rarely simple syntax errors. They are almost always related to network latency, concurrency, or eventual consistency. Here is how a Senior Backend Engineer triages the most common TFS defects.

### Defect 1: "The user paid, the money left their bank, but their Toyota loan balance didn't update."
*   **The Cause:** This is the classic "Distributed Transaction" failure. The `payment-service` successfully saved to the Payment DB, but the `loan-service` failed to process the Kafka event.
*   **Triage Steps:**
    1.  **Check Consumer Lag:** Look at the Kafka monitoring dashboard. Is the `loan-service` consumer group lagging behind? If lag is 10,000+, the service might be frozen or under-provisioned.
    2.  **Check the DLQ (Dead Letter Queue):** Did the message fail completely and get routed to the `.DLT` topic? 
    3.  **Check the Logs:** Search Datadog/Splunk for the specific `paymentId`. You might see a `ClassNotFoundException` (the `__TypeId__` header issue) or a NullPointerException in the `loan-service` listener.
*   **Resolution:** Fix the bug in `loan-service` and manually replay the messages sitting in the DLQ back into the main topic.

### Defect 2: "User double-clicked the Pay button and was charged twice."
*   **The Cause:** Race condition bypassing the Idempotency check. Two HTTP requests hit the Load Balancer at the exact same millisecond. Request A and Request B both checked the database, both saw "No previous payment", and both executed an `INSERT`.
*   **Triage Steps:**
    1.  Look at the database timestamps. You will see two identical transactions separated by milliseconds.
    2.  Verify if the frontend actually generated and sent a unique `Idempotency-Key` header.
*   **Resolution:** Ensure the database has a `UNIQUE CONSTRAINT` on the `idempotency_key` column. Even if both requests try to insert simultaneously, the database engine will reject the second one with a `DataIntegrityViolationException`.

### Defect 3: "High volume of HTTP 409 Conflict Errors on Loan Updates."
*   **The Cause:** Optimistic Locking exception (`ObjectOptimisticLockingFailureException`). Two different Kafka events tried to update the exact same loan balance simultaneously. Event A updated the `@Version` from 1 to 2. Event B tried to update Version 1, realized it was stale, and failed.
*   **Triage Steps:**
    1.  This is actually a *feature*, not a bug! It means the database successfully prevented corrupted data.
    2.  However, if the error rate is too high, the Kafka Retry mechanism might be failing.
*   **Resolution:** Ensure your Kafka `DefaultErrorHandler` is configured to automatically retry the failed message after a 1-second `FixedBackOff`. On the retry, it will pull the fresh Version 2 from the database and succeed.

### Defect 4: "Kubernetes Pods are randomly restarting (OOMKilled)."
*   **The Cause:** A Memory Leak in the Java application. The Spring Boot app consumed more RAM than the Kubernetes Pod limit allowed, so the Kubelet assassinated the container to protect the server.
*   **Triage Steps:**
    1.  Run `kubectl describe pod <pod-name>` and look for the `Reason: OOMKilled` status.
    2.  Look at JVM metrics (Prometheus/Grafana) to see the heap memory slowly climbing like a staircase without ever dropping.
*   **Resolution:** Generate a Java Heap Dump (`jmap`) just before it crashes. Open it in Eclipse MAT or IntelliJ Profiler to find which objects are not being Garbage Collected (usually unclosed Database Connections, endless Lists, or ThreadLocals).

### Defect 5: "API Gateway returning 429 Too Many Requests for normal users."
*   **The Cause:** The Redis Rate Limiter is configured too aggressively, or users are sharing a corporate IP address.
*   **Triage Steps:**
    1.  Check the API Gateway logs. Which `KeyResolver` is triggering the block?
    2.  If the Gateway uses IP-based rate limiting, a large corporate office (where 50 users share 1 public IP) will trigger the limit instantly.
*   **Resolution:** Change the `KeyResolver` in Spring Cloud Gateway to rate-limit based on the user's *JWT User ID* rather than their IP address, ensuring fair usage per actual customer.
