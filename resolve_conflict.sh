#!/bin/bash

# 충돌 해결 스크립트
# TodoListController 이동으로 인한 충돌 해결

echo "=== 충돌 해결 시작 ==="

# 1. main 브랜치 최신화
echo "1. main 브랜치 fetch 중..."
git fetch origin main

# 2. merge 시도
echo "2. main 브랜치 merge 시도..."
git merge origin/main --no-commit --no-ff 2>&1 | tee /tmp/merge_output.txt

# 3. 충돌 확인
if grep -q "CONFLICT" /tmp/merge_output.txt; then
    echo "3. 충돌 발견! 해결 중..."
    
    # controller/TodoListController.java 삭제 (우리가 이미 이동했으므로)
    if [ -f "src/main/java/com/app/replant/controller/TodoListController.java" ]; then
        echo "   - controller/TodoListController.java 삭제 중..."
        git rm src/main/java/com/app/replant/controller/TodoListController.java
    fi
    
    # domain/missionset/controller/TodoListController.java 유지
    if [ -f "src/main/java/com/app/replant/domain/missionset/controller/TodoListController.java" ]; then
        echo "   - domain/missionset/controller/TodoListController.java 유지 중..."
        git add src/main/java/com/app/replant/domain/missionset/controller/TodoListController.java
    fi
    
    # 충돌 해결 완료
    echo "4. 충돌 해결 완료! 커밋 중..."
    git commit -m "[refactor] #13 - 충돌 해결: TodoListController 이동 유지"
    
    echo "=== 충돌 해결 완료 ==="
    echo "다음 명령어로 push하세요:"
    echo "  git push origin feat/#13"
else
    echo "3. 충돌 없음. 이미 해결된 상태입니다."
    git merge --abort 2>/dev/null || true
fi

rm -f /tmp/merge_output.txt
