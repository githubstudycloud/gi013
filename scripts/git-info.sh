#!/bin/bash
# 获取 Git 提交信息
echo "=== Git Commit 信息 ==="
echo "最新 Commit:"
git log -1 --pretty=format:"Hash: %H%nMessage: %s%nTime: %ai%nAuthor: %an"
echo ""
echo "远程仓库:"
git remote get-url origin
echo ""
echo "分支:"
git branch --show-current
