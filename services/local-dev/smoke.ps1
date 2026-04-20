# Sierra-Lima Checkpoint #1 smoke test -- Phase 11 §5 (PowerShell port).
# Runs the same happy-path flow as smoke.sh: mint dev token, create restaurant,
# add menu item, toggle status, call availability, call batch validate.
# Exits 0 only if every step passes.

[CmdletBinding()]
param(
    [string]$RestaurantBase = $env:RESTAURANT_BASE,
    [string]$MenuBase       = $env:MENU_BASE,
    [string]$JwtSecret      = $env:JWT_SECRET,
    [string]$JwtIssuer      = $env:JWT_ISSUER
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrEmpty($RestaurantBase)) { $RestaurantBase = 'http://localhost:8081' }
if ([string]::IsNullOrEmpty($MenuBase))       { $MenuBase       = 'http://localhost:8082' }
if ([string]::IsNullOrEmpty($JwtSecret))      { $JwtSecret      = 'dGVzdC1zZWNyZXQtZm9yLWRldi1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZC0xMjM0NTY=' }
if ([string]::IsNullOrEmpty($JwtIssuer))      { $JwtIssuer      = 'quickbite-user-service' }

$CustomerUserId = '00000000-0000-0000-0000-0000000000c1'
$OwnerUserId    = '00000000-0000-0000-0000-000000000099'

function Write-Fail([string]$msg) {
    Write-Host "FAIL: $msg" -ForegroundColor Red
    exit 1
}
function Write-Info([string]$msg) { Write-Host "  $msg" }

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
    $signing = "$hB64.$pB64"
    $keyBytes = [System.Convert]::FromBase64String($JwtSecret)
    $hmac = [System.Security.Cryptography.HMACSHA256]::new($keyBytes)
    try {
        $sigBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($signing))
    } finally {
        $hmac.Dispose()
    }
    $sig = ConvertTo-Base64Url $sigBytes
    return "$signing.$sig"
}

function Invoke-JsonRequest {
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
    }
    if ($Body) {
        $splat.Body        = ($Body | ConvertTo-Json -Depth 6 -Compress)
        $splat.ContentType = 'application/json'
    }
    $response = Invoke-WebRequest @splat
    $status = [int]$response.StatusCode
    if ($status -ne $ExpectedStatus) {
        Write-Fail "$Method $Url expected HTTP $ExpectedStatus, got $status. Body: $($response.Content)"
    }
    if ([string]::IsNullOrWhiteSpace($response.Content)) { return $null }
    return ($response.Content | ConvertFrom-Json)
}

Write-Host "== Sierra-Lima smoke test =="
Write-Host "Restaurant Service : $RestaurantBase"
Write-Host "Menu Service       : $MenuBase"
Write-Host ""

# -------- Step 1-2: mint tokens --------
$ownerToken    = New-DevToken 'RestaurantOwner' $OwnerUserId    'smoke-owner'
$customerToken = New-DevToken 'Customer'        $CustomerUserId 'smoke-customer'
if ($ownerToken.Length    -lt 100) { Write-Fail 'owner token looks too short' }
if ($customerToken.Length -lt 100) { Write-Fail 'customer token looks too short' }
Write-Info 'owner token minted'
Write-Info 'customer token minted'

# -------- Step 3: create restaurant --------
$suffix      = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$restaurant  = Invoke-JsonRequest -Method POST -Url "$RestaurantBase/restaurants" -Token $ownerToken -ExpectedStatus 201 -Body @{
    name           = "Smoke Cafe $suffix"
    address        = 'Main 1'
    city           = 'Tartu'
    latitude       = 58.38
    longitude      = 26.72
    operatingHours = '00:00-23:59'
}
$restaurantId = $restaurant.restaurantId
Write-Info "restaurant created: $restaurantId"

# -------- Step 4: add menu item --------
$menuItem = Invoke-JsonRequest -Method POST -Url "$MenuBase/restaurants/$restaurantId/menu-items" -Token $ownerToken -ExpectedStatus 201 -Body @{
    name          = 'Smoke Burger'
    description   = 'Smoke test fixture.'
    priceAmount   = '9.99'
    priceCurrency = 'EUR'
    category      = 'Main'
    isAvailable   = $true
}
$menuItemId = $menuItem.menuItemId
Write-Info "menu item created: $menuItemId"

# -------- Step 5: toggle status closed -> open --------
Invoke-JsonRequest -Method PATCH -Url "$RestaurantBase/restaurants/$restaurantId/status" -Token $ownerToken -ExpectedStatus 200 -Body @{ isOpen = $false } | Out-Null
$opened = Invoke-JsonRequest -Method PATCH -Url "$RestaurantBase/restaurants/$restaurantId/status" -Token $ownerToken -ExpectedStatus 200 -Body @{ isOpen = $true }
if (-not $opened.isOpen) { Write-Fail "expected isOpen=true after toggle, got $($opened.isOpen)" }
Write-Info 'status toggled closed -> open'

# -------- Step 6: availability --------
$availability = Invoke-JsonRequest -Method GET -Url "$RestaurantBase/restaurants/$restaurantId/availability" -Token $customerToken -ExpectedStatus 200
if (-not $availability.acceptsOrders) { Write-Fail "expected acceptsOrders=true, got $($availability.acceptsOrders)" }
Write-Info 'availability acceptsOrders=true'

# -------- Step 7: batch validate --------
$validate = Invoke-JsonRequest -Method POST -Url "$MenuBase/menu-items/validate" -Token $customerToken -ExpectedStatus 200 -Body @{
    items = @(@{ menuItemId = $menuItemId; quantity = 2 })
}
if (-not $validate.allValid) { Write-Fail "expected allValid=true, got $($validate.allValid)" }
Write-Info 'batch validate allValid=true'

Write-Host ''
Write-Host 'OK -- Sierra-Lima smoke test passed.' -ForegroundColor Green
exit 0
