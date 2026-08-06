#!/bin/bash
# ============================================================
# pmd-pr.sh - PMD 增量扫描脚本
# 功能：只扫描本次提交中变更的 Java 文件
# 生成报告到 target/pmd-report.xml（或自定义路径）
# ============================================================

set -e

echo "========================================"
echo "  PMD 增量扫描"
echo "  扫描范围：本次变更的 Java 文件"
echo "========================================"

# 1. 确定目标分支
if [ -n "$GITHUB_BASE_REF" ]; then
    BASE_BRANCH="origin/$GITHUB_BASE_REF"
elif [ -n "$GITHUB_REF" ] && [ "$GITHUB_EVENT_NAME" == "push" ]; then
    BASE_BRANCH="HEAD^"
else
    if git rev-parse --verify origin/main >/dev/null 2>&1; then
        BASE_BRANCH="origin/main"
    elif git rev-parse --verify origin/develop >/dev/null 2>&1; then
        BASE_BRANCH="origin/develop"
    else
        echo "❌ 无法确定目标分支，请设置 BASE_BRANCH 环境变量。"
        exit 1
    fi
    echo "🔍 本地运行模式，对比分支: $BASE_BRANCH"
fi

# 2. 获取变更的 Java 文件
CHANGED_FILES=$(git diff --name-only "$BASE_BRANCH" HEAD 2>/dev/null | grep '\.java$' || true)

if [ -z "$CHANGED_FILES" ]; then
    echo "✅ 没有 Java 文件变更，跳过 PMD 扫描。"
    exit 0
fi

echo "📝 变更的 Java 文件："
echo "$CHANGED_FILES"
echo "----------------------------------------"

# 3. 生成文件列表（绝对路径）
FILE_LIST="changed-files.txt"
> "$FILE_LIST"
for file in $CHANGED_FILES; do
    echo "$PWD/$file" >> "$FILE_LIST"
done

echo "📄 文件列表已生成：$FILE_LIST"
echo "----------------------------------------"

# 4. 准备 PMD（如果未安装）
PMD_VERSION="6.55.0"
PMD_HOME="./pmd"
if [ ! -d "$PMD_HOME" ]; then
    echo "⬇️ 下载 PMD $PMD_VERSION ..."
    curl -L "https://github.com/pmd/pmd/releases/download/pmd_releases%2F${PMD_VERSION}/pmd-bin-${PMD_VERSION}.zip" -o pmd.zip
    unzip -q pmd.zip
    mv pmd-bin-${PMD_VERSION} "$PMD_HOME"
    rm pmd.zip
fi
PMD_CMD="$PMD_HOME/bin/run.sh"
chmod +x "$PMD_CMD"

# 5. 执行 PMD 扫描（使用 -filelist）
echo "🚀 执行 PMD 扫描..."
REPORT_FILE="target/pmd-report.xml"
set +e
"$PMD_CMD" pmd --no-cache \
    -filelist "$FILE_LIST" \
    -f xml \
    -R category/java/quickstart.xml \
    -r "$REPORT_FILE"
EXIT_CODE=$?
set -e

# 6. 检查是否生成报告
if [ -f "$REPORT_FILE" ]; then
    echo "✅ PMD 报告已生成：$REPORT_FILE"
else
    echo "⚠️ PMD 未生成报告，可能无违规或出错。"
fi

# 7. 清理临时文件
rm -f "$FILE_LIST"

# 8. 始终以成功状态退出（违规由 YAML 汇总步骤决定）
exit 0