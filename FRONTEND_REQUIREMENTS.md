# 프론트엔드 요구사항 - 미션 조회 API 변경사항

## 📌 변경 사항 요약

백엔드에서 `GET /api/missions/my` (getUserMissions) API의 조회 로직이 변경되었습니다.

### 주요 변경점
1. **완료된 커스텀 미션도 포함**: 오늘 완료한 커스텀 미션이 응답에 포함됩니다.
2. **다음날 자동 초기화**: 어제 완료한 미션은 오늘 조회되지 않습니다.

---

## 🔍 API 응답 구조 (변경 없음)

API 응답 구조 자체는 변경되지 않았지만, **응답에 포함되는 미션의 범위**가 변경되었습니다.

### 현재 응답 구조
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "missionType": "OFFICIAL", // 또는 "CUSTOM"
        "mission": {
          "id": 10,
          "title": "미션 제목",
          "category": "DAILY_LIFE",
          "verificationType": "COMMUNITY",
          "requiredMinutes": 30
        },
        "assignedAt": "2024-01-15T09:00:00",
        "dueDate": "2024-01-22T23:59:59",
        "status": "ASSIGNED" // "PENDING" 또는 "COMPLETED"
      }
    ],
    "totalElements": 10, // 변경됨: 완료된 커스텀 미션 포함
    "totalPages": 1,
    "number": 0
  }
}
```

---

## ⚠️ 프론트엔드에서 처리해야 할 사항

### 1. **완료된 미션(COMPLETED) 상태 처리**

#### 변경 전
- `status: "COMPLETED"`인 미션은 응답에 포함되지 않았습니다.
- 완료된 미션은 `/api/missions/my/history`에서만 조회 가능했습니다.

#### 변경 후
- **오늘 날짜에 할당된 완료된 미션**도 응답에 포함됩니다.
- `status: "COMPLETED"` 상태를 UI에서 적절히 표시해야 합니다.

#### 권장 처리 방법
```typescript
// 미션 상태에 따른 UI 처리
const renderMissionItem = (mission: UserMission) => {
  if (mission.status === 'COMPLETED') {
    return (
      <MissionItem 
        status="completed"
        isDisabled={true}
        showCheckIcon={true}
      />
    );
  }
  
  if (mission.status === 'ASSIGNED') {
    return (
      <MissionItem 
        status="active"
        onClick={handleVerifyMission}
      />
    );
  }
  
  // PENDING 상태 처리
  return <MissionItem status="pending" />;
};
```

---

### 2. **totalMissions 계산 로직 확인**

#### 변경 전
```typescript
// 완료된 커스텀 미션이 제외되어 totalMissions가 부정확했을 수 있음
const totalMissions = response.data.totalElements;
```

#### 변경 후
```typescript
// 이제 완료된 커스텀 미션도 포함되어 totalMissions가 정확합니다
const totalMissions = response.data.totalElements; // ✅ 정확한 총 미션 수
const completedMissions = missions.filter(m => m.status === 'COMPLETED').length;
const activeMissions = missions.filter(m => m.status === 'ASSIGNED' || m.status === 'PENDING').length;

// 진행률 계산 시 주의
const progress = (completedMissions / totalMissions) * 100;
```

---

### 3. **다음날 미션 초기화 처리**

#### 변경 사항
- **어제 완료한 미션은 오늘 자동으로 조회되지 않습니다.**
- 사용자가 다음날 앱을 열면 완료한 미션 목록이 비어있을 수 있습니다.

#### 권장 처리 방법
```typescript
// 미션 목록 조회 후 처리
const fetchMissions = async () => {
  const response = await getUserMissions();
  const missions = response.data.content;
  
  // 오늘 완료한 미션과 진행 중인 미션 구분
  const today = new Date().toISOString().split('T')[0];
  const todayCompleted = missions.filter(m => 
    m.status === 'COMPLETED' && 
    m.assignedAt.startsWith(today)
  );
  
  const activeMissions = missions.filter(m => 
    m.status === 'ASSIGNED' || m.status === 'PENDING'
  );
  
  // UI 업데이트
  setCompletedMissions(todayCompleted);
  setActiveMissions(activeMissions);
  
  // 만약 모든 미션이 완료되고 어제 완료한 미션만 있다면
  // (어제 완료한 미션은 조회되지 않으므로)
  if (activeMissions.length === 0 && todayCompleted.length === 0) {
    // 새로운 미션을 생성하라는 안내 표시
    showNewMissionPrompt();
  }
};
```

---

### 4. **미션 상태별 UI 표시**

#### 권장 UI 표시 방법

| 상태 | 표시 방법 | 클릭 가능 여부 | 설명 |
|------|----------|---------------|------|
| `ASSIGNED` | 활성화된 미션 카드 | ✅ 가능 | 인증 버튼 표시 |
| `PENDING` | 대기 중 미션 카드 | ⚠️ 제한적 | 대기 상태 표시 |
| `COMPLETED` | 완료된 미션 카드 | ❌ 불가능 | 체크 아이콘 + 회색 처리 |

#### 예시 UI 코드
```typescript
interface MissionCardProps {
  mission: UserMission;
}

const MissionCard: React.FC<MissionCardProps> = ({ mission }) => {
  const isCompleted = mission.status === 'COMPLETED';
  const isActive = mission.status === 'ASSIGNED';
  
  return (
    <Card 
      className={cn(
        "mission-card",
        isCompleted && "mission-card--completed",
        isActive && "mission-card--active"
      )}
      onClick={isCompleted ? undefined : handleClick}
    >
      <CardHeader>
        <CardTitle>{mission.mission.title}</CardTitle>
        {isCompleted && (
          <CheckCircle className="text-green-500" />
        )}
      </CardHeader>
      
      {isActive && (
        <CardFooter>
          <Button onClick={handleVerify}>인증하기</Button>
        </CardFooter>
      )}
      
      {isCompleted && (
        <CardFooter>
          <Badge variant="success">완료됨</Badge>
        </CardFooter>
      )}
    </Card>
  );
};
```

---

### 5. **미션 필터링 로직 (선택사항)**

필요한 경우 프론트에서 추가 필터링을 할 수 있습니다:

```typescript
// 상태별 필터링
const filterMissionsByStatus = (missions: UserMission[], status: UserMissionStatus) => {
  return missions.filter(m => m.status === status);
};

// 오늘 할당된 미션만 필터링
const filterTodayMissions = (missions: UserMission[]) => {
  const today = new Date().toISOString().split('T')[0];
  return missions.filter(m => 
    m.assignedAt.startsWith(today)
  );
};

// 완료된 미션만 필터링
const completedMissions = filterMissionsByStatus(missions, 'COMPLETED');

// 활성 미션만 필터링 (진행 중)
const activeMissions = missions.filter(m => 
  m.status === 'ASSIGNED' || m.status === 'PENDING'
);
```

---

### 6. **미션 이력 조회 (변경 없음)**

완료된 미션의 전체 이력을 조회하려면 기존 API를 사용합니다:

```typescript
// 전체 완료 이력 조회 (어제, 오늘 모두 포함)
const fetchMissionHistory = async () => {
  const response = await fetch('/api/missions/my/history');
  // 이 API는 어제 완료한 미션도 포함합니다
};
```

---

## 📝 체크리스트

프론트엔드 개발 시 확인해야 할 사항:

- [ ] `status: "COMPLETED"`인 미션을 UI에서 적절히 표시하는가?
- [ ] 완료된 미션은 클릭할 수 없도록 처리하는가?
- [ ] `totalMissions` 계산 로직이 완료된 커스텀 미션을 포함하는가?
- [ ] 다음날 미션이 자동으로 사라지는 것을 사용자에게 적절히 안내하는가?
- [ ] 오늘 완료한 미션과 어제 완료한 미션을 구분하여 표시하는가? (필요한 경우)

---

## 🔗 관련 API

- `GET /api/missions/my` - 내 미션 목록 조회 (변경됨)
- `GET /api/missions/my/history` - 미션 완료 이력 조회 (변경 없음)
- `GET /api/missions/my/{userMissionId}` - 미션 상세 조회 (변경 없음)

---

## 💡 참고사항

1. **백엔드 필터링 기준**: `assignedAt`이 오늘 날짜인 미션만 조회됩니다.
2. **상태 필터**: `ASSIGNED`, `PENDING`, `COMPLETED` (오늘 날짜만) 모두 포함됩니다.
3. **정렬**: `assignedAt` 기준 내림차순으로 정렬됩니다.
4. **페이징**: 기존과 동일하게 작동합니다.

---

## 📞 문의

추가 질문이나 이슈가 있으면 백엔드 팀에 문의해주세요.