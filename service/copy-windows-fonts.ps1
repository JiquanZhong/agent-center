# Windows中文字体复制脚本
# 复制Windows系统中已安装的中文字体到./fonts目录

Write-Host "正在从Windows系统复制中文字体..." -ForegroundColor Green

# 创建字体目录
$fontsDir = ".\fonts"
if (!(Test-Path $fontsDir)) {
    New-Item -ItemType Directory -Path $fontsDir -Force | Out-Null
    Write-Host "创建字体目录: $fontsDir" -ForegroundColor Yellow
}

# 清理旧文件
Write-Host "清理旧字体文件..." -ForegroundColor Yellow
Get-ChildItem -Path $fontsDir -File | Remove-Item -Force

# Windows字体目录
$windowsFontsPath = "$env:SystemRoot\Fonts"
$userFontsPath = "$env:LOCALAPPDATA\Microsoft\Windows\Fonts"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "开始复制中文字体文件..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 定义要复制的中文字体文件名（常见的Windows中文字体）
$chineseFonts = @(
    # 微软雅黑系列
    "msyh.ttc",           # 微软雅黑
    "msyhbd.ttc",         # 微软雅黑 粗体
    "msyhl.ttc",          # 微软雅黑 Light
    
    # 宋体系列
    "simsun.ttc",         # 宋体 & 新宋体
    "simsunb.ttf",        # 宋体 粗体
    
    # 黑体
    "simhei.ttf",         # 黑体
    
    # 楷体
    "simkai.ttf",         # 楷体
    
    # 仿宋
    "simfang.ttf",        # 仿宋
    
    # 其他中文字体
    "STXIHEI.TTF",        # 华文细黑
    "STZHONGS.TTF",       # 华文中宋
    "STKAITI.TTF",        # 华文楷体
    "STFANGSO.TTF",       # 华文仿宋
    "STSONG.TTF",         # 华文宋体
    
    # 微软正黑体（如果有的话）
    "msjh.ttc",           # 微软正黑体
    "msjhbd.ttc",         # 微软正黑体 粗体
    
    # 等线体
    "Deng.ttf",           # 等线 Regular
    "Dengb.ttf",          # 等线 Bold
    "Dengl.ttf"           # 等线 Light
)

$copiedCount = 0
$totalSize = 0

# 复制系统字体
foreach ($fontFile in $chineseFonts) {
    $systemFontPath = Join-Path $windowsFontsPath $fontFile
    $userFontPath = Join-Path $userFontsPath $fontFile
    $targetPath = Join-Path $fontsDir $fontFile
    
    $sourcePath = $null
    
    # 优先从系统字体目录查找
    if (Test-Path $systemFontPath) {
        $sourcePath = $systemFontPath
    }
    # 其次从用户字体目录查找
    elseif (Test-Path $userFontPath) {
        $sourcePath = $userFontPath
    }
    
    if ($sourcePath) {
        try {
            Copy-Item -Path $sourcePath -Destination $targetPath -Force
            $fileInfo = Get-Item $targetPath
            $fileSizeMB = [math]::Round($fileInfo.Length / 1MB, 2)
            $totalSize += $fileInfo.Length
            Write-Host "✓ 复制成功: $fontFile ($fileSizeMB MB)" -ForegroundColor Green
            $copiedCount++
        }
        catch {
            Write-Host "✗ 复制失败: $fontFile - $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    else {
        Write-Host "○ 未找到: $fontFile" -ForegroundColor Gray
    }
}

# 查找其他可能的中文字体文件
Write-Host "`n查找其他中文字体..." -ForegroundColor Yellow
$otherChineseFonts = Get-ChildItem -Path $windowsFontsPath -File | Where-Object {
    $_.Name -match "(微软|宋体|黑体|楷体|仿宋|雅黑|华文|方正|汉仪|文泉驿|思源|noto.*cjk)" -or
    $_.Name -match "^(MS|MJ|DFKai|PMing|KaiTi|FangSong|SimSun|SimHei)" -or
    $_.Extension -in @(".ttc", ".ttf", ".otf") -and $_.Length -gt 1MB
} | Select-Object -First 10  # 限制最多10个额外字体

foreach ($font in $otherChineseFonts) {
    $targetPath = Join-Path $fontsDir $font.Name
    if (!(Test-Path $targetPath)) {  # 避免重复复制
        try {
            Copy-Item -Path $font.FullName -Destination $targetPath -Force
            $fileSizeMB = [math]::Round($font.Length / 1MB, 2)
            $totalSize += $font.Length
            Write-Host "✓ 发现并复制: $($font.Name) ($fileSizeMB MB)" -ForegroundColor Cyan
            $copiedCount++
        }
        catch {
            Write-Host "✗ 复制失败: $($font.Name)" -ForegroundColor Red
        }
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "字体复制完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

if ($copiedCount -gt 0) {
    $totalSizeMB = [math]::Round($totalSize / 1MB, 2)
    
    Write-Host "成功复制: $copiedCount 个字体文件" -ForegroundColor Green
    Write-Host "总大小: $totalSizeMB MB" -ForegroundColor Green
    
    Write-Host "`n字体文件列表:" -ForegroundColor Yellow
    Get-ChildItem -Path $fontsDir -File | ForEach-Object {
        $sizeMB = [math]::Round($_.Length / 1MB, 2)
        Write-Host "  $($_.Name) - $sizeMB MB" -ForegroundColor White
    }
    
    Write-Host "`n包含的字体类型:" -ForegroundColor Yellow
    Write-Host "• 微软雅黑 - 现代无衬线字体" -ForegroundColor White
    Write-Host "• 宋体 - 传统衬线字体" -ForegroundColor White
    Write-Host "• 黑体 - 粗体显示字体" -ForegroundColor White
    Write-Host "• 楷体 - 手写风格字体" -ForegroundColor White
    Write-Host "• 仿宋 - 印刷体字体" -ForegroundColor White
    Write-Host "• 华文系列 - 专业字体" -ForegroundColor White
    
    Write-Host "`n✓ 这些字体足够满足所有中文显示需求！" -ForegroundColor Green
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "Docker使用说明:" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "在 Dockerfile 中添加以下内容:" -ForegroundColor White
    Write-Host "COPY fonts/* /usr/share/fonts/chinese/" -ForegroundColor Cyan
    Write-Host "RUN fc-cache -fv" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}
else {
    Write-Host "⚠ 没有找到任何中文字体文件" -ForegroundColor Yellow
    Write-Host "可能的原因:" -ForegroundColor Yellow
    Write-Host "1. 系统没有安装中文字体" -ForegroundColor White
    Write-Host "2. 权限不足无法访问字体目录" -ForegroundColor White
    Write-Host "3. 字体文件名已更改" -ForegroundColor White
    
    Write-Host "`n建议:" -ForegroundColor Yellow
    Write-Host "1. 检查系统语言设置" -ForegroundColor White
    Write-Host "2. 以管理员身份运行此脚本" -ForegroundColor White
    Write-Host "3. 手动安装中文语言包" -ForegroundColor White
}

Write-Host "`n脚本执行完成！" -ForegroundColor Green 