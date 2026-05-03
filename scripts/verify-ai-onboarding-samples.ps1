param(
    [string]$GoldenPath,
    [string]$SampleDir,
    [string]$ScanEndpoint = "http://localhost/api/ai/onboard/scan",
    [string]$JsonReportPath = "/tmp/ai-onboarding-sample-report.json",
    [string]$MarkdownReportPath = "/tmp/ai-onboarding-sample-report.md",
    [string]$AuthToken,
    [switch]$IncludeNeedsHumanGolden,
    [switch]$RequireSeedDependentGolden,
    [string]$MockScanResponsePath,
    [string[]]$SampleIds,
    [switch]$ReportOnly
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if ([string]::IsNullOrWhiteSpace($GoldenPath)) {
    $GoldenPath = Join-Path $RepoRoot "docs/fixtures/ai-onboarding-sample-golden.json"
}
if ([string]::IsNullOrWhiteSpace($SampleDir)) {
    $SampleDir = Join-Path $RepoRoot "sample"
}
$ActualFieldAliasMap = @{
    "basePrice" = "price"
}

function Normalize-FieldPath {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $Path
    }
    if ($Path.StartsWith("expected.")) {
        return $Path.Substring("expected.".Length)
    }
    return $Path
}

function Resolve-ActualFieldPath {
    param([string]$ExpectedPath)

    $normalizedExpectedPath = Normalize-FieldPath $ExpectedPath
    if ($ActualFieldAliasMap.ContainsKey($normalizedExpectedPath)) {
        return $ActualFieldAliasMap[$normalizedExpectedPath]
    }
    return $normalizedExpectedPath
}

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
    $text = ([string]$Value).Trim()
    $text = $text -replace 'å', '円'
    $text = $text -replace '蜀・', '円/'
    $text = $text -replace '¥', '円'
    $text = $text -replace '￥', '円'
    $text = $text -replace '\s+', ''
    return $text
}

function Test-IsMissingValue {
    param([object]$Value)

    if ($null -eq $Value) {
        return $true
    }
    return ($Value -is [string]) -and [string]::IsNullOrWhiteSpace($Value)
}

function Get-NormalizedOptionalFields {
    param($Tolerance)

    return @(
        @($Tolerance.optionalFields) |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
            ForEach-Object { Normalize-FieldPath $_ }
    )
}

function Get-NormalizedSeedDependentFields {
    param($Tolerance)

    return @(
        @($Tolerance.seedDependentFields) |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
            ForEach-Object { Normalize-FieldPath $_ }
    )
}

function Get-NormalizedSynonyms {
    param($Tolerance)

    $normalized = @{}
    if ($null -eq $Tolerance -or $null -eq $Tolerance.synonyms) {
        return $normalized
    }

    foreach ($property in $Tolerance.synonyms.PSObject.Properties) {
        $normalized[(Normalize-FieldPath $property.Name)] = @($property.Value | ForEach-Object { Normalize-ComparableValue $_ })
    }
    return $normalized
}

function Convert-ToReportCellValue {
    param([object]$Value)

    if ($null -eq $Value) {
        return ""
    }
    if ($Value -is [string] -or $Value -is [ValueType]) {
        return ([string]$Value) -replace '\r?\n', '<br/>' -replace '\|', '\\|'
    }
    return (($Value | ConvertTo-Json -Depth 10 -Compress) -replace '\r?\n', '<br/>') -replace '\|', '\\|'
}

function Get-MockScanResponse {
    param(
        [Parameter(Mandatory = $true)] $Responses,
        [Parameter(Mandatory = $true)][string] $SampleId
    )

    if ($Responses -is [System.Collections.IDictionary]) {
        return $Responses[$SampleId]
    }

    $property = $Responses.PSObject.Properties[$SampleId]
    if ($null -eq $property) {
        return $null
    }
    return $property.Value
}

function Test-FieldMatch {
    param(
        [string]$ExpectedPath,
        [string]$ActualPath,
        [object]$Expected,
        [object]$Actual,
        $Tolerance
    )

    $expectedPath = Normalize-FieldPath $ExpectedPath
    $resolvedActualPath = if ([string]::IsNullOrWhiteSpace($ActualPath)) { Resolve-ActualFieldPath $expectedPath } else { $ActualPath }
    $optionalFields = Get-NormalizedOptionalFields $Tolerance
    $seedDependentFields = Get-NormalizedSeedDependentFields $Tolerance
    $synonyms = Get-NormalizedSynonyms $Tolerance
    $priceTolerance = if ($null -ne $Tolerance.priceTolerance) { [double]$Tolerance.priceTolerance } else { 0.0 }

    if (($seedDependentFields -contains $expectedPath) -and -not $RequireSeedDependentGolden) {
        return [pscustomobject]@{
            field = $expectedPath
            expectedPath = $expectedPath
            actualPath = $resolvedActualPath
            expected = $Expected
            actual = $Actual
            passed = $true
            reason = "seed-dependent-skipped"
        }
    }

    if ($null -eq $Expected) {
        $passed = $optionalFields -contains $expectedPath
        return [pscustomobject]@{
            field = $expectedPath
            expectedPath = $expectedPath
            actualPath = $resolvedActualPath
            expected = $Expected
            actual = $Actual
            passed = $passed
            reason = if ($passed) { "optional-null" } else { "expected-null" }
        }
    }

    if (($optionalFields -contains $expectedPath) -and (Test-IsMissingValue $Actual)) {
        return [pscustomobject]@{
            field = $expectedPath
            expectedPath = $expectedPath
            actualPath = $resolvedActualPath
            expected = $Expected
            actual = $Actual
            passed = $true
            reason = "optional-missing"
        }
    }

    $normalizedExpected = Normalize-ComparableValue $Expected
    $normalizedActual = Normalize-ComparableValue $Actual

    if ($normalizedExpected -is [double] -and $normalizedActual -is [double]) {
        $passed = [math]::Abs($normalizedExpected - $normalizedActual) -le $priceTolerance
        return [pscustomobject]@{
            field = $expectedPath
            expectedPath = $expectedPath
            actualPath = $resolvedActualPath
            expected = $normalizedExpected
            actual = $normalizedActual
            passed = $passed
            reason = if ($passed) { "numeric-within-tolerance" } else { "numeric-mismatch" }
        }
    }

    $candidateValues = @("$normalizedExpected")
    if ($synonyms.ContainsKey($expectedPath)) {
        $candidateValues += @($synonyms[$expectedPath])
    }

    $passed = $candidateValues | Where-Object { "$_" -eq "$normalizedActual" } | Select-Object -First 1
    return [pscustomobject]@{
        field = $expectedPath
        expectedPath = $expectedPath
        actualPath = $resolvedActualPath
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

try {
    $goldenEntries = Get-Content -Raw -Path $GoldenPath | ConvertFrom-Json
    $selectedSampleIds = @($SampleIds | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($selectedSampleIds.Count -gt 0) {
        $goldenEntries = @($goldenEntries | Where-Object { $selectedSampleIds -contains $_.sampleId })
    }

    $mockScanResponses = $null
    if (-not [string]::IsNullOrWhiteSpace($MockScanResponsePath)) {
        $mockScanResponses = Get-Content -Raw -Path $MockScanResponsePath | ConvertFrom-Json
    }

    $results = @()

    foreach ($entry in $goldenEntries) {
        $samplePath = Join-Path $SampleDir $entry.sampleId
        if ($null -eq $mockScanResponses -and -not (Test-Path $samplePath)) {
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

        try {
            if ($null -ne $mockScanResponses) {
                $scanResponse = Get-MockScanResponse -Responses $mockScanResponses -SampleId $entry.sampleId
                if ($null -eq $scanResponse) {
                    throw "Mock response not found for sample: $($entry.sampleId)"
                }
            } else {
                $bytes = [System.IO.File]::ReadAllBytes($samplePath)
                $body = @{
                    base64Image = "data:image/jpeg;base64,$([Convert]::ToBase64String($bytes))"
                    sampleId = $entry.sampleId
                } | ConvertTo-Json -Depth 10
                $headers = @{}
                if (-not [string]::IsNullOrWhiteSpace($AuthToken)) {
                    $headers["Authorization"] = "Bearer $AuthToken"
                }
                $scanResponse = Invoke-RestMethod -Uri $ScanEndpoint -Method Post -ContentType "application/json; charset=utf-8" -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) -TimeoutSec 180
            }

            if ($scanResponse.status -and $scanResponse.status -ne "success") {
                throw "Non-success response: $($scanResponse | ConvertTo-Json -Depth 10 -Compress)"
            }
            $actual = if ($null -ne $scanResponse.data) { $scanResponse.data } else { $scanResponse }
            $fieldResults = @()
            foreach ($field in (Expand-ExpectedFields -Expected $entry.expected)) {
                $expectedPath = Normalize-FieldPath $field.path
                $actualPath = Resolve-ActualFieldPath $expectedPath
                $fieldResults += Test-FieldMatch -ExpectedPath $expectedPath -ActualPath $actualPath -Expected $field.value -Actual (Get-NestedValue -Object $actual -Path $actualPath) -Tolerance $entry.tolerance
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

    $summary = [pscustomobject]@{
        total = @($results).Count
        pass = @($results | Where-Object { $_.status -eq "PASS" }).Count
        fail = @($results | Where-Object { $_.status -eq "FAIL" }).Count
        skipped = @($results | Where-Object { $_.status -eq "NEEDS_HUMAN_GOLDEN" }).Count
        scanFail = @($results | Where-Object { $_.status -eq "SCAN_FAIL" }).Count
        missingFile = @($results | Where-Object { $_.status -eq "MISSING_FILE" }).Count
    }
    $summary | Add-Member -NotePropertyName gatePassed -NotePropertyValue (($summary.fail + $summary.scanFail + $summary.missingFile) -eq 0)

    $report = [pscustomobject]@{
        generatedAt = (Get-Date).ToString("o")
        goldenPath = $GoldenPath
        sampleDir = $SampleDir
        scanEndpoint = $ScanEndpoint
        reportOnly = [bool]$ReportOnly
        summary = $summary
        results = $results
    }

    $report | ConvertTo-Json -Depth 10 | Set-Content -Path $JsonReportPath -Encoding UTF8

    $markdown = @()
    $markdown += "# AI onboarding sample field verification report"
    $markdown += ""
    $modeLabel = if ($ReportOnly) { "report-only (manual report; not a release gate)" } else { "gate (default blocking mode)" }
    $markdown += "- Mode: $modeLabel"
    $markdown += "- Total samples: $($summary.total)"
    $markdown += "- PASS: $($summary.pass)"
    $markdown += "- FAIL: $($summary.fail)"
    $markdown += "- SCAN_FAIL: $($summary.scanFail)"
    $markdown += "- MISSING_FILE: $($summary.missingFile)"
    $markdown += "- SKIPPED(NEEDS_HUMAN_GOLDEN): $($summary.skipped)"
    $markdown += "- gatePassed: $($summary.gatePassed)"
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
        $markdown += "- Status: $($result.status)"
        $markdown += "- Detail: $($result.detail)"
        if ($result.fieldResults.Count -gt 0) {
            $markdown += ""
            $markdown += "| Expected Path | Actual Path | Expected | Actual | Result | Reason |"
            $markdown += "| :-- | :-- | :-- | :-- | :-- | :-- |"
            foreach ($fieldResult in $result.fieldResults) {
                $markdown += "| $($fieldResult.expectedPath) | $($fieldResult.actualPath) | $(Convert-ToReportCellValue $fieldResult.expected) | $(Convert-ToReportCellValue $fieldResult.actual) | $(if ($fieldResult.passed) { 'PASS' } else { 'FAIL' }) | $($fieldResult.reason) |"
            }
            $markdown += ""
        }
    }

    $markdown -join [Environment]::NewLine | Set-Content -Path $MarkdownReportPath -Encoding UTF8

    Write-Host "JSON report: $JsonReportPath"
    Write-Host "Markdown report: $MarkdownReportPath"

    if (-not $summary.gatePassed -and -not $ReportOnly) {
        exit 1
    }
} catch {
    Write-Error $_
    exit 2
}
