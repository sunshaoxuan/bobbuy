param(
  [Parameter(Mandatory = $true)]
  [string]$PlainText,

  [string]$Password = $env:BOBBUY_AI_SECRET_MASTER_PASSWORD,

  [int]$Iterations = 200000
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Password)) {
  throw "Set BOBBUY_AI_SECRET_MASTER_PASSWORD or pass -Password. Do not commit the password."
}

if ($Iterations -lt 10000) {
  throw "Iterations must be at least 10000."
}

$salt = [byte[]]::new(16)
$nonce = [byte[]]::new(12)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($salt)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($nonce)

$kdf = [System.Security.Cryptography.Rfc2898DeriveBytes]::new(
  $Password,
  $salt,
  $Iterations,
  [System.Security.Cryptography.HashAlgorithmName]::SHA256
)
$key = $kdf.GetBytes(32)

$plaintextBytes = [System.Text.Encoding]::UTF8.GetBytes($PlainText)
$ciphertext = [byte[]]::new($plaintextBytes.Length)
$tag = [byte[]]::new(16)

$aes = [System.Security.Cryptography.AesGcm]::new($key, 16)
$aes.Encrypt($nonce, $plaintextBytes, $ciphertext, $tag)

[pscustomobject]@{
  BOBBUY_AI_LLM_CODEX_BRIDGE_SECRET_SALT = [Convert]::ToBase64String($salt)
  BOBBUY_AI_LLM_CODEX_BRIDGE_SECRET_NONCE = [Convert]::ToBase64String($nonce)
  BOBBUY_AI_LLM_CODEX_BRIDGE_SECRET_CIPHERTEXT = [Convert]::ToBase64String($ciphertext)
  BOBBUY_AI_LLM_CODEX_BRIDGE_SECRET_TAG = [Convert]::ToBase64String($tag)
  BOBBUY_AI_LLM_CODEX_BRIDGE_SECRET_ITERATIONS = $Iterations
} | ConvertTo-Json
