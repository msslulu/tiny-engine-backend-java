#!/bin/bash
# ============================================================
# spotbugs-incremental.sh - 增量 SpotBugs 扫描
# 功能：只分析本次变更的 Java 文件对应的类
# ============================================================

set -e

echo "========================================"
echo "  SpotBugs 增量扫描"
echo "========================================"

# 确定目标分支
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

# 获取变更的 Java 文件
CHANGED_JAVA=$(git diff --name-only "$BASE_BRANCH" HEAD 2>/dev/null | grep '\.java$' || true)

if [ -z "$CHANGED_JAVA" ]; then
    echo "✅ 没有 Java 文件变更，跳过 SpotBugs 扫描。"
    exit 0
fi

echo "📝 变更的 Java 文件："
echo "$CHANGED_JAVA"

# 提取类名列表
class_list=""
for file in $CHANGED_JAVA; do
    # 文件可能已被删除，跳过
    if [ ! -f "$file" ]; then
        continue
    fi
    # 提取包名（假设文件中有 package 声明）
    pkg=$(grep '^package' "$file" | sed -E 's/package\s+([^;]+);.*/\1/' | head -1)
    if [ -z "$pkg" ]; then
        echo "⚠️ 跳过 $file（未找到 package 声明）"
        continue
    fi
    # 提取类名（不含 .java 后缀）
    classname=$(basename "$file" .java)
    fqdn="$pkg.$classname"
    if [ -z "$class_list" ]; then
        class_list="$fqdn"
    else
        class_list="$class_list,$fqdn"
    fi
done

if [ -z "$class_list" ]; then
    echo "⚠️ 未能提取到任何有效的类名，跳过 SpotBugs。"
    exit 0
fi

echo "📋 待分析的类：$class_list"
echo "----------------------------------------"

# 执行 SpotBugs 增量分析
echo "🚀 执行 SpotBugs 增量扫描..."
mvn spotbugs:check -Dspotbugs.onlyAnalyze="$class_list"