#!/bin/bash
# ============================================================
# spotbugs-incremental.sh - 增量 SpotBugs 扫描
# 功能：只分析本次变更的 src/main/java 或 src/test/java 文件对应的类
# 报告：各模块 target/spotbugsXml.xml、target/spotbugs-reports/spotbugs.html
# ============================================================

set -euo pipefail

PYTHON_BIN="${PYTHON_BIN:-}"
if [ -z "$PYTHON_BIN" ]; then
    if command -v python3 >/dev/null 2>&1; then
        PYTHON_BIN="python3"
    elif command -v python >/dev/null 2>&1; then
        PYTHON_BIN="python"
    fi
fi

generate_styled_spotbugs_html_report() {
    local module_dir="$1"
    local module_name="$2"
    local class_list="$3"
    local output_file="$module_dir/target/spotbugs-reports/spotbugs.html"
    local xml_files=()

    while IFS= read -r xml_file; do
        xml_files+=("$xml_file")
    done < <(find "$module_dir/target" -type f \( -name 'spotbugsXml.xml' -o -name 'spotbugs*.xml' \) 2>/dev/null)

    if [ ${#xml_files[@]} -eq 0 ] || [ -z "$PYTHON_BIN" ]; then
        return 1
    fi

    mkdir -p "$(dirname "$output_file")"
    "$PYTHON_BIN" - "$output_file" "$module_name" "$class_list" "${xml_files[@]}" <<'PY'
import datetime
import html
import sys
import xml.etree.ElementTree as ET

output_file = sys.argv[1]
module_name = sys.argv[2]
class_list = sys.argv[3]
xml_files = sys.argv[4:]
priority_names = {
    "1": "High",
    "2": "Medium",
    "3": "Low",
    "4": "Experimental",
}
bugs = []

for xml_file in xml_files:
    try:
        root = ET.parse(xml_file).getroot()
    except ET.ParseError:
        continue

    for bug in root.findall(".//BugInstance"):
        source = bug.find("SourceLine")
        bug_class = bug.find("Class")
        long_message = bug.findtext("LongMessage") or bug.findtext("ShortMessage") or ""
        location = ""
        if source is not None:
            location = source.get("sourcepath") or source.get("sourcefile") or ""
            line = source.get("start") or source.get("startLine") or ""
            if line and line != "-1":
                location = f"{location}:{line}" if location else line

        bugs.append({
            "priority": priority_names.get(bug.get("priority", ""), bug.get("priority", "")),
            "rank": bug.get("rank", ""),
            "category": bug.get("category", ""),
            "type": bug.get("type", ""),
            "class": bug_class.get("classname", "") if bug_class is not None else "",
            "location": location,
            "message": long_message.strip(),
        })

def esc(value):
    return html.escape(str(value or ""), quote=True)

rows = []
for bug in bugs:
    priority_class = esc(bug["priority"].lower())
    rows.append(f"""
        <tr>
          <td><span class="badge {priority_class}">{esc(bug["priority"])}</span></td>
          <td>{esc(bug["rank"])}</td>
          <td>{esc(bug["category"])}</td>
          <td><code>{esc(bug["type"])}</code></td>
          <td>{esc(bug["class"])}</td>
          <td>{esc(bug["location"])}</td>
          <td>{esc(bug["message"])}</td>
        </tr>
    """)

empty_state = ""
if not bugs:
    empty_state = '<div class="empty">No SpotBugs issues were found in the incremental scan.</div>'

generated_at = datetime.datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S UTC")
html_doc = f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>SpotBugs Incremental Report - {esc(module_name)}</title>
  <style>
    :root {{
      color-scheme: light;
      --bg: #f6f8fa;
      --panel: #ffffff;
      --text: #24292f;
      --muted: #57606a;
      --border: #d0d7de;
      --accent: #0969da;
      --high: #cf222e;
      --medium: #9a6700;
      --low: #1a7f37;
      --experimental: #8250df;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      background: var(--bg);
      color: var(--text);
      font: 14px/1.55 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    }}
    header {{
      background: #24292f;
      color: #ffffff;
      padding: 24px 32px;
    }}
    header h1 {{
      margin: 0 0 8px;
      font-size: 24px;
      font-weight: 650;
      letter-spacing: 0;
    }}
    header p {{
      margin: 0;
      color: #d0d7de;
    }}
    main {{
      max-width: 1240px;
      margin: 24px auto;
      padding: 0 24px 32px;
    }}
    .summary {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 12px;
      margin-bottom: 16px;
    }}
    .metric {{
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 6px;
      padding: 14px 16px;
    }}
    .metric span {{
      display: block;
      color: var(--muted);
      font-size: 12px;
      text-transform: uppercase;
    }}
    .metric strong {{
      display: block;
      margin-top: 4px;
      font-size: 22px;
    }}
    .panel {{
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 6px;
      overflow: hidden;
    }}
    table {{
      width: 100%;
      border-collapse: collapse;
    }}
    th, td {{
      padding: 10px 12px;
      border-bottom: 1px solid var(--border);
      text-align: left;
      vertical-align: top;
    }}
    th {{
      background: #f6f8fa;
      color: var(--muted);
      font-size: 12px;
      font-weight: 650;
      text-transform: uppercase;
    }}
    tr:hover td {{
      background: #f6f8fa;
    }}
    code {{
      font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
      font-size: 12px;
      word-break: break-word;
    }}
    .badge {{
      display: inline-block;
      min-width: 78px;
      border-radius: 999px;
      padding: 2px 9px;
      color: #ffffff;
      font-size: 12px;
      font-weight: 650;
      text-align: center;
    }}
    .high {{ background: var(--high); }}
    .medium {{ background: var(--medium); }}
    .low {{ background: var(--low); }}
    .experimental {{ background: var(--experimental); }}
    .empty {{
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 6px;
      padding: 28px;
      color: var(--muted);
      text-align: center;
    }}
    .muted {{
      color: var(--muted);
      word-break: break-word;
    }}
  </style>
</head>
<body>
  <header>
    <h1>SpotBugs Incremental Report</h1>
    <p>Module: {esc(module_name)} · Generated: {esc(generated_at)}</p>
  </header>
  <main>
    <section class="summary">
      <div class="metric"><span>Issues</span><strong>{len(bugs)}</strong></div>
      <div class="metric"><span>Classes</span><strong>{len([c for c in class_list.split(",") if c])}</strong></div>
      <div class="metric"><span>Sources</span><strong>{len(xml_files)}</strong></div>
    </section>
    <p class="muted">Analyzed classes: {esc(class_list)}</p>
    {empty_state}
    <section class="panel">
      <table>
        <thead>
          <tr>
            <th>Priority</th>
            <th>Rank</th>
            <th>Category</th>
            <th>Type</th>
            <th>Class</th>
            <th>Location</th>
            <th>Message</th>
          </tr>
        </thead>
        <tbody>
          {''.join(rows)}
        </tbody>
      </table>
    </section>
  </main>
</body>
</html>
"""

with open(output_file, "w", encoding="utf-8") as report:
    report.write(html_doc)
PY
}

echo "========================================"
echo "  SpotBugs 增量扫描"
echo "  扫描范围：本次变更的主源码和测试源码 Java 类"
echo "========================================"

# 1. 确定比较基线
if [ -n "${GITHUB_BASE_REF:-}" ]; then
    BASE_BRANCH="origin/$GITHUB_BASE_REF"
elif [ "${GITHUB_EVENT_NAME:-}" == "push" ]; then
    BASE_BRANCH="${EVENT_BEFORE:-}"
    HEAD_COMMIT="${EVENT_SHA:-${GITHUB_SHA:-HEAD}}"
    if [ -z "$BASE_BRANCH" ] || [ "$BASE_BRANCH" == "0000000000000000000000000000000000000000" ]; then
        BASE_BRANCH="FULL_SCAN"
    fi
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
if [ "$BASE_BRANCH" == "FULL_SCAN" ]; then
    CHANGED_JAVA=$(find . -type f \( -path '*/src/main/java/*.java' -o -path '*/src/test/java/*.java' \) -print | sed 's|^./||' | sort)
elif [ "${GITHUB_EVENT_NAME:-}" == "push" ]; then
    if ! CHANGED_JAVA=$(git diff --name-only --diff-filter=ACMRT "$BASE_BRANCH" "$HEAD_COMMIT" -- '*.java'); then
        echo "❌ 无法比较提交 $BASE_BRANCH 与 $HEAD_COMMIT。"
        exit 1
    fi
else
    if ! CHANGED_JAVA=$(git diff --name-only --diff-filter=ACMRT "$BASE_BRANCH"...HEAD -- '*.java'); then
        echo "❌ 无法比较基线 $BASE_BRANCH 与 HEAD。"
        exit 1
    fi
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

    if generate_styled_spotbugs_html_report "$module" "$module" "$class_list"; then
        echo "   HTML 可视化报告: $module/target/spotbugs-reports/spotbugs.html"
    fi

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

        if generate_styled_spotbugs_html_report "$module" "$module" "$class_list"; then
            echo "   HTML 可视化报告: $module/target/spotbugs-reports/spotbugs.html"
        fi

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
