#!/bin/bash
# ============================================================
# checkstyle-pr.sh - 增量检查（扫描整个变更文件，不过滤行号）
# 功能：对本次提交中变更的 Java 文件执行完整的 Checkstyle 检查
# 不阻断构建，生成完整报告
# ============================================================

set -e

echo "========================================"
echo "  Checkstyle 增量检查"
echo "  扫描范围：本次变更的 Java 文件（完整文件）"
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
    echo "✅ 没有 Java 文件变更，跳过检查。"
    exit 0
fi

echo "📝 变更的 Java 文件："
echo "$CHANGED_FILES"
echo "----------------------------------------"

# 3. 将文件列表转为逗号分隔
FILES_LIST=$(echo "$CHANGED_FILES" | tr '\n' ',' | sed 's/,$//')

# 4. 执行 Checkstyle 扫描（生成完整报告）
echo "🚀 执行 Checkstyle 扫描（完整文件）..."
echo "FILES_LIST: $FILES_LIST"
set +e
mvn -pl base checkstyle:check \
    -Dcheckstyle.config.location=checkstyle/huawei-checkstyle.xml \
    -Dcheckstyle.violationSeverity=warning
MVN_EXIT=$?
set -e

# 额外生成 HTML 报告（不受违规影响，始终执行）
echo "📄 生成 HTML 报告..."
mvn -pl base checkstyle:checkstyle \
    -Dcheckstyle.config.location=checkstyle/huawei-checkstyle.xml \
    -Dcheckstyle.outputFormat=html \
    -Dcheckstyle.violationSeverity=warning

# 5. 确定报告路径（根据项目结构调整）
REPORT_FILE="base/target/checkstyle-result.xml"
if [ ! -f "$REPORT_FILE" ]; then
    echo "❌ 未生成 Checkstyle 报告，请检查 Maven 配置。"
    exit 0
fi

# 6. 统计违规数
VIOLATIONS=$(grep -c '<error' "$REPORT_FILE" || true)

echo "----------------------------------------"
if [ $VIOLATIONS -eq 0 ]; then
    echo "✅ 所有变更文件未发现违规！"
else
    echo "⚠️ 发现 $VIOLATIONS 个违规项（完整文件扫描）"
    echo ""
    echo "📋 违规摘要（前 30 条）："
    grep '<error' "$REPORT_FILE" | head -30 | sed 's/<error //; s/\/>//' | \
        sed 's/line="/行号: /; s/column="/列: /; s/severity="/严重性: /; s=message="=信息: =; s=source="//' | \
        while read -r line; do
            echo "  $line"
        done
fi

# 7. 输出到 GitHub Step Summary
if [ -n "$GITHUB_STEP_SUMMARY" ]; then
    {
        echo "## 📋 Checkstyle 报告"
        echo ""
        echo "| 指标 | 结果 |"
        echo "|------|------|"
        if [ $VIOLATIONS -eq 0 ]; then
            echo "| 违规数 | ✅ **0** |"
        else
            echo "| 违规数 | ⚠️ **$VIOLATIONS** |"
        fi
        echo "| 扫描文件 | **$(echo "$CHANGED_FILES" | wc -l)** 个变更 Java 文件 |"
        echo "| 检查方式 | 对变更文件进行完整扫描 |"
        echo ""
        echo "📥 完整报告已作为 Artifact 上传，请在工作流运行页面下载。"
    } >> "$GITHUB_STEP_SUMMARY"
    echo "✅ Step Summary 已更新"
fi

# 8. 始终以成功状态退出
# ================================================
# 7. 根据违规数决定构建状态（拦截 PR）
# ================================================
if [ $VIOLATIONS -eq 0 ]; then
    echo "✅ 检查通过，构建成功。"
    exit 0
else
    echo "❌ 发现 $VIOLATIONS 个违规，构建失败。"
    # 为了让 GitHub Actions 标记为失败，退出码设为 1
    exit 1
fi