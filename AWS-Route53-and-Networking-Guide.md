# AWS Route 53 & ALB Complete Guide
## Reference Project: Student Service (Spring Boot on EKS)

---

## Project Overview

Your **Student Service** is a Spring Boot microservice that:
- Exposes REST APIs: `POST /students`, `GET /students`, `GET /students/{id}`, `PUT /students/{id}`, `DELETE /students/{id}`
- Uses **DynamoDB** as the database
- Uses **S3** for file storage
- Uses **SQS** for messaging
- Uses **SNS** for notifications
- Deployed on **EKS** (Elastic Kubernetes Service)
- Runs on port `8089`

This entire guide will use this project as the reference throughout.

---

## 1. Application Load Balancer (ALB)

### What is ALB?
ALB is a Layer 7 (HTTP/HTTPS) load balancer that distributes incoming traffic to multiple targets (pods, EC2 instances, IP addresses). It understands HTTP headers, paths, hostnames, and can make intelligent routing decisions.

### How it Works with Student Service

```
User Request
     ↓
[Internet]
     ↓
ALB (alb-student.us-east-1.elb.amazonaws.com)
     ↓
[Target Group: student-service-pods]
     ↓
   Pod 1 (port 8089)
   Pod 2 (port 8089)
   Pod 3 (port 8089)
```

### Why ALB for Student Service?
| Without ALB | With ALB |
|---|---|
| All traffic hits 1 pod → crashes | Traffic split across 3 pods |
| Pod IP changes on restart | ALB always has same DNS endpoint |
| No HTTPS/SSL | ALB handles SSL termination |
| No health checks | ALB auto-removes unhealthy pods |

### ALB Components
| Component | What it is | Student Service Example |
|---|---|---|
| **Listener** | Waits for traffic on a port | Listen on port 443 (HTTPS) |
| **Target Group** | Group of pods/instances | All student-service pods on port 8089 |
| **Rules** | How to route traffic | `/students/*` → student-service target group |
| **Health Check** | Checks if target is healthy | `GET /actuator/health` returns 200 |

### ALB in EKS - How it Works
In EKS, you do NOT create ALB manually in production. You use the **AWS Load Balancer Controller** (an in-cluster operator). You add annotations to your Kubernetes `Ingress` resource and the controller automatically creates the ALB for you.

**Ingress Annotation Example:**
```yaml
annotations:
  kubernetes.io/ingress.class: alb
  alb.ingress.kubernetes.io/scheme: internet-facing
  alb.ingress.kubernetes.io/target-type: ip
```

When Kubernetes sees this Ingress → ALB Controller creates ALB → ALB routes to student-service pods.

### For Demo on Free Tier
- You CAN create an ALB manually for demo purposes
- **Cost**: ALB costs ~$0.008/LCU-hour. Not fully free tier, but minimal cost (~$16-18/month if running 24/7)
- **Recommendation**: Create it, test, then **delete immediately** to avoid charges
- Steps: EC2 Console → Load Balancers → Create ALB → Select internet-facing → Assign to your VPC/subnets → Create Target Group → Register student-service instance

---

## 2. Auto Scaling with ALB

### What is Auto Scaling?
Auto Scaling automatically increases or decreases the number of running instances/pods based on traffic or CPU/memory usage. It ensures your student service is always available and cost-efficient.

### Auto Scaling Types
| Type | Where used | How it works |
|---|---|---|
| **EC2 Auto Scaling** | EC2 instances | Adds/removes EC2 machines |
| **EKS HPA** (Horizontal Pod Autoscaler) | Kubernetes pods | Adds/removes pods |
| **EKS KEDA** | Kubernetes | Scales based on SQS queue depth etc. |

### Student Service Auto Scaling Flow (EKS + ALB)

```
Normal Traffic:
  ALB → 2 Pods (CPU: 20%)

Black Friday / Exam Season (High Traffic):
  1000 students hit GET /students simultaneously
  ALB → CPU spikes to 80% on both pods
  HPA sees CPU > 70% threshold
  HPA scales: 2 pods → 5 pods
  ALB auto-discovers new pods via Target Group
  ALB → 5 Pods (CPU: 30% each)

Traffic drops at night:
  HPA scales down: 5 pods → 2 pods
```

### How ALB + HPA Work Together
1. **ALB** receives all incoming traffic
2. **ALB** distributes to all healthy pods in Target Group
3. **HPA** monitors CPU/memory of student-service pods
4. When CPU > threshold → HPA creates new pods
5. New pods register with ALB Target Group automatically
6. ALB starts sending traffic to new pods too
7. When traffic reduces → HPA removes extra pods
8. ALB stops sending traffic to removed pods

### Launch Template
A **Launch Template** is a saved configuration blueprint for EC2 instances used by Auto Scaling Groups (ASG).

Think of it like a saved "settings file" that says:
- Which AMI (Amazon Machine Image) to use
- Instance type (t3.medium)
- Key pair for SSH
- Security groups
- User data script (startup commands)
- EBS volume size

**Student Service Example:**
```
Launch Template: student-service-lt
  - AMI: Amazon Linux 2 + Java 17
  - Instance Type: t3.medium
  - Security Group: allow port 8089
  - User Data:
      java -jar student-service-1.0.0.jar \
        --spring.profiles.active=dev
```
When ASG needs a new EC2 instance for your student service, it uses this template to launch identical, pre-configured instances automatically.

---

## 3. Route 53

### What is Route 53?
Route 53 is AWS's DNS (Domain Name System) service. DNS converts human-readable domain names like `api.studentservice.com` into IP addresses that computers understand like `54.23.11.4`.

**Real-world analogy**: Route 53 is like a phone book. You look up "Student Service API" and it gives you the actual phone number (IP address) to call.

### How DNS Works - Step by Step (Student Service)

```
You type: https://api.studentservice.com/students

Step 1: Browser checks local DNS cache
Step 2: If not cached → asks your ISP's DNS resolver
Step 3: DNS resolver asks Route 53 Root DNS
Step 4: Route 53 returns IP: 52.45.123.67 (your ALB IP)
Step 5: Browser connects to 52.45.123.67
Step 6: ALB receives request → routes to student-service pod
Step 7: Pod returns list of students
```

### Hosted Zones

A **Hosted Zone** is a container for DNS records for a domain. It's like a folder that holds all DNS settings for `studentservice.com`.

| Type | Use Case | Student Service Example |
|---|---|---|
| **Public Hosted Zone** | For internet-facing domains | `api.studentservice.com` accessible from anywhere |
| **Private Hosted Zone** | For internal VPC DNS only | `student-service.internal` only within your AWS VPC |

**Private Hosted Zone Real Use:**
Inside EKS cluster, your student-service calls another internal service `report-service.internal` — this resolves only within your VPC, not from the internet.

### DNS Record Types

| Record Type | Purpose | Student Service Example |
|---|---|---|
| **A** | Maps domain → IPv4 address | `api.studentservice.com → 54.23.11.4` |
| **AAAA** | Maps domain → IPv6 address | `api.studentservice.com → 2001:db8::1` |
| **CNAME** | Maps domain → another domain | `www.studentservice.com → studentservice.com` |
| **MX** | Mail exchange server | `studentservice.com → mail.studentservice.com` |
| **TXT** | Text info (SPF, domain verification) | `"v=spf1 include:amazon.com ~all"` |
| **NS** | Name server records | `studentservice.com → ns1.amazonaws.com` |
| **SOA** | Start of authority | Auto-created by Route 53 |
| **PTR** | Reverse DNS lookup | `4.11.23.54 → api.studentservice.com` |
| **SRV** | Service locator | `_http._tcp.studentservice.com` |
| **CAA** | Certificate authority | `studentservice.com → amazon.com` |
| **ALIAS** | AWS specific → maps to AWS resources | `api.studentservice.com → alb-123.us-east-1.elb.amazonaws.com` |

### CNAME vs ALIAS — Key Difference

| Point | CNAME | ALIAS |
|---|---|---|
| **What it maps to** | Another domain name | AWS resource (ALB, CloudFront, S3) |
| **Used at root domain?** | ❌ Cannot use at root `studentservice.com` | ✅ Can use at root domain |
| **Extra DNS lookup?** | Yes - 2 lookups (slower) | No - resolves in 1 step (faster) |
| **AWS charges for queries?** | Yes - you pay per query | ✅ Free for AWS resource queries |
| **Real example** | `www.studentservice.com → studentservice.com` | `studentservice.com → alb-xyz.elb.amazonaws.com` |
| **When to use** | Redirect subdomain to another domain | Always when pointing to ALB, CloudFront, S3 |

**Student Service Rule of Thumb:**
- Pointing to ALB → use **ALIAS**
- Pointing to CloudFront → use **ALIAS**
- Redirecting `www` to root → use **CNAME**

---

## 4. Route 53 Routing Policies

These control HOW traffic is routed when multiple records exist.

### 4.1 Simple Routing
- One record, one destination
- **Student Service**: `api.studentservice.com → ALB`
- No intelligence, just direct mapping

### 4.2 Weighted Routing
- Split traffic by percentage
- **Student Service Use Case**: Blue/Green Deployment
```
v1 (stable): 90% traffic → student-service v1 pods
v2 (new):    10% traffic → student-service v2 pods
(gradually shift to 100% v2 once confirmed stable)
```

### 4.3 Latency-Based Routing
- Route users to the AWS region with lowest latency
- **Student Service Use Case**: Global deployment
```
Indian students    → ap-south-1 (Mumbai) ALB
US students        → us-east-1 (N.Virginia) ALB
European students  → eu-west-1 (Ireland) ALB
```

### 4.4 Failover Routing
- Primary + Secondary setup
- If primary health check fails → automatically switch to secondary
- **Student Service Use Case**:
```
Primary:   us-east-1 EKS cluster (active)
Secondary: us-west-2 EKS cluster (standby)
If us-east-1 goes down → Route 53 sends all traffic to us-west-2 automatically
```

### 4.5 Geolocation Routing
- Route based on user's geographic location
- **Student Service Use Case**:
```
Users from India   → India-specific endpoint (data residency compliance)
Users from EU      → EU endpoint (GDPR compliance)
Rest of the world  → Default endpoint
```

### 4.6 Multi-Value Answer Routing
- Returns multiple IP addresses, client picks one
- Like simple load balancing at DNS level
- Can attach health checks per record
- **Not a replacement for ALB** — ALB is smarter

---

## 5. Route 53 Health Checks

### What it does
Route 53 continuously pings your student service endpoint and checks if it's healthy.

### Types
| Type | How it works | Student Service Example |
|---|---|---|
| **Endpoint Health Check** | HTTP/HTTPS request to your endpoint | Checks `GET /actuator/health` returns 200 |
| **Calculated Health Check** | Combines multiple health checks | student-service healthy AND database healthy |
| **CloudWatch Alarm** | Based on CloudWatch metric alarm | DynamoDB latency alarm triggers unhealthy |

### Health Check + Failover Flow
```
Route 53 checks: GET https://api.studentservice.com/actuator/health
  ↓
Response: 200 OK {"status":"UP"} → HEALTHY → send traffic here

Later:
  ↓
Response: 503 / Timeout → UNHEALTHY
  ↓
Route 53 activates Failover Record → traffic shifts to backup region
  ↓
Alert sent to team via CloudWatch / SNS
```

---

## 6. Steps to Set Up Route 53 for Student Service

### Step 1: Register Domain or Use Existing
- Route 53 Console → Registered Domains → Register Domain
- Example: `studentservice.com` ($12/year for .com)
- OR use any domain you already own

### Step 2: Create Public Hosted Zone
- Route 53 Console → Hosted Zones → Create Hosted Zone
- Domain Name: `studentservice.com`
- Type: Public Hosted Zone
- Route 53 auto-creates NS and SOA records

### Step 3: Update Name Servers (if domain from outside)
- Copy the 4 NS records Route 53 gives you
- Go to your domain registrar (GoDaddy/Namecheap etc.)
- Replace their name servers with Route 53's NS records

### Step 4: Create ALIAS Record for ALB
- In your Hosted Zone → Create Record
- Record Name: `api` (creates `api.studentservice.com`)
- Record Type: `A`
- Toggle: Enable **Alias**
- Alias target: Select `Application Load Balancer` → select your region → select your ALB
- Routing Policy: Simple
- Click Create

### Step 5: Create Health Check
- Route 53 → Health Checks → Create Health Check
- Name: `student-service-health`
- Monitor: Endpoint
- Protocol: HTTPS
- Domain: `api.studentservice.com`
- Path: `/actuator/health`
- Check every: 30 seconds

### Step 6: Test
```bash
# Check DNS resolution
nslookup api.studentservice.com

# Call your API via domain
curl https://api.studentservice.com/students
```

---

## 7. Route 53 + ALB + EKS Full Architecture

```
[Student Browser]
       ↓  https://api.studentservice.com/students
[Route 53]
  ALIAS Record: api.studentservice.com → ALB DNS
       ↓
[ALB: alb-xyz.us-east-1.elb.amazonaws.com]
  Listener: 443 (HTTPS) → SSL Certificate (ACM)
  Rule: /students/* → Target Group: student-pods
       ↓
[EKS Cluster]
  Namespace: student-ns
  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
  │  Pod 1      │  │  Pod 2      │  │  Pod 3      │
  │  port:8089  │  │  port:8089  │  │  port:8089  │
  └─────────────┘  └─────────────┘  └─────────────┘
       ↓
[DynamoDB]   [S3: s3-reenas-bucket]   [SQS: student-queue]   [SNS: student-sns]
```

---

## 8. Free Tier Considerations

| Service | Free Tier | Cost After |
|---|---|---|
| **Route 53 Hosted Zone** | ❌ $0.50/month/zone | $0.50/month |
| **Route 53 DNS Queries** | First 1B queries free | $0.40/million |
| **Route 53 Health Check** | ❌ $0.50/month | $0.50/month |
| **ALB** | ❌ Not free tier | ~$16-18/month |
| **EKS Cluster** | ❌ $0.10/hour | ~$72/month |
| **EC2 (t2.micro)** | ✅ 750 hours/month free | After 750hrs |

**Recommendation**:
- Route 53 demo: ~$1/month total — acceptable for learning
- ALB: Create → test for 1-2 hours → delete to avoid charges
- If free tier expiring: Decide based on whether you need live AWS practice. For documentation/learning, a personal account with careful cleanup is fine.

---

## 9. Route 53 Resolver (Advanced - Hybrid Cloud)

When your student service on EKS needs to talk to an **on-premise database** (like Daimler's internal systems):

```
EKS Pod (student-service)
  ↓
Needs to connect to: db.internal.daimler.com
  ↓
Route 53 Resolver: Outbound Endpoint
  ↓
Forwards DNS query through VPN/Direct Connect
  ↓
On-Premise DNS Server → resolves db.internal.daimler.com
  ↓
Connection established
```

---

## 10. Summary Table

| Concept | What it is | Student Service Use |
|---|---|---|
| **ALB** | Layer 7 load balancer | Distributes traffic to student-service pods |
| **Target Group** | Group of ALB targets | All student-service pods on port 8089 |
| **Launch Template** | EC2 config blueprint | Pre-configured student-service EC2 instances |
| **Auto Scaling** | Auto add/remove instances | Scale pods based on student API traffic |
| **HPA** | Kubernetes pod scaler | Scale from 2→5 pods on high exam traffic |
| **Route 53** | AWS DNS service | `api.studentservice.com` → ALB |
| **Hosted Zone** | DNS record container | All records for `studentservice.com` |
| **ALIAS Record** | AWS-specific DNS record | Point domain to ALB for free |
| **CNAME** | Domain → domain redirect | `www` → root domain |
| **Health Check** | Monitors endpoint health | Check `/actuator/health` every 30s |
| **Failover Routing** | Auto switch on failure | us-east-1 down → switch to us-west-2 |
| **Weighted Routing** | Split traffic by % | 90/10 blue-green deployment |
| **Latency Routing** | Route to nearest region | India → Mumbai, US → Virginia |

