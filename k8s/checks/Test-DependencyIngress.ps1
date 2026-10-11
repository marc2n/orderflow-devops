$ErrorActionPreference = "Stop"

$namespace = "orderflow-dev"
$image = "acrorderflowmarc2ndev1.azurecr.io/third-party/kafka@sha256:5cc2a2fd93fa2687b44015eee04fb2c3edd9e526bd64bf8bec5ff1e268772e0e"

$clients = @(
    @{ Name = "netcheck-order"; Role = "order-service"; Mysql = $true; Kafka = $true },
    @{ Name = "netcheck-notification"; Role = "notification-service"; Mysql = $false; Kafka = $true },
    @{ Name = "netcheck-init"; Role = "kafka-topic-init"; Mysql = $false; Kafka = $true },
    @{ Name = "netcheck-untrusted"; Role = "untrusted"; Mysql = $false; Kafka = $false }
)

function Get-KubeObject {
    param([string[]]$Arguments)

    $result = & kubectl @Arguments -o json
    if ($LASTEXITCODE -ne 0) {
        throw "Could not read Kubernetes resource."
    }
    return ($result | ConvertFrom-Json)
}

function Test-TcpAccess {
    param(
        [string]$Pod,
        [string]$Address,
        [int]$Port,
        [bool]$ShouldConnect
    )

    # Positional arguments avoid nested PowerShell/bash quoting.
    & kubectl exec $Pod -n $namespace -- `
        timeout 5 /bin/bash -c 'exec 3<>/dev/tcp/$1/$2' `
        -- $Address "$Port"

    $code = $LASTEXITCODE

    if ($ShouldConnect -and $code -eq 0) {
        Write-Host "PASS: $Pod -> ${Address}:$Port allowed"
    }
    elseif (-not $ShouldConnect -and $code -eq 124) {
        Write-Host "PASS: $Pod -> ${Address}:$Port timed out as expected"
    }
    else {
        throw "FAIL: $Pod -> ${Address}:$Port; expected allowed=$ShouldConnect, exit=$code"
    }
}

$created = @()

try {
    $mysql = Get-KubeObject -Arguments @(
        "get", "service", "mysql", "-n", $namespace
    )
    $kafka = Get-KubeObject -Arguments @(
        "get", "service", "kafka", "-n", $namespace
    )
    $broker = Get-KubeObject -Arguments @(
        "get", "pod", "kafka-0", "-n", $namespace
    )

    $mysqlIP = $mysql.spec.clusterIP
    $kafkaIP = $kafka.spec.clusterIP
    $brokerIP = $broker.status.podIP

    if (-not $mysqlIP -or -not $kafkaIP -or -not $brokerIP) {
        throw "Missing destination IP address."
    }

    foreach ($client in $clients) {
        $pod = @{
            apiVersion = "v1"
            kind = "Pod"
            metadata = @{
                name = $client.Name
                namespace = $namespace
                labels = @{
                    "app.kubernetes.io/name" = $client.Role
                    "orderflow.dev/check" = "dependency-ingress"
                }
            }
            spec = @{
                restartPolicy = "Never"
                activeDeadlineSeconds = 600
                automountServiceAccountToken = $false
                enableServiceLinks = $false
                securityContext = @{
                    runAsNonRoot = $true
                    runAsUser = 1000
                    runAsGroup = 1000
                    seccompProfile = @{ type = "RuntimeDefault" }
                }
                containers = @(
                    @{
                        name = "check"
                        image = $image
                        command = @("/bin/sh", "-ec", "sleep 600")
                        securityContext = @{
                            allowPrivilegeEscalation = $false
                            readOnlyRootFilesystem = $true
                            capabilities = @{ drop = @("ALL") }
                        }
                        resources = @{
                            requests = @{ cpu = "10m"; memory = "32Mi" }
                            limits = @{ cpu = "100m"; memory = "64Mi" }
                        }
                    }
                )
            }
        }

        $pod | ConvertTo-Json -Depth 12 | kubectl create -f -
        if ($LASTEXITCODE -ne 0) {
            throw "Could not create $($client.Name)."
        }
        $created += $client.Name
    }

    foreach ($client in $clients) {
        kubectl wait --for=condition=Ready `
            "pod/$($client.Name)" -n $namespace --timeout=120s

        if ($LASTEXITCODE -ne 0) {
            throw "Test pod did not become ready."
        }
    }

    # Allow time for policy propagation before opening fresh connections.
    Start-Sleep -Seconds 5

    # Confirm controller port is listening from the existing broker pod.
    Test-TcpAccess -Pod "kafka-0" `
        -Address $brokerIP -Port 9093 -ShouldConnect $true

    foreach ($client in $clients) {
        Test-TcpAccess -Pod $client.Name `
            -Address $mysqlIP -Port 3306 -ShouldConnect $client.Mysql

        Test-TcpAccess -Pod $client.Name `
            -Address $kafkaIP -Port 9092 -ShouldConnect $client.Kafka

        Test-TcpAccess -Pod $client.Name `
            -Address $brokerIP -Port 9093 -ShouldConnect $false
    }

    Write-Host "PASS: All dependency ingress checks passed."
}
finally {
    foreach ($name in $created) {
        kubectl delete pod $name -n $namespace `
            --ignore-not-found --wait=false
    }
}