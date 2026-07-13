#!/bin/bash
# ============================================================
# checkstyle-pr.sh - 增量 Checkstyle 检查（仅报告，不阻断构建）
# 用途：扫描 PR/commit 中变更的 Java 文件，生成违规报告
# ============================================================

set -e  # 遇到错误立即退出（但我们会特殊处理 mvn 的返回）

echo "========================================"
echo "  Checkstyle PR 增量检查 (仅报告)"
echo "========================================"

# 1. 确定对比的目标分支
if [ -n "$GITHUB_BASE_REF" ]; then
    # GitHub Actions 环境：PR 事件
    BASE_BRANCH="origin/$GITHUB_BASE_REF"
    echo "🔍 检测到 PR 事件，目标分支: $GITHUB_BASE_REF"
elif [ -n "$GITHUB_REF" ] && [ "$GITHUB_EVENT_NAME" == "push" ]; then
    # push 事件，对比上一次提交
    BASE_BRANCH="HEAD^"
    echo "🔍 检测到 Push 事件，对比上一次提交"
else
    # 本地运行：默认对比 origin/main，如果不存在则尝试 origin/develop
    if git rev-parse --verify origin/main >/dev/null 2>&1; then
        BASE_BRANCH="origin/main"
    elif git rev-parse --verify origin/develop >/dev/null 2>&1; then
        BASE_BRANCH="origin/develop"
    else
        echo "❌ 无法确定目标分支，请指定 BASE_BRANCH 环境变量或在 Git 仓库中设置远程分支。"
        exit 1
    fi
    echo "🔍 本地运行模式，对比分支: $BASE_BRANCH"
fi

# 2. 确保目标分支存在（如果是远程分支，先 fetch）
if [[ "$BASE_BRANCH" == origin/* ]] && ! git rev-parse --verify "$BASE_BRANCH" >/dev/null 2>&1; then
    echo "⬇️ 正在 fetch 远程分支 $BASE_BRANCH ..."
    git fetch origin "${BASE_BRANCH#origin/}"
fi

# 3. 获取变更的 Java 文件列表
CHANGED_FILES=$(git diff --name-only "$BASE_BRANCH" HEAD 2>/dev/null | grep '\.java$' || true)

if [ -z "$CHANGED_FILES" ]; then
    echo "✅ 本次没有变更 Java 文件，跳过检查。"
    exit 0
fi

echo "📝 本次变更的 Java 文件："
echo "$CHANGED_FILES"
echo "----------------------------------------"

# 4. 将文件列表转为逗号分隔（用于 Maven 参数）
FILES_LIST=$(echo "$CHANGED_FILES" | tr '\n' ',' | sed 's/,$//')

# 5. 执行 Checkstyle（生成报告，不阻断）
echo "🚀 开始执行 Checkstyle 增量扫描..."
set +e  # 暂时关闭错误退出，以便我们自己处理返回值
mvn checkstyle:checkstyle \
    -Dcheckstyle.includes="$FILES_LIST" \
    -Dcheckstyle.excludes="**/test/**/*.java"
MVN_EXIT=$?
set -e  # 恢复

# 6. 解析违规数
VIOLATIONS=0
REPORT_FILE="target/checkstyle-result.xml"
if [ -f "$REPORT_FILE" ]; then
    VIOLATIONS=$(grep -c '<error' "$REPORT_FILE" || true)
    HTML_REPORT="target/site/checkstyle.html"
fi

echo "----------------------------------------"
if [ $VIOLATIONS -eq 0 ]; then
    echo "✅ 所有变更文件符合编码规范！"
else
    echo "⚠️ 发现 $VIOLATIONS 个违规项（详见下方摘要和 Artifact 报告）"
    echo ""
    echo "📋 违规摘要（前 30 条）："
    grep '<error' "$REPORT_FILE" | head -30 | sed 's/<error //; s/\/>//' | \
        sed 's/line="/行号: /; s/column="/列: /; s/severity="/严重性: /; s=message="=信息: =; s=source="//' | \
        while read -r line; do
            echo "  $line"
        done
fi

# 7. 输出到 GitHub Step Summary（仅在 Actions 环境中有效）
if [ -n "$GITHUB_STEP_SUMMARY" ]; then
    {
        echo "## 📋 Checkstyle 报告摘要"
        echo ""
        echo "| 指标 | 结果 |"
        echo "|------|------|"
        if [ $VIOLATIONS -eq 0 ]; then
            echo "| 违规数 | ✅ **0** |"
        else
            echo "| 违规数 | ⚠️ **$VIOLATIONS** |"
        fi
        echo "| 扫描文件 | **$(echo "$CHANGED_FILES" | wc -l)** 个 Java 文件 |"
        echo ""
        if [ -f "$HTML_REPORT" ]; then
            echo "📄 [查看详细 HTML 报告](${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }})"
        fi
        echo "💡 完整报告作为 Artifact 下载，请查看工作流运行页面。"
    } >> "$GITHUB_STEP_SUMMARY"
    echo "✅ Step Summary 已生成"
fi

# 8. 始终以成功状态退出（不阻断构建）
exit 0