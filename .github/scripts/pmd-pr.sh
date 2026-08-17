#!/bin/bash
# ============================================================
# pmd-pr.sh - PMD 增量扫描脚本
# 功能：只扫描本次提交中变更且仍存在的 Java 文件
# 报告：target/pmd-report.xml、target/pmd-report.html
# ============================================================

set -euo pipefail

echo "========================================"
echo "  PMD 增量扫描"
echo "  扫描范围：本次变更的 Java 文件"
echo "========================================"

# 1. 确定目标分支
if [ -n "${GITHUB_BASE_REF:-}" ]; then
    BASE_BRANCH="origin/$GITHUB_BASE_REF"
elif [ -n "${GITHUB_REF:-}" ] && [ "${GITHUB_EVENT_NAME:-}" == "push" ]; then
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
if [ "$BASE_BRANCH" == "HEAD^" ]; then
    CHANGED_FILES=$(git diff --name-only --diff-filter=ACMRT "$BASE_BRANCH" HEAD -- '*.java' 2>/dev/null || true)
else
    CHANGED_FILES=$(git diff --name-only --diff-filter=ACMRT "$BASE_BRANCH"...HEAD -- '*.java' 2>/dev/null || true)
fi

if [ -z "$CHANGED_FILES" ]; then
    echo "✅ 没有 Java 文件变更，跳过 PMD 扫描。"
    exit 0
fi

echo "📝 变更的 Java 文件："
echo "$CHANGED_FILES"
echo "----------------------------------------"

# 3. 生成文件列表（绝对路径），跳过已删除文件
PROJECT_ROOT=$(pwd)
mkdir -p target
FILE_LIST="target/pmd-changed-files.txt"
REPORT_FILE="target/pmd-report.xml"
HTML_REPORT_FILE="target/pmd-report.html"
> "$FILE_LIST"
rm -f "$REPORT_FILE" "$HTML_REPORT_FILE"

scan_count=0
for file in $CHANGED_FILES; do
    if [ ! -f "$file" ]; then
        echo "⚠️ 跳过不存在的文件: $file"
        continue
    fi

    echo "$PROJECT_ROOT/$file" >> "$FILE_LIST"
    scan_count=$((scan_count + 1))
done

if [ "$scan_count" -eq 0 ]; then
    echo "✅ 没有需要 PMD 扫描的现存 Java 文件。"
    rm -f "$FILE_LIST"
    exit 0
fi

echo "📄 PMD 文件列表：$FILE_LIST"
cat "$FILE_LIST"
echo "----------------------------------------"

# 4. 准备 PMD（如果未安装）
PMD_VERSION="${PMD_VERSION:-6.55.0}"
PMD_HOME="${PMD_HOME:-target/pmd-bin-$PMD_VERSION}"
PMD_ZIP="target/pmd-bin-$PMD_VERSION.zip"
PMD_RULESETS="${PMD_RULESETS:-category/java/bestpractices.xml,category/java/codestyle.xml,category/java/design.xml,category/java/errorprone.xml,category/java/performance.xml,category/java/security.xml}"

if [ ! -x "$PMD_HOME/bin/run.sh" ]; then
    echo "⬇️ 下载 PMD $PMD_VERSION ..."
    rm -rf "$PMD_HOME" "$PMD_ZIP" "target/pmd-bin-$PMD_VERSION"
    curl -fsSL "https://github.com/pmd/pmd/releases/download/pmd_releases%2F${PMD_VERSION}/pmd-bin-${PMD_VERSION}.zip" -o "$PMD_ZIP"
    unzip -q "$PMD_ZIP" -d target
    rm -f "$PMD_ZIP"
fi

PMD_CMD="$PMD_HOME/bin/run.sh"
chmod +x "$PMD_CMD"

# 5. 执行 PMD 扫描（使用 filelist）
echo "🚀 执行 PMD 扫描..."
set +e
"$PMD_CMD" pmd --no-cache \
    -filelist "$FILE_LIST" \
    -f xml \
    -R "$PMD_RULESETS" \
    -r "$REPORT_FILE"
pmd_exit=$?
set -e

# 6. 统计违规数
if [ -f "$REPORT_FILE" ]; then
    violations=$(grep -c -- '<violation ' "$REPORT_FILE" 2>/dev/null || true)
    echo "✅ PMD 报告已生成：$REPORT_FILE"
else
    violations=0
    echo "⚠️ PMD 未生成报告。"
fi

# 7. 生成 PMD HTML 可视化报告
echo "🖼️ 生成 PMD HTML 报告..."
set +e
"$PMD_CMD" pmd --no-cache \
    -filelist "$FILE_LIST" \
    -f html \
    -R "$PMD_RULESETS" \
    -r "$HTML_REPORT_FILE"
pmd_html_exit=$?
set -e

if [ -f "$HTML_REPORT_FILE" ]; then
    echo "✅ PMD HTML 报告已生成：$HTML_REPORT_FILE"
else
    echo "⚠️ PMD HTML 报告未生成。"
fi

rm -f "$FILE_LIST"

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    {
        echo "## PMD 增量扫描"
        echo ""
        echo "| 指标 | 结果 |"
        echo "|------|------|"
        echo "| 扫描文件数 | $scan_count |"
        echo "| 违规数 | $violations |"
        echo "| XML 报告 | $REPORT_FILE |"
        echo "| HTML 报告 | $HTML_REPORT_FILE |"
    } >> "$GITHUB_STEP_SUMMARY"
fi

if [ "$violations" -gt 0 ]; then
    echo "❌ PMD 发现 $violations 个问题，构建失败。"
    exit 1
fi

if [ "$pmd_exit" -ne 0 ]; then
    echo "❌ PMD 执行失败，退出码: $pmd_exit"
    exit "$pmd_exit"
fi

if [ "$pmd_html_exit" -ne 0 ]; then
    echo "❌ PMD HTML 报告生成失败，退出码: $pmd_html_exit"
    exit "$pmd_html_exit"
fi

echo "✅ PMD 未发现问题。"
exit 0
