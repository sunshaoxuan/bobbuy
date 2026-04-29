param(
    [string]$GoldenPath = "/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-golden.json",
    [string]$SampleDir = "/home/runner/work/bobbuy/bobbuy/sample",
    [string]$ScanEndpoint = "http://localhost/api/ai/onboard/scan",
    [string]$JsonReportPath = "/tmp/ai-onboarding-sample-report.json",
    [string]$MarkdownReportPath = "/tmp/ai-onboarding-sample-report.md",
    [switch]$IncludeNeedsHumanGolden
)

$ErrorActionPreference = "Stop"

function Get-NestedValue {
    param(
        [Parameter(Mandatory = $true)] $Object,
        [Parameter(Mandatory = $true)][string] $Path
    )

    $current = $Object
    foreach ($segment in $Path.Split('.')) {
        if ($null -eq $current) {
            return $null
        }
        if ($current -is [System.Collections.IDictionary]) {
            if (-not $current.Contains($segment)) {
                return $null
            }
            $current = $current[$segment]
            continue
        }
        $property = $current.PSObject.Properties[$segment]
        if ($null -eq $property) {
            return $null
        }
        $current = $property.Value
    }
    return $current
}

function Normalize-ComparableValue {
    param([object]$Value)

    if ($null -eq $Value) {
        return $null
    }
    if ($Value -is [double] -or $Value -is [decimal] -or $Value -is [int] -or $Value -is [long]) {
        return [double]$Value
    }
    return ([string]$Value).Trim()
}

function Test-FieldMatch {
    param(
        [string]$Path,
        [object]$Expected,
        [object]$Actual,
        $Tolerance
    )

    $optionalFields = @($Tolerance.optionalFields)
    $synonyms = $Tolerance.synonyms
    $priceTolerance = if ($null -ne $Tolerance.priceTolerance) { [double]$Tolerance.priceTolerance } else { 0.0 }

    if ($null -eq $Expected) {
        return [pscustomobject]@{
            field = $Path
            expected = $Expected
            actual = $Actual
            passed = ($optionalFields -contains $Path)
            reason = if ($optionalFields -contains $Path) { "optional-null" } else { "expected-null" }
        }
    }

    $normalizedExpected = Normalize-ComparableValue $Expected
    $normalizedActual = Normalize-ComparableValue $Actual

    if ($normalizedExpected -is [double] -and $normalizedActual -is [double]) {
        $passed = [math]::Abs($normalizedExpected - $normalizedActual) -le $priceTolerance
        return [pscustomobject]@{
            field = $Path
            expected = $normalizedExpected
            actual = $normalizedActual
            passed = $passed
            reason = if ($passed) { "numeric-within-tolerance" } else { "numeric-mismatch" }
        }
    }

    $candidateValues = @("$normalizedExpected")
    if ($synonyms -and $synonyms.PSObject.Properties[$Path]) {
        $candidateValues += @($synonyms.PSObject.Properties[$Path].Value)
    }

    $passed = $candidateValues | Where-Object { "$_" -eq "$normalizedActual" } | Select-Object -First 1
    return [pscustomobject]@{
        field = $Path
        expected = $normalizedExpected
        actual = $normalizedActual
        passed = [bool]$passed
        reason = if ($passed) { "exact-or-synonym" } else { "string-mismatch" }
    }
}

function Expand-ExpectedFields {
    param(
        [Parameter(Mandatory = $true)] $Expected,
        [string]$Prefix = ""
    )

    $results = @()
    foreach ($property in $Expected.PSObject.Properties) {
        $path = if ([string]::IsNullOrWhiteSpace($Prefix)) { $property.Name } else { "$Prefix.$($property.Name)" }
        $value = $property.Value
        if ($null -ne $value -and $value -isnot [string] -and $value -isnot [ValueType] -and $value.PSObject.Properties.Count -gt 0) {
            $results += Expand-ExpectedFields -Expected $value -Prefix $path
        } else {
            $results += [pscustomobject]@{ path = $path; value = $value }
        }
    }
    return $results
}

$goldenEntries = Get-Content -Raw -Path $GoldenPath | ConvertFrom-Json
$results = @()

foreach ($entry in $goldenEntries) {
    $samplePath = Join-Path $SampleDir $entry.sampleId
    if (-not (Test-Path $samplePath)) {
        $results += [pscustomobject]@{
            sampleId = $entry.sampleId
            status = "MISSING_FILE"
            needsHumanGolden = [bool]$entry.needsHumanGolden
            detail = "Sample file not found"
            fieldResults = @()
        }
        continue
    }

    if ($entry.needsHumanGolden -and -not $IncludeNeedsHumanGolden) {
        $results += [pscustomobject]@{
            sampleId = $entry.sampleId
            status = "NEEDS_HUMAN_GOLDEN"
            needsHumanGolden = $true
            detail = $entry.description
            fieldResults = @()
        }
        continue
    }

    $bytes = [System.IO.File]::ReadAllBytes($samplePath)
    $body = @{
        base64Image = "data:image/jpeg;base64,$([Convert]::ToBase64String($bytes))"
        sampleId = $entry.sampleId
    } | ConvertTo-Json -Depth 10

    try {
        $scanResponse = Invoke-RestMethod -Uri $ScanEndpoint -Method Post -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -TimeoutSec 180
        if ($scanResponse.status -ne "success") {
            throw "Non-success response: $($scanResponse | ConvertTo-Json -Depth 10 -Compress)"
        }
        $actual = $scanResponse.data
        $fieldResults = @()
        foreach ($field in (Expand-ExpectedFields -Expected $entry.expected)) {
            $fieldResults += Test-FieldMatch -Path $field.path -Expected $field.value -Actual (Get-NestedValue -Object $actual -Path $field.path) -Tolerance $entry.tolerance
        }

        $failedCount = @($fieldResults | Where-Object { -not $_.passed }).Count
        $results += [pscustomobject]@{
            sampleId = $entry.sampleId
            status = if ($failedCount -eq 0) { "PASS" } else { "FAIL" }
            needsHumanGolden = [bool]$entry.needsHumanGolden
            detail = $entry.description
            actual = [pscustomobject]@{
                name = $actual.name
                brand = $actual.brand
                itemNumber = $actual.itemNumber
                basePrice = $actual.price
                categoryId = $actual.categoryId
                attributes = $actual.attributes
                traceStage = $actual.trace.stage
                ocrProvider = if ($actual.trace.events.Count -gt 0) { $actual.trace.events[0].provider } else { $null }
                llmProvider = $actual.trace.provider
                fallbackReason = $actual.trace.fallbackReason
            }
            fieldResults = $fieldResults
        }
    } catch {
        $results += [pscustomobject]@{
            sampleId = $entry.sampleId
            status = "SCAN_FAIL"
            needsHumanGolden = [bool]$entry.needsHumanGolden
            detail = $_.Exception.Message
            fieldResults = @()
        }
    }
}

$report = [pscustomobject]@{
    generatedAt = (Get-Date).ToString("o")
    goldenPath = $GoldenPath
    sampleDir = $SampleDir
    scanEndpoint = $ScanEndpoint
    results = $results
}

$report | ConvertTo-Json -Depth 10 | Set-Content -Path $JsonReportPath -Encoding UTF8

$markdown = @()
$markdown += "# AI 商品字段识别样例验证报告"
$markdown += ""
$markdown += "| Sample | Status | Trace Stage | OCR/LLM | Fallback |"
$markdown += "| :-- | :-- | :-- | :-- | :-- |"
foreach ($result in $results) {
    $actual = $result.actual
    $markdown += "| $($result.sampleId) | $($result.status) | $($actual.traceStage) | $($actual.ocrProvider)/$($actual.llmProvider) | $($actual.fallbackReason) |"
}
$markdown += ""
foreach ($result in $results) {
    $markdown += "## $($result.sampleId)"
    $markdown += ""
    $markdown += "- 状态：$($result.status)"
    $markdown += "- 说明：$($result.detail)"
    if ($result.fieldResults.Count -gt 0) {
        $markdown += ""
        $markdown += "| 字段 | 期望 | 实际 | 结果 | 原因 |"
        $markdown += "| :-- | :-- | :-- | :-- | :-- |"
        foreach ($fieldResult in $result.fieldResults) {
            $markdown += "| $($fieldResult.field) | $($fieldResult.expected) | $($fieldResult.actual) | $(if ($fieldResult.passed) { 'PASS' } else { 'FAIL' }) | $($fieldResult.reason) |"
        }
        $markdown += ""
    }
}

$markdown -join [Environment]::NewLine | Set-Content -Path $MarkdownReportPath -Encoding UTF8

Write-Host "JSON report: $JsonReportPath"
Write-Host "Markdown report: $MarkdownReportPath"
