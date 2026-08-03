#!/bin/bash
# ============================================================
# checkstyle-pr.sh - 增量检查（支持多模块，汇总统计）
# 功能：对本次提交中变更的 Java 文件，按模块分组执行 Checkstyle 检查
# 不阻断构建，生成完整报告，汇总所有模块的违规数
# ============================================================

set -e

echo "========================================"
echo "  Checkstyle 增量检查（多模块）"
echo "  扫描范围：本次变更的 Java 文件（按模块分组）"
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

# 3. 按模块分组，提取模块内相对路径
declare -A module_files
for file in $CHANGED_FILES; do
    # 提取第一级目录作为模块名（例如 base, app, core 等）
    module="${file%%/*}"
    # 如果模块名为空（如文件在根目录），跳过或处理
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
    echo "⚠️ 没有识别到任何模块的 Java 文件变更，跳过检查。"
    exit 0
fi

echo "📝 按模块分组后的相对路径："
for module in "${!module_files[@]}"; do
    echo "  $module: ${module_files[$module]}"
done
echo "----------------------------------------"

# 4. 对每个模块执行 Checkstyle 检查
for module in "${!module_files[@]}"; do
    file_list="${module_files[$module]}"
    echo "🚀 扫描模块: $module"
    echo "   文件列表: $file_list"

    # 检查模块目录是否存在且包含 pom.xml
    if [ ! -d "$module" ] || [ ! -f "$module/pom.xml" ]; then
        echo "⚠️ 模块目录 $module 不存在或没有 pom.xml，跳过。"
        continue
    fi

    # 执行检查（XML 报告）
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

    # 生成 HTML 报告（始终执行）
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
    echo ""
done

# 5. 汇总所有模块的违规数
total_violations=0
module_violations=""

# 遍历所有变更的模块，统计违规数
for module in "${!module_files[@]}"; do
    report_file="$module/target/checkstyle-result.xml"
    if [ -f "$report_file" ]; then
        count=$(grep -c '<error' "$report_file" || true)
        total_violations=$((total_violations + count))
        if [ $count -gt 0 ]; then
            module_violations="${module_violations}\n  - $module: $count 个违规"
        fi
    else
        echo "⚠️ 模块 $module 未生成报告，可能没有变更或配置问题。"
    fi
done

echo "----------------------------------------"
if [ $total_violations -eq 0 ]; then
    echo "✅ 所有变更文件未发现违规！"
else
    echo "⚠️ 总计发现 $total_violations 个违规项，分布如下："
    echo -e "$module_violations"
    echo ""
    echo "📋 违规摘要（前 30 条，仅显示第一个有违规的模块）："
    # 取第一个有违规的模块显示摘要，避免输出过长
    for module in "${!module_files[@]}"; do
        report_file="$module/target/checkstyle-result.xml"
        if [ -f "$report_file" ] && [ $(grep -c '<error' "$report_file" || true) -gt 0 ]; then
            echo "  来自模块 $module 的违规（前30条）："
            grep '<error' "$report_file" | head -30 | sed 's/<error //; s/\/>//' | \
                sed 's/line="/行号: /; s/column="/列: /; s/severity="/严重性: /; s=message="=信息: =; s=source="//' | \
                while read -r line; do
                    echo "    $line"
                done
            break  # 只显示第一个有违规的模块
        fi
    done
fi

# 6. 输出到 GitHub Step Summary（汇总所有模块）
if [ -n "$GITHUB_STEP_SUMMARY" ]; then
    {
        echo "## 📋 Checkstyle 报告（汇总）"
        echo ""
        echo "| 指标 | 结果 |"
        echo "|------|------|"
        if [ $total_violations -eq 0 ]; then
            echo "| 总违规数 | ✅ **0** |"
        else
            echo "| 总违规数 | ⚠️ **$total_violations** |"
        fi
        echo "| 涉及模块 | **${!module_files[*]}** |"
        echo ""
        if [ $total_violations -gt 0 ]; then
            echo "### 模块违规分布"
            echo "$module_violations" | sed 's/^/  /'
        fi
        echo ""
        echo "📥 完整报告已作为 Artifact 上传，请在工作流运行页面下载。"
    } >> "$GITHUB_STEP_SUMMARY"
    echo "✅ Step Summary 已更新"
fi

# 7. 始终以成功状态退出
exit 0