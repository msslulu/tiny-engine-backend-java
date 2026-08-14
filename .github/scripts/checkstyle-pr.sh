#!/bin/bash
# ============================================================
# checkstyle-pr.sh - 增量检查（扫描整个变更文件，不过滤行号）
# 功能：对本次提交中变更的 Java 文件执行完整的 Checkstyle 检查
# 不阻断构建，生成完整报告
# ============================================================

set -e
unset GREP_OPTIONS
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

# 按模块分组
declare -A module_files
for file in $CHANGED_FILES; do
    module="${file%%/*}"
    if [ -z "$module" ] || [ "$module" == "$file" ]; then
        echo "⚠️ 忽略根目录文件: $file"
        continue
    fi
    rel="${file#$module/}"
    if [ -z "${module_files[$module]}" ]; then
        module_files[$module]="$rel"
    else
        module_files[$module]="${module_files[$module]},$rel"
    fi
done

if [ ${#module_files[@]} -eq 0 ]; then
    echo "⚠️ 没有识别到任何模块，跳过检查。"
    exit 0
fi

echo "📝 按模块分组后的相对路径："
for module in "${!module_files[@]}"; do
    echo "  $module: ${module_files[$module]}"
done
echo "----------------------------------------"

total_violations=0

# 对每个模块执行 Checkstyle
for module in "${!module_files[@]}"; do
    file_list="${module_files[$module]}"
    echo "🚀 扫描模块: $module"
    echo "   文件列表: $file_list"

    if [ ! -d "$module" ] || [ ! -f "$module/pom.xml" ]; then
        echo "⚠️ 模块目录 $module 不存在或没有 pom.xml，跳过。"
        continue
    fi

    echo "   - 运行 Checkstyle 检查（增量扫描）..."
    echo "$file_list"
    set +e
    PROJECT_ROOT=$(pwd)
    output=$(cd "$module" && \
        echo "   Current directory: $(pwd)" && \
        echo "   Checking file existence:" && \
        ls -l "${module_files[$module]}" 2>/dev/null || echo "   ⚠️  not found" && \
        mvn checkstyle:check -X \
            -Dcheckstyle.config.location="$PROJECT_ROOT/checkstyle/huawei-checkstyle.xml" \
            -Dcheckstyle.violationSeverity=warning \
            -Dcheckstyle.outputFormat=xml \
            -Dcheckstyle.includes="$file_list" 2>&1 )
    mvn_exit=$?
    if [ $mvn_exit -ne 0 ]; then
        echo "   ⚠️ 模块 $module 的 Checkstyle 检查失败（但继续）"
    fi
    set -e

    # 从输出中提取违规数
    count=$(echo "$output" | grep -oE 'You have [0-9]+ Checkstyle violations' | grep -oE '[0-9]+' | tail -1)
    if [ -z "$count" ]; then
        count=0
    fi
    total_violations=$((total_violations + count))
    echo "   模块 $module 违规数: $count"

    # （可选）生成 HTML 报告供人工查看
    echo "   - 生成 HTML 报告（可选）..."
    set +e
    (cd "$module" && mvn checkstyle:checkstyle \
        -Dcheckstyle.config.location="$PROJECT_ROOT/checkstyle/huawei-checkstyle.xml" \
        -Dcheckstyle.includes="$file_list" \
        -Dcheckstyle.violationSeverity=warning) > /dev/null 2>&1
    set -e
    echo ""
done

# 汇总输出
echo "----------------------------------------"
if [ $total_violations -eq 0 ]; then
    echo "✅ 所有变更文件未发现违规！"
else
    echo "⚠️ 总计发现 $total_violations 个违规。"
    echo ""
    echo "📋 违规摘要（前 30 条）："
    for module in "${!module_files[@]}"; do
        report_file="$module/target/checkstyle-result.xml"
        if [ -f "$report_file" ] && grep -q -- '<error' "$report_file" 2>/dev/null; then
            grep -- '<error' "$report_file" 2>/dev/null | head -30 | sed 's/<error //; s/\/>//' | \
                sed 's|line="|行号: |g; s|column="|列: |g; s|severity="|严重性: |g; s|message="|信息: |g; s|source="||g' | \
                while read -r line; do
                    echo "  $line"
                done || true
            break
        fi
    done
fi

# Step Summary
if [ -n "$GITHUB_STEP_SUMMARY" ]; then
    {
        echo "## 📋 Checkstyle 汇总报告"
        echo ""
        echo "| 指标 | 结果 |"
        echo "|------|------|"
        if [ $total_violations -eq 0 ]; then
            echo "| 总违规数 | ✅ **0** |"
        else
            echo "| 总违规数 | ⚠️ **$total_violations** |"
        fi
        echo "| 涉及模块 | ${!module_files[*]} |"
        echo ""
        echo "📥 完整报告已作为 Artifact 上传。"
    } >> "$GITHUB_STEP_SUMMARY"
fi

# 根据违规数决定退出码
if [ $total_violations -eq 0 ]; then
    echo "✅ 检查通过，构建成功。"
    exit 0
else
    echo "❌ 发现 $total_violations 个违规，构建失败。"
    exit 1
fi