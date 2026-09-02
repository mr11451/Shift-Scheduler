# ================================
# WSL2 portproxy auto-update script (IPv4 filter)
# ================================

# 対象ポート
$port = 8000

# WSL の IP をすべて取得
$ips = wsl.exe hostname -I

if (-not $ips) {
    Write-Host "WSL が起動していません。"
    exit
}

# IPv4 のみ抽出（IPv6 を除外）
$ipv4_list = $ips -split " " | Where-Object { $_ -match '^\d{1,3}(\.\d{1,3}){3}$' }

# WSL2 の NAT IP（通常 172.x.x.x）を選択
$wsl_ip = $ipv4_list | Where-Object { $_ -match '^172\.' } | Select-Object -First 1

if (-not $wsl_ip) {
    Write-Host "WSL の NAT IPv4 が見つかりません。取得結果: $ips"
    exit
}

Write-Host "使用する WSL IP: $wsl_ip"

# 既存の portproxy を削除
netsh interface portproxy delete v4tov4 listenport=$port listenaddress=0.0.0.0

# 新しい portproxy を追加
netsh interface portproxy add v4tov4 `
    listenport=$port listenaddress=0.0.0.0 `
    connectport=$port connectaddress=$wsl_ip

Write-Host "portproxy を更新しました: 0.0.0.0 : $port → $wsl_ip : $port"

# ファイアウォールルールが無ければ追加
$rule = Get-NetFirewallRule -DisplayName "WSL2 WebApp $port" -ErrorAction SilentlyContinue
if (-not $rule) {
    New-NetFirewallRule -DisplayName "WSL2 WebApp $port" `
        -Direction Inbound -Protocol TCP -LocalPort $port -Action Allow
    Write-Host "ファイアウォールルールを追加しました。"
} else {
    Write-Host "ファイアウォールルールは既に存在します。"
}
