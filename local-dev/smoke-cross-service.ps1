# Phase 16 cross-service smoke -- Sten (PowerShell port).
# See smoke-cross-service.sh for the canonical description.
# Exits 0 on Sten success (teammate skips are fine), 1 on a
# Sten failure, 2 on a configured teammate URL that does not answer.

[CmdletBinding()]
param(
    [string]$RestaurantBase    = $env:RESTAURANT_BASE,
    [string]$MenuBase          = $env:MENU_BASE,
    [string]$JwtSecret         = $env:JWT_SECRET,
    [string]$JwtIssuer         = $env:JWT_ISSUER,
    [string]$GatewayBase       = $env:GATEWAY_BASE,
    [string]$UserBase          = $env:USER_BASE,
    [string]$OrderBase         = $env:ORDER_BASE,
    [string]$PaymentBase       = $env:PAYMENT_BASE,
    [string]$DeliveryBase      = $env:DELIVERY_BASE,
    [string]$NotificationBase  = $env:NOTIFICATION_BASE,
    [string]$MenuContainer     = $env:MENU_CONTAINER,
    [string]$EvidenceDir       = $env:EVIDENCE_DIR
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

if ([string]::IsNullOrEmpty($RestaurantBase))  { $RestaurantBase  = 'http://localhost:8081' }
if ([string]::IsNullOrEmpty($MenuBase))        { $MenuBase        = 'http://localhost:8082' }
if ([string]::IsNullOrEmpty($JwtSecret))       { $JwtSecret       = 'dGVzdC1zZWNyZXQtZm9yLWRldi1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZC0xMjM0NTY=' }
if ([string]::IsNullOrEmpty($JwtIssuer))       { $JwtIssuer       = 'quickbite-user-service' }
if ([string]::IsNullOrEmpty($GatewayBase))     { $GatewayBase     = 'http://localhost:8080' }
if ([string]::IsNullOrEmpty($MenuContainer))   { $MenuContainer   = 'quickbite-menu-service' }
if ([string]::IsNullOrEmpty($EvidenceDir))     { $EvidenceDir     = Join-Path $PSScriptRoot 'evidence' }

$script:SierraFailed   = 0
$script:TeammateFailed = 0

function Write-Trace([string]$msg) {
    $ts = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
    $line = "$ts $msg"
    $line | Out-Host
    Add-Content -LiteralPath $TraceFile -Value $line
}
function Write-Info([string]$msg)     { Write-Host "  $msg" }
function Write-Skip([string]$msg)     { Write-Host "SKIP: $msg" -ForegroundColor Yellow }
function Write-FailSierra([string]$m) { Write-Host "SIERRA-FAIL: $m" -ForegroundColor Red; $script:SierraFailed = 1 }
function Write-FailTeam([string]$m)   { Write-Host "TEAMMATE-FAIL: $m" -ForegroundColor Red; $script:TeammateFailed = 1 }

New-Item -ItemType Directory -Force -Path $EvidenceDir | Out-Null
$RunTag    = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')
$TraceFile = Join-Path $EvidenceDir "cross-service-smoke_${RunTag}.log"
New-Item -ItemType File -Force -Path $TraceFile | Out-Null

Write-Trace '== Phase 16 cross-service smoke (PowerShell) =='
Write-Trace "Restaurant        : $RestaurantBase"
Write-Trace "Menu              : $MenuBase"
Write-Trace "Gateway (probe)   : $GatewayBase"
Write-Trace "User (probe)      : $(if ($UserBase) { $UserBase } else { '<unset>' })"
Write-Trace "Order (probe)     : $(if ($OrderBase) { $OrderBase } else { '<unset>' })"
Write-Trace "Payment (probe)   : $(if ($PaymentBase) { $PaymentBase } else { '<unset>' })"
Write-Trace "Delivery (probe)  : $(if ($DeliveryBase) { $DeliveryBase } else { '<unset>' })"
Write-Trace "Notification      : $(if ($NotificationBase) { $NotificationBase } else { '<unset>' })"
$HasDocker = $null -ne (Get-Command docker -ErrorAction SilentlyContinue)
Write-Trace "Docker available  : $(if ($HasDocker) { 'yes' } else { 'no' })"
Write-Trace "Trace file        : $TraceFile"

function ConvertTo-Base64Url([byte[]]$bytes) {
    $b64 = [System.Convert]::ToBase64String($bytes)
    return ($b64.TrimEnd('=') -replace '\+','-' -replace '/','_')
}

function New-DevToken([string]$role, [string]$userId, [string]$subject) {
    $now   = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $exp   = $now + 3600
    $header  = '{"alg":"HS256","typ":"JWT"}'
    $payload = '{"iss":"' + $JwtIssuer + '","sub":"' + $subject +
               '","userId":"' + $userId + '","role":"' + $role +
               '","tokenType":"USER","iat":' + $now + ',"exp":' + $exp + '}'
    $hB64   = ConvertTo-Base64Url ([System.Text.Encoding]::UTF8.GetBytes($header))
    $pB64   = ConvertTo-Base64Url ([System.Text.Encoding]::UTF8.GetBytes($payload))
    $signing  = "$hB64.$pB64"
    $keyBytes = [System.Convert]::FromBase64String($JwtSecret)
    $hmac     = [System.Security.Cryptography.HMACSHA256]::new($keyBytes)
    try {
        $sigBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($signing))
    } finally {
        $hmac.Dispose()
    }
    $sig = ConvertTo-Base64Url $sigBytes
    return "$signing.$sig"
}

function Invoke-SmokeRequest {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Token,
        [object]$Body,
        [int]$ExpectedStatus
    )
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $splat = @{
        Method             = $Method
        Uri                = $Url
        Headers            = $headers
        SkipHttpErrorCheck = $true
        ErrorAction        = 'SilentlyContinue'
    }
    if ($Body) {
        $splat.Body        = ($Body | ConvertTo-Json -Depth 6 -Compress)
        $splat.ContentType = 'application/json'
    }
    $response = $null
    try {
        $response = Invoke-WebRequest @splat
    } catch {
        return @{ ok = $false; status = 0; body = $_.Exception.Message }
    }
    if ($null -eq $response) {
        return @{ ok = $false; status = 0; body = '<no response>' }
    }
    $status = [int]$response.StatusCode
    $ok = ($status -eq $ExpectedStatus)
    $parsed = $null
    if ($response.Content -and -not [string]::IsNullOrWhiteSpace($response.Content)) {
        try { $parsed = $response.Content | ConvertFrom-Json } catch { $parsed = $null }
    }
    return @{ ok = $ok; status = $status; body = $response.Content; json = $parsed }
}

function Test-Reachable([string]$url) {
    try {
        $r = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 2 `
            -SkipHttpErrorCheck -ErrorAction SilentlyContinue
        return ($null -ne $r -and $r.StatusCode -gt 0)
    } catch {
        return $false
    }
}

# -------- Step 1: mint tokens --------
$CustomerUserId = '00000000-0000-0000-0000-0000000000c1'
$OwnerUserId    = '00000000-0000-0000-0000-000000000099'

Write-Trace ''
Write-Trace '[STEP 1] Mint dev tokens (owner + customer)'
$ownerToken    = New-DevToken 'RestaurantOwner' $OwnerUserId    'cross-smoke-owner'
$customerToken = New-DevToken 'Customer'        $CustomerUserId 'cross-smoke-customer'
if ($ownerToken.Length    -lt 100) { Write-FailSierra 'owner token too short' }
if ($customerToken.Length -lt 100) { Write-FailSierra 'customer token too short' }
Write-Info 'owner token minted'
Write-Info 'customer token minted'

# -------- Step 2: Sten W1 portion --------
Write-Trace ''
Write-Trace '[STEP 2] Sten W1 hops'

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$create = Invoke-SmokeRequest -Method POST -Url "$RestaurantBase/restaurants" -Token $ownerToken -ExpectedStatus 201 -Body @{
    name           = "X-Smoke Cafe $suffix"
    address        = 'Main 1'
    city           = 'Tartu'
    latitude       = 58.38
    longitude      = 26.72
    operatingHours = '00:00-23:59'
}
$restaurantId = $null
if (-not $create.ok) {
    Write-FailSierra "POST /restaurants expected 201, got $($create.status)"
} else {
    $restaurantId = $create.json.restaurantId
    Write-Info "restaurant created: $restaurantId"
    Write-Trace "restaurantId=$restaurantId"
}

$menuItemId = $null
if ($restaurantId) {
    $item = Invoke-SmokeRequest -Method POST -Url "$MenuBase/restaurants/$restaurantId/menu-items" -Token $ownerToken -ExpectedStatus 201 -Body @{
        name          = 'X-Smoke Burger'
        description   = 'Phase 16 cross-service fixture.'
        priceAmount   = '9.99'
        priceCurrency = 'EUR'
        category      = 'Main'
        isAvailable   = $true
    }
    if (-not $item.ok) {
        Write-FailSierra "POST menu-item expected 201, got $($item.status)"
    } else {
        $menuItemId = $item.json.menuItemId
        Write-Info "menu item created: $menuItemId"
        Write-Trace "menuItemId=$menuItemId"
    }

    # New restaurants are created with isOpen=false per F.3; flip to open so
    # the availability probe below reflects a "ready to serve" state, matching
    # the equivalent step in smoke.ps1.
    $openResp = Invoke-SmokeRequest -Method PATCH -Url "$RestaurantBase/restaurants/$restaurantId/status" -Token $ownerToken -ExpectedStatus 200 -Body @{
        isOpen = $true
    }
    if (-not $openResp.ok) {
        Write-FailSierra "PATCH /restaurants/{rid}/status expected 200, got $($openResp.status)"
    } else {
        Write-Info 'restaurant toggled open'
    }

    $avail = Invoke-SmokeRequest -Method GET -Url "$RestaurantBase/restaurants/$restaurantId/availability" -Token $customerToken -ExpectedStatus 200
    if (-not $avail.ok) {
        Write-FailSierra "GET availability expected 200, got $($avail.status)"
    } elseif (-not $avail.json.acceptsOrders) {
        Write-FailSierra "acceptsOrders was $($avail.json.acceptsOrders), expected true"
    } else {
        Write-Info 'availability acceptsOrders=true'
    }
}

if ($menuItemId) {
    $validate = Invoke-SmokeRequest -Method POST -Url "$MenuBase/menu-items/validate" -Token $customerToken -ExpectedStatus 200 -Body @{
        items = @(@{ menuItemId = $menuItemId; quantity = 2 })
    }
    if (-not $validate.ok) {
        Write-FailSierra "validate expected 200, got $($validate.status)"
    } elseif (-not $validate.json.allValid) {
        Write-FailSierra "allValid was $($validate.json.allValid), expected true"
    } else {
        Write-Info 'batch validate allValid=true'
    }
}

# -------- Step 3: menu-events exercise --------
Write-Trace ''
Write-Trace '[STEP 3] Toggle availability to exercise the menu-events publisher'

if ($menuItemId) {
    $body = @{
        name          = 'X-Smoke Burger'
        description   = 'Phase 16 cross-service fixture.'
        priceAmount   = '9.99'
        priceCurrency = 'EUR'
        category      = 'Main'
    }
    $off = Invoke-SmokeRequest -Method PUT -Url "$MenuBase/menu-items/$menuItemId" -Token $ownerToken -ExpectedStatus 200 -Body ($body + @{ isAvailable = $false })
    if (-not $off.ok) {
        Write-FailSierra "PUT availability=false expected 200, got $($off.status)"
    } else {
        Write-Info 'availability toggled true -> false'
    }

    $on = Invoke-SmokeRequest -Method PUT -Url "$MenuBase/menu-items/$menuItemId" -Token $ownerToken -ExpectedStatus 200 -Body ($body + @{ isAvailable = $true })
    if (-not $on.ok) {
        Write-FailSierra "PUT availability=true expected 200, got $($on.status)"
    } else {
        Write-Info 'availability toggled false -> true'
    }

    if ($HasDocker) {
        $names = (& docker ps --format '{{.Names}}' 2>$null) -split "`n"
        if ($names -contains $MenuContainer) {
            $logSlice = Join-Path $EvidenceDir "menu-events_${RunTag}.log"
            $logs = & docker logs --since 2m $MenuContainer 2>&1
            $matches = $logs | Select-String -SimpleMatch 'topic=menu-events'
            $matches | ForEach-Object { $_.Line } | Set-Content -LiteralPath $logSlice
            $count = ($matches | Measure-Object).Count
            if ($count -ge 2) {
                Write-Info "captured $count menu-events log lines -> $logSlice"
                Write-Trace "menu-events evidence: $logSlice ($count lines)"
            } else {
                Write-FailSierra "expected >=2 menu-events log lines, found $count"
            }
        } else {
            Write-Skip "docker container '$MenuContainer' not running -- log capture unavailable"
            Write-Trace 'menu-events log capture skipped (container not running)'
        }
    } else {
        Write-Skip 'docker CLI unavailable -- menu-events log capture by-hand only'
        Write-Trace 'menu-events log capture skipped (no docker CLI)'
    }
} else {
    Write-Skip 'no menu item created -- skipping availability toggle exercise'
}

# -------- Step 4: teammate probes --------
Write-Trace ''
Write-Trace '[STEP 4] Teammate service probes (optional)'

if (Test-Reachable "$GatewayBase/actuator/health") {
    Write-Info "gateway reachable at $GatewayBase"
    Write-Trace 'gateway: reachable'
} else {
    Write-Skip "gateway not reachable at $GatewayBase -- probe skipped"
    Write-Trace 'gateway: not reachable'
}

function Probe-Optional([string]$name, [string]$base, [string]$path) {
    if ([string]::IsNullOrEmpty($base)) {
        Write-Skip "${name}: URL unset -- skipping probe"
        Write-Trace "${name}: unset"
        return
    }
    if (Test-Reachable ($base + $path)) {
        Write-Info "$name reachable at $base"
        Write-Trace "${name}: reachable"
    } else {
        Write-FailTeam "$name URL set ($base) but $base$path not reachable"
        Write-Trace "${name}: unreachable"
    }
}

Probe-Optional 'User Service'         $UserBase         '/actuator/health'
Probe-Optional 'Order Service'        $OrderBase        '/actuator/health'
Probe-Optional 'Payment Service'      $PaymentBase      '/actuator/health'
Probe-Optional 'Delivery Service'     $DeliveryBase     '/actuator/health'
Probe-Optional 'Notification Service' $NotificationBase '/actuator/health'

# -------- Summary --------
Write-Trace ''
Write-Trace '[SUMMARY]'
Write-Trace "Sten failures = $script:SierraFailed"
Write-Trace "teammate failures    = $script:TeammateFailed"
Write-Trace "trace file           = $TraceFile"

Write-Host ''
if ($script:SierraFailed -ne 0) {
    Write-Host "FAIL -- Sten-owned steps failed. See $TraceFile." -ForegroundColor Red
    exit 1
}
if ($script:TeammateFailed -ne 0) {
    Write-Host "WARN -- Sten OK; teammate probe(s) failed. See $TraceFile." -ForegroundColor Yellow
    exit 2
}
Write-Host 'OK -- cross-service smoke passed (teammate-owned parts SKIPped if unconfigured).' -ForegroundColor Green
exit 0
