#!/bin/bash
# ============================================================
# spotbugs-incremental.sh - 增量 SpotBugs 扫描
# 功能：只分析本次变更的 src/main/java 或 src/test/java 文件对应的类
# 报告：各模块 target/spotbugsXml.xml、target/spotbugs-reports/spotbugs*.html
# ============================================================

set -euo pipefail

echo "========================================"
echo "  SpotBugs 增量扫描"
echo "  扫描范围：本次变更的主源码和测试源码 Java 类"
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
    CHANGED_JAVA=$(git diff --name-only --diff-filter=ACMRT "$BASE_BRANCH" HEAD -- '*.java' 2>/dev/null || true)
else
    CHANGED_JAVA=$(git diff --name-only --diff-filter=ACMRT "$BASE_BRANCH"...HEAD -- '*.java' 2>/dev/null || true)
fi

if [ -z "$CHANGED_JAVA" ]; then
    echo "✅ 没有 Java 文件变更，跳过 SpotBugs 扫描。"
    exit 0
fi

echo "📝 变更的 Java 文件："
echo "$CHANGED_JAVA"
echo "----------------------------------------"

# 3. 按 Maven 模块提取待分析类
declare -A module_classes
declare -A module_include_tests
declare -A module_scopes
scan_count=0

for file in $CHANGED_JAVA; do
    if [ ! -f "$file" ]; then
        echo "⚠️ 跳过不存在的文件: $file"
        continue
    fi

    module="${file%%/*}"
    rel="${file#$module/}"

    if [ "$module" == "$file" ] || [ ! -f "$module/pom.xml" ]; then
        echo "⚠️ 跳过非模块 Java 文件: $file"
        continue
    fi

    source_scope=""
    if [[ "$rel" == src/main/java/* ]]; then
        source_scope="main"
    elif [[ "$rel" == src/test/java/* ]]; then
        source_scope="test"
        module_include_tests[$module]="true"
    else
        echo "ℹ️ SpotBugs 分析编译后的 main/test class，跳过非源码目录文件: $file"
        continue
    fi

    pkg=$(sed -nE 's/^[[:space:]]*package[[:space:]]+([^;]+);.*/\1/p' "$file" | head -1)
    if [ -z "$pkg" ]; then
        echo "⚠️ 跳过未声明 package 的文件: $file"
        continue
    fi

    classname=$(basename "$file" .java)
    fqcn="$pkg.$classname"

    if [ -z "${module_classes[$module]:-}" ]; then
        module_classes[$module]="$fqcn"
    else
        module_classes[$module]="${module_classes[$module]},$fqcn"
    fi

    if [ -z "${module_scopes[$module]:-}" ]; then
        module_scopes[$module]="$source_scope"
    elif [[ ",${module_scopes[$module]}," != *",$source_scope,"* ]]; then
        module_scopes[$module]="${module_scopes[$module]},$source_scope"
    fi

    scan_count=$((scan_count + 1))
done

if [ ${#module_classes[@]} -eq 0 ]; then
    echo "✅ 没有需要 SpotBugs 分析的主源码或测试源码类。"
    exit 0
fi

echo "📋 按模块分组后的类："
for module in "${!module_classes[@]}"; do
    include_tests="${module_include_tests[$module]:-false}"
    echo "  $module (${module_scopes[$module]}, includeTests=$include_tests): ${module_classes[$module]}"
done
echo "----------------------------------------"

total_bugs=0
execution_failures=0
total_html_reports=0
html_failures=0

# 4. 对每个模块执行 SpotBugs
for module in "${!module_classes[@]}"; do
    class_list="${module_classes[$module]}"
    include_tests="${module_include_tests[$module]:-false}"
    echo "🚀 扫描模块: $module"
    echo "   类列表: $class_list"
    echo "   扫描测试类: $include_tests"

    rm -f "$module/target/spotbugsXml.xml"
    rm -rf "$module/target/spotbugs-reports"

    set +e
    if [ "$include_tests" == "true" ]; then
        output=$(cd "$module" && mvn test-compile spotbugs:check \
            -DskipTests \
            -Dcheckstyle.skip=true \
            -Dpmd.skip=true \
            -Dcpd.skip=true \
            -Dspotbugs.onlyAnalyze="$class_list" \
            -Dspotbugs.includeTests=true \
            -Dspotbugs.xmlOutput=true \
            -Dspotbugs.htmlOutput=true 2>&1)
    else
        output=$(cd "$module" && mvn spotbugs:check \
            -Dspotbugs.onlyAnalyze="$class_list" \
            -Dspotbugs.includeTests=false \
            -Dspotbugs.xmlOutput=true \
            -Dspotbugs.htmlOutput=true 2>&1)
    fi
    mvn_exit=$?
    set -e
    echo "$output"

    html_count=0
    while IFS= read -r html_file; do
        html_count=$((html_count + 1))
        echo "   HTML 报告: $html_file"
    done < <(find "$module/target" -type f \( -name 'spotbugs.html' -o -name 'spotbugs*.html' \) 2>/dev/null)

    if [ "$html_count" -eq 0 ]; then
        echo "   - 未找到 SpotBugs HTML 报告，单独生成可视化报告..."
        set +e
        if [ "$include_tests" == "true" ]; then
            report_output=$(cd "$module" && mvn test-compile spotbugs:spotbugs \
                -DskipTests \
                -Dcheckstyle.skip=true \
                -Dpmd.skip=true \
                -Dcpd.skip=true \
                -Dspotbugs.onlyAnalyze="$class_list" \
                -Dspotbugs.includeTests=true \
                -Dspotbugs.xmlOutput=true \
                -Dspotbugs.htmlOutput=true 2>&1)
        else
            report_output=$(cd "$module" && mvn spotbugs:spotbugs \
                -Dspotbugs.onlyAnalyze="$class_list" \
                -Dspotbugs.includeTests=false \
                -Dspotbugs.xmlOutput=true \
                -Dspotbugs.htmlOutput=true 2>&1)
        fi
        report_exit=$?
        set -e
        echo "$report_output"

        html_count=0
        while IFS= read -r html_file; do
            html_count=$((html_count + 1))
            echo "   HTML 报告: $html_file"
        done < <(find "$module/target" -type f \( -name 'spotbugs.html' -o -name 'spotbugs*.html' \) 2>/dev/null)

        if [ "$report_exit" -ne 0 ] && [ "$html_count" -eq 0 ]; then
            echo "   ❌ 模块 $module 的 SpotBugs HTML 报告生成失败，退出码: $report_exit"
        fi
    fi

    if [ "$html_count" -eq 0 ]; then
        html_failures=$((html_failures + 1))
    fi
    total_html_reports=$((total_html_reports + html_count))

    bug_count=0
    while IFS= read -r report_file; do
        count=$(grep -c -- '<BugInstance ' "$report_file" 2>/dev/null || true)
        bug_count=$((bug_count + count))
        echo "   报告: $report_file，问题数: $count"
    done < <(find "$module/target" -type f \( -name 'spotbugsXml.xml' -o -name 'spotbugs*.xml' \) 2>/dev/null)

    total_bugs=$((total_bugs + bug_count))
    echo "   模块 $module SpotBugs 问题数: $bug_count"

    if [ "$mvn_exit" -ne 0 ] && [ "$bug_count" -eq 0 ]; then
        echo "   ❌ 模块 $module 的 SpotBugs 执行失败，且未生成可解析的问题报告。"
        execution_failures=$((execution_failures + 1))
    fi
    echo ""
done

# 5. 汇总
echo "----------------------------------------"
echo "SpotBugs 扫描类数: $scan_count"
echo "SpotBugs 问题总数: $total_bugs"
echo "SpotBugs 执行失败模块数: $execution_failures"
echo "SpotBugs HTML 报告数: $total_html_reports"
echo "SpotBugs HTML 报告失败模块数: $html_failures"

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    {
        echo "## SpotBugs 增量扫描"
        echo ""
        echo "| 指标 | 结果 |"
        echo "|------|------|"
        echo "| 扫描类数 | $scan_count |"
        echo "| 问题数 | $total_bugs |"
        echo "| 执行失败模块数 | $execution_failures |"
        echo "| HTML 报告数 | $total_html_reports |"
        echo "| HTML 报告失败模块数 | $html_failures |"
    } >> "$GITHUB_STEP_SUMMARY"
fi

if [ "$html_failures" -ne 0 ]; then
    echo "❌ 有 $html_failures 个模块未生成 SpotBugs HTML 报告，构建失败。"
    exit 1
fi

if [ "$execution_failures" -ne 0 ]; then
    echo "❌ 有 $execution_failures 个模块 SpotBugs 执行失败，构建失败。"
    exit 1
fi

if [ "$total_bugs" -ne 0 ]; then
    echo "❌ SpotBugs 发现 $total_bugs 个问题，构建失败。"
    exit 1
fi

echo "✅ SpotBugs 未发现问题。"
exit 0
