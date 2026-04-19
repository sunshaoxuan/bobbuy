$backendUrl = "http://localhost:8080"

function Post-Json($url, $body) {
    $json = $body | ConvertTo-Json -Depth 10
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $response = Invoke-RestMethod -Uri "$backendUrl$url" -Method Post -ContentType "application/json; charset=utf-8" -Body $bytes
    return $response
}

# 1. Create Products
Write-Host "Seeding Products..."
$p1 = Post-Json "/api/ai/onboard/confirm" @{
    name = @{ "ja-JP" = "Shiroi Koibito"; "en-US" = "Shiroi Koibito"; "zh-CN" = "白色恋人" }
    brand = "ISHIYA"
    basePrice = 1522.0
    categoryId = "cat-food"
    itemNumber = "SKU-JPN-001"
    isRecommended = $true
    mediaGallery = @(
        @{ url = "https://images.unsplash.com/photo-1549122718-da11377b949b"; type = "image" }
    )
}

$p2 = Post-Json "/api/ai/onboard/confirm" @{
    name = @{ "ja-JP" = "Ichiran Ramen"; "en-US" = "Ichiran Ramen"; "zh-CN" = "一兰拉面" }
    brand = "Ichiran"
    basePrice = 2100.0
    categoryId = "cat-food"
    itemNumber = "SKU-JPN-002"
    isRecommended = $true
    mediaGallery = @(
        @{ url = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624"; type = "image" }
    )
}

$p3 = Post-Json "/api/ai/onboard/confirm" @{
    name = @{ "ja-JP" = "SK-II Essence"; "en-US" = "SK-II Essence"; "zh-CN" = "SK-II 神仙水" }
    brand = "SK-II"
    basePrice = 28000.0
    categoryId = "cat-beauty"
    itemNumber = "SKU-JPN-003"
    isRecommended = $false
    mediaGallery = @(
        @{ url = "https://images.unsplash.com/photo-1556229010-6c3f2c9ca5f8"; type = "image" }
    )
}

# 2. Create Trips
Write-Host "Seeding Trips..."
$trip1 = Post-Json "/api/trips" @{
    agentId = 1
    origin = "Tokyo"
    destination = "Shanghai"
    departDate = "2026-05-01"
    capacity = 100.0
    reservedCapacity = 10.0
}

# 3. Create Orders
Write-Host "Seeding Orders..."
$order1 = Post-Json "/api/orders" @{
    businessId = "B2B-2026-0501-001"
    customerId = 2
    desiredDeliveryWindow = "Early May"
    status = "NEW"
    lines = @(
        @{ skuId = "SKU-JPN-001"; itemName = "Shiroi Koibito"; quantity = 5; unitPrice = 1522.0 }
    )
}

Write-Host "Seeding Completed."
