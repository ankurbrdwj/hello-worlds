# Spring on Kubernetes

A multi-project learning guide for deploying Spring Boot apps to Kubernetes.

---

## Projects

| Module | Stack | Purpose |
|---|---|---|
| `hello-spring-k8s` | Spring MVC + Actuator | The main service — exposes `/helloWorld` and `/name` |
| `hello-caller` | Spring WebFlux + Actuator | Calls `hello-spring-k8s` over the k8s network |

### hello-spring-k8s endpoints

| Endpoint | Response |
|---|---|
| `GET /helloWorld` | `"Hello World!!"` |
| `GET /name` | A random Beatle name. Also adds a `k8s-host` header showing which pod responded. |
| `GET /actuator/health` | Spring health check (used by k8s probes) |

### hello-caller endpoint

| Endpoint | Response |
|---|---|
| `GET /` | Calls `hello-spring-k8s` via k8s DNS and returns e.g. `"Hello John from pod-abc123"` |

---

## Anatomy of a Kubernetes YAML file

Every k8s resource file shares the same four top-level fields:

```yaml
apiVersion: apps/v1   # which k8s API group/version this resource belongs to
kind: Deployment      # what type of resource this is
metadata:             # name, namespace, labels
  name: my-app
spec:                 # the desired state — this is where most config lives
  ...
```

---

## Resource types used in this project

### Deployment

Tells k8s to run N copies (replicas) of your container and keep them running.

```yaml
# k8s-artifacts/basic/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gs-spring-boot-k8s          # name of this Deployment
  labels:
    app: gs-spring-boot-k8s         # label applied to the Deployment itself
spec:
  replicas: 1                        # how many pod copies to run
  selector:
    matchLabels:
      app: gs-spring-boot-k8s       # which pods THIS deployment manages (must match template labels)
  template:                          # blueprint for each pod
    metadata:
      labels:
        app: gs-spring-boot-k8s     # label on each pod — Service uses this to find pods
    spec:
      containers:
      - name: hello-spring-k8s
        image: spring-k8s/hello-spring-k8s   # Docker image to run
        imagePullPolicy: Never               # use local image (for local dev with minikube)
```

Key idea: the `selector.matchLabels` and the pod `template.labels` must match. That is how the Deployment knows which pods belong to it.

### Service

Gives pods a stable network address. Pods are ephemeral and get new IPs when restarted — a Service in front of them stays constant.

```yaml
# k8s-artifacts/basic/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: gs-spring-boot-k8s          # this name becomes the DNS hostname inside the cluster
spec:
  selector:
    app: gs-spring-boot-k8s         # routes traffic to pods with this label
  ports:
  - port: 80                        # port the Service listens on
    targetPort: 8080                 # port on the pod to forward to
    protocol: TCP
  type: ClusterIP                    # only reachable inside the cluster (default)
```

Key idea: `hello-caller` calls `http://gs-spring-boot-k8s/name` — k8s resolves that hostname to the Service, which load-balances across all matching pods.

### ConfigMap

Stores configuration as key-value pairs or files, separate from your container image. Pods mount it as a file or read it as environment variables.

```yaml
# Used in k8s-artifacts/config_map/
# The application.properties file in that folder gets loaded into a ConfigMap:
#   kubectl create configmap gs-spring-boot-k8s --from-file=application.properties
#
# The deployment then mounts it at /workspace/config so Spring Boot picks it up automatically.
volumeMounts:
  - name: config-volume
    mountPath: /workspace/config     # Spring Boot checks this path for config
volumes:
  - name: config-volume
    configMap:
      name: gs-spring-boot-k8s      # name of the ConfigMap to mount
```

---

## Learning path — k8s-artifacts folders

Work through them in this order:

### 1. basic/
Minimal deployment — just run the container and expose it.
No health checks, no external config.

```
basic/
  deployment.yaml   # 1 replica, no probes
  service.yaml      # ClusterIP service on port 80 -> 8080
```

### 2. best_practice/
Adds liveness and readiness probes. k8s uses these to decide:
- **readiness**: is this pod ready to receive traffic?
- **liveness**: is this pod still alive, or should it be restarted?

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
```

Spring Boot Actuator exposes these endpoints automatically.

### 3. config_map/
Externalises `application.properties` so you can change config without rebuilding the image.

```
config_map/
  application.properties   # mounted into the pod at /workspace/config
  deployment.yaml          # mounts the ConfigMap as a volume
  service.yaml
```

Commands to apply:
```bash
kubectl create configmap gs-spring-boot-k8s --from-file=k8s-artifacts/config_map/application.properties
kubectl apply -f k8s-artifacts/config_map/
```

### 4. service_discovery/
Runs 3 replicas of `hello-spring-k8s` and deploys `hello-caller` alongside it.
`hello-caller` calls `http://gs-spring-boot-k8s/name` — k8s DNS resolves this and load-balances across all 3 pods.
The `k8s-host` response header shows which pod actually handled each request.

```
service_discovery/
  deployment.yaml         # hello-spring-k8s with 3 replicas
  service.yaml            # ClusterIP for hello-spring-k8s
  caller_deployment.yaml  # hello-caller
  caller_service.yaml     # ClusterIP for hello-caller
```

---

## Build & run locally

```bash
# Build both projects
./gradlew build

# Build Docker images (requires minikube)
eval $(minikube docker-env)
docker build -t spring-k8s/hello-spring-k8s hello-spring-k8s/
docker build -t spring-k8s/hello-caller hello-caller/

# Deploy (start with basic)
kubectl apply -f k8s-artifacts/basic/

# Check pods are running
kubectl get pods

# Forward a local port to the service to test it
kubectl port-forward service/gs-spring-boot-k8s 8080:80

# Hit the endpoint
curl http://localhost:8080/helloWorld
curl http://localhost:8080/name

# Clean up
kubectl delete -f k8s-artifacts/basic/
```

---

## How the two services talk to each other

```
[ hello-caller pod ]
        |
        | HTTP GET http://gs-spring-boot-k8s/name
        |
        v
[ gs-spring-boot-k8s Service ]   <-- stable DNS name, load-balances across pods
        |
        +-----> [ hello-spring-k8s pod 1 ]
        +-----> [ hello-spring-k8s pod 2 ]
        +-----> [ hello-spring-k8s pod 3 ]
```

k8s automatically creates a DNS entry for every Service. The format is:
`<service-name>.<namespace>.svc.cluster.local` — or just `<service-name>` within the same namespace.