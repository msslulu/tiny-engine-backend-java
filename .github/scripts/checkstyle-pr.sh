#!/bin/bash
# ============================================================
# checkstyle-pr.sh - 真正的增量检查
# 功能：只报告本次提交中新增或修改行的 Checkstyle 违规
# 不阻断构建，生成过滤后的报告
# ============================================================

set -e

echo "========================================"
echo "  Checkstyle 增量行检查 (只检测新代码)"
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

# 3. 生成包含变更行号信息的文件 (用于后续过滤)
echo "🔄 提取变更行号..."
CHANGE_INFO_FILE=".checkstyle-changes.txt"
rm -f "$CHANGE_INFO_FILE"

for file in $CHANGED_FILES; do
    # 获取该文件新增或修改的行号范围（新文件行号）
    # git diff -U0 输出 unified=0，只显示变更行
    diff_output=$(git diff -U0 "$BASE_BRANCH" HEAD -- "$file" 2>/dev/null | grep '^@@' || true)
    if [ -n "$diff_output" ]; then
        # 解析 @@ -old,count +new,count @@ 提取新行号范围
        echo "$diff_output" | while read -r line; do
            # 提取 + 后面的起始行和行数
            new_info=$(echo "$line" | sed -n 's/.*+\([0-9]*\)\(,[0-9]*\)\?.*/\1 \2/p')
            if [ -n "$new_info" ]; then
                start=$(echo "$new_info" | awk '{print $1}')
                count=$(echo "$new_info" | awk '{print $2}' | sed 's/,//')
                if [ -z "$count" ]; then
                    count=1
                fi
                # 生成行号范围，例如 file:10-15
                echo "$file:$start-$((start+count-1))" >> "$CHANGE_INFO_FILE"
            fi
        done
    fi
done

if [ ! -s "$CHANGE_INFO_FILE" ]; then
    echo "⚠️ 未提取到变更行号，可能变更只涉及删除或文件重命名，跳过检查。"
    exit 0
fi

echo "变更行号信息已保存到 $CHANGE_INFO_FILE"

# 4. 执行 Checkstyle 扫描全部变更文件（生成完整报告）
echo "🚀 执行 Checkstyle 扫描（生成完整报告）..."
FILES_LIST=$(echo "$CHANGED_FILES" | tr '\n' ',' | sed 's/,$//')

set +e
mvn checkstyle:check \
    -Dcheckstyle.includes="$FILES_LIST" \
    -Dcheckstyle.excludes="**/test/**/*.java" \
    -Dcheckstyle.violationSeverity=warning
MVN_EXIT=$?
set -e

REPORT_FILE="base/target/checkstyle-result.xml"
if [ ! -f "$REPORT_FILE" ]; then
    echo "❌ 未生成 Checkstyle 报告，请检查 Maven 配置。"
    exit 0
fi

# 5. 过滤报告：只保留变更行上的违规
echo "🔄 过滤违规，只保留变更行上的问题..."
FILTERED_REPORT="target/checkstyle-filtered-result.xml"
FILTERED_VIOLATIONS=0

# 使用 Python 脚本过滤（GitHub Actions 环境自带 python3）
python3 << EOF
import sys
import xml.etree.ElementTree as ET

# 读取变更行信息
changed_lines = {}
with open("$CHANGE_INFO_FILE", "r") as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        parts = line.split(':')
        if len(parts) != 2:
            continue
        file = parts[0]
        range_str = parts[1]
        start_end = range_str.split('-')
        if len(start_end) == 2:
            start = int(start_end[0])
            end = int(start_end[1])
            for i in range(start, end+1):
                changed_lines.setdefault(file, set()).add(i)
        else:
            # 单行
            changed_lines.setdefault(file, set()).add(int(start_end[0]))

# 解析 Checkstyle 报告
tree = ET.parse("$REPORT_FILE")
root = tree.getroot()

# 创建过滤后的根
new_root = ET.Element("checkstyle", attrib=root.attrib)

for file_elem in root.findall("file"):
    file_name = file_elem.get("name")
    # 获取相对于项目根目录的路径（可能带有绝对路径，需要规范化）
    # 简单处理：取文件名部分，但为了匹配，我们只比较文件名（如果路径不同）
    # 更健壮：使用文件名的 basename 或匹配路径结尾
    # 这里我们直接匹配字符串，因为 diff 输出的是相对路径，而 Checkstyle 输出绝对路径
    # 我们尝试使用相对路径匹配
    changed_set = changed_lines.get(file_name, set())
    if not changed_set:
        # 尝试取 basename 匹配
        base = file_name.split('/')[-1]
        for key in changed_lines:
            if key.endswith(base):
                changed_set = changed_lines[key]
                break

    new_file = ET.SubElement(new_root, "file", attrib=file_elem.attrib)
    errors_found = False
    for error in file_elem.findall("error"):
        line_num = int(error.get("line", 0))
        if line_num in changed_set:
            new_file.append(error)
            errors_found = True
    if not errors_found:
        # 如果该文件没有违规，则移除（但保留空文件无意义，可以跳过）
        new_root.remove(new_file)

# 写回过滤后的报告
tree = ET.ElementTree(new_root)
tree.write("$FILTERED_REPORT", encoding="utf-8", xml_declaration=True)
EOF

# 6. 统计过滤后的违规数
if [ -f "$FILTERED_REPORT" ]; then
    FILTERED_VIOLATIONS=$(grep -c '<error' "$FILTERED_REPORT" || true)
else
    FILTERED_VIOLATIONS=0
fi

echo "----------------------------------------"
if [ $FILTERED_VIOLATIONS -eq 0 ]; then
    echo "✅ 新增代码未发现违规！"
else
    echo "⚠️ 新增代码中发现 $FILTERED_VIOLATIONS 个违规项（仅统计新提交的行）"
    echo ""
    echo "📋 违规摘要（前 30 条）："
    grep '<error' "$FILTERED_REPORT" | head -30 | sed 's/<error //; s/\/>//' | \
        sed 's/line="/行号: /; s/column="/列: /; s/severity="/严重性: /; s=message="=信息: =; s=source="//' | \
        while read -r line; do
            echo "  $line"
        done
fi

# 7. 输出到 GitHub Step Summary
if [ -n "$GITHUB_STEP_SUMMARY" ]; then
    {
        echo "## 📋 Checkstyle 增量行报告"
        echo ""
        echo "| 指标 | 结果 |"
        echo "|------|------|"
        if [ $FILTERED_VIOLATIONS -eq 0 ]; then
            echo "| 新增代码违规数 | ✅ **0** |"
        else
            echo "| 新增代码违规数 | ⚠️ **$FILTERED_VIOLATIONS** |"
        fi
        echo "| 扫描文件 | **$(echo "$CHANGED_FILES" | wc -l)** 个变更 Java 文件 |"
        echo "| 检查范围 | 仅新增或修改的行 |"
        echo ""
        echo "📄 [下载完整报告（所有文件）](${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }})"
        echo "📄 [下载过滤后报告（仅新增行）](${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }})"
    } >> "$GITHUB_STEP_SUMMARY"
    echo "✅ Step Summary 已更新"
fi

# 8. 替换原始报告为过滤后的报告（以便 Artifact 上传只包含过滤结果）
# 但为了保留完整报告，我们不上传过滤后的，而是上传原始和过滤两份。
# 在 YAML 中，我们会上传整个 target/ 下的相关文件，所以无所谓。

# 9. 清理临时文件
rm -f "$CHANGE_INFO_FILE"

# 10. 始终以成功状态退出
exit 0