#!/bin/bash
# ============================================================
# checkstyle-pr.sh - Checkstyle 增量扫描（多模块，包含测试）
# 功能：只扫描本次提交中变更的 Java 文件（所有模块，src/main 和 src/test）
# 生成报告，根据违规数决定退出码
# ============================================================

set -e

echo "========================================"
echo "  Checkstyle 增量扫描（多模块）"
echo "  扫描范围：所有模块的变更 Java 文件（包含测试）"
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

# 2. 获取变更的 Java 文件（所有模块）
CHANGED_FILES=$(git diff --name-only "$BASE_BRANCH" HEAD 2>/dev/null | grep '\.java$' || true)

if [ -z "$CHANGED_FILES" ]; then
    echo "✅ 没有 Java 文件变更，跳过检查。"
    exit 0
fi

echo "📝 变更的 Java 文件："
echo "$CHANGED_FILES"
echo "----------------------------------------"

# 3. 按模块分组，提取模块内相对路径
declare -A module_files
for file in $CHANGED_FILES; do
    # 提取第一级目录作为模块名（例如 base, app, core 等）
    module="${file%%/*}"
    # 如果模块名为空或文件在根目录，跳过
    if [ -z "$module" ] || [ "$module" == "$file" ]; then
        echo "⚠️ 忽略根目录文件: $file"
        continue
    fi
    # 去掉模块前缀，得到相对路径
    rel="${file#$module/}"
    # 追加到对应模块的列表中（逗号分隔）
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

# 4. 对每个模块执行 Checkstyle 检查
total_violations=0
for module in "${!module_files[@]}"; do
    file_list="${module_files[$module]}"
    echo "🚀 扫描模块: $module"
    echo "   文件列表: $file_list"

    if [ ! -d "$module" ] || [ ! -f "$module/pom.xml" ]; then
        echo "⚠️ 模块目录 $module 不存在或没有 pom.xml，跳过。"
        continue
    fi

    # 执行 Checkstyle（生成 XML 报告）
    echo "   - 生成 XML 报告..."
    set +e
    (cd "$module" && mvn checkstyle:check \
        -Dcheckstyle.config.location=../checkstyle/huawei-checkstyle.xml \
        -Dcheckstyle.includes="$file_list" \
        -Dcheckstyle.violationSeverity=warning)
    if [ $? -ne 0 ]; then
        echo "   ⚠️ 模块 $module 的 Checkstyle 检查失败（但继续）"
    fi
    set -e

    # 生成 HTML 报告
    echo "   - 生成 HTML 报告..."
    set +e
    (cd "$module" && mvn checkstyle:checkstyle \
        -Dcheckstyle.config.location=../checkstyle/huawei-checkstyle.xml \
        -Dcheckstyle.includes="$file_list" \
        -Dcheckstyle.outputFormat=html \
        -Dcheckstyle.violationSeverity=warning)
    if [ $? -ne 0 ]; then
        echo "   ⚠️ 模块 $module 的 HTML 报告生成失败（但继续）"
    fi
    set -e

    # 统计该模块的违规数
    report_file="$module/target/checkstyle-result.xml"
    if [ -f "$report_file" ]; then
        count=$(grep -c '<error' "$report_file" || true)
        total_violations=$((total_violations + count))
        echo "   模块 $module 违规数: $count"
    else
        echo "   ⚠️ 模块 $module 未生成报告"
    fi
    echo ""
done

# 5. 输出汇总信息
echo "----------------------------------------"
if [ $total_violations -eq 0 ]; then
    echo "✅ 所有变更文件未发现违规！"
else
    echo "⚠️ 总计发现 $total_violations 个违规。"
fi

# 6. 输出到 Step Summary（可选）
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

# 7. 根据违规数决定构建状态
if [ $total_violations -eq 0 ]; then
    echo "✅ 检查通过，构建成功。"
    exit 0
else
    echo "❌ 发现 $total_violations 个违规，构建失败。"
    exit 1
fi