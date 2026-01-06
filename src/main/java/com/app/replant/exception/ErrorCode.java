package com.app.replant.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통 에러
    NO_TOKEN(HttpStatus.UNAUTHORIZED, "COMMON-001", "로그인을 해주세요"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "COMMON-002", "토큰값이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "COMMON-003", "토큰이 유효하지않습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-004", "서버가 점검중입니다"),
    INVALID_MEMBER(HttpStatus.FORBIDDEN, "COMMON-005", "비활성화된 계정입니다"),

    // 회원 관리 - 본인 인증
    AUTH_FAIL(HttpStatus.BAD_REQUEST, "ACCOUNT-001", "인증 번호가 일치하지 않습니다."),
    TIME_OUT(HttpStatus.REQUEST_TIMEOUT, "ACCOUNT-002", "인증 시간이 초과되었습니다. 다시 인증을 시도해주세요."),
    UNAUTHORIZED_REQUEST(HttpStatus.UNAUTHORIZED, "ACCOUNT-003", "인증이 확인되지 않았습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ACCOUNT-004", "인증에 실패하였습니다."),

    // 회원 관리 - 회원 가입
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "ACCOUNT-005", "이미 사용 중인 이메일이 있습니다."),
    REQUIRED_MISSING(HttpStatus.BAD_REQUEST, "ACCOUNT-007", "필수 요소가 입력되지 않았습니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "ACCOUNT-008", "형식에 맞지 않는 입력입니다."),
    SIGNIN_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-009", "회원가입에 실패하였습니다."),

    // 회원 관리 - 로그인
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT-010", "아이디가 존재하지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ACCOUNT-011", "비밀번호가 틀립니다."),

    // 회원 관리 - 아이디/비밀번호 찾기
    INVALID_USER(HttpStatus.NOT_FOUND, "ACCOUNT-012", "등록되지 않은 사용자입니다."),
    INVALID_PHONE(HttpStatus.NOT_FOUND, "ACCOUNT-013", "등록되지 않은 전화번호입니다."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "ACCOUNT-014", "비활성화 된 계정입니다."),

    // 회원 관리 - 비밀번호 재설정
    TEMP_PASSWORD_GEN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-015", "임시 비밀번호 생성에 실패했습니다."),
    TEMP_PASSWORD_INCORRECT(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-016", "부여한 임시 비밀번호와 일치하지 않습니다."),
    UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-017", "임시 비밀번호 저장이 실패했습니다."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "ACCOUNT-020", "새 비밀번호가 기존 비밀번호와 동일합니다."),

    // 회원 관리 - 로그아웃
    DATA_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-018", "로그아웃 처리가 불가합니다."),
    REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "ACCOUNT-019", "이미 로그아웃 된 계정입니다."),

    // OAuth
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_REQUEST, "OAUTH-001", "OAuth 제공자로부터 정보를 가져오는데 실패했습니다."),
    INVALID_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "OAUTH-002", "지원하지 않는 OAuth 제공자입니다."),

    // 소비 내역
    CARD_INVALID(HttpStatus.BAD_REQUEST, "HISTORY-001", "카드가 유효하지 않습니다."),
    HISTORY_ISNULL(HttpStatus.NOT_FOUND, "HISTORY-002", "해당 소비 내역이 존재하지 않습니다."),
    HISTORY_ISNOTYOURS(HttpStatus.FORBIDDEN, "HISTORY-003", "해당 소비 내역은 수정이 불가합니다."),
    HISTORY_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "HISTORY-004", "수정에 실패했습니다."),
    HISTORY_INVALID_DATE(HttpStatus.BAD_REQUEST, "HISTORY-005", "유효하지 않은 연/월 값입니다."),
    // 추가
    HISTORY_INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "HISTORY-006", "유효하지 않은 카테고리 값입니다."),
    HISTORY_INCLUDE_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "HISTORY-007", "지출 합계 포함 여부 수정에 실패했습니다."),
    HISTORY_CATEGORY_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "HISTORY-008", "카테고리 수정에 실패했습니다."),
    HISTORY_INVALID_DUTCHPAY(HttpStatus.BAD_REQUEST, "HISTORY-009", "유효하지 않은 더치페이 인원 수입니다."),
    HISTORY_DUTCHPAY_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "HISTORY-010", "더치페이 인원 수정에 실패했습니다."),
    HISTORY_PRICE_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "HISTORY-011", "금액 수정에 실패했습니다."),
    HISTORY_INVALID_PRICE(HttpStatus.BAD_REQUEST, "HISTORY-012", "유효하지 않은 금액 값입니다."),

    // 소비 일기
    DIARY_INSERT_INVALID(HttpStatus.BAD_REQUEST, "DIARY-001", "일기가 너무 깁니다."),
    DIARY_ISNOTYOURS(HttpStatus.FORBIDDEN, "DIARY-002", "해당 일기는 수정이 불가합니다."),
    DIARY_UPDATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "DIARY-003", "수정에 실패했습니다."),
    DIARY_TOKEN_REVOKED(HttpStatus.FORBIDDEN, "DIARY-004", "해당 일기는 삭제에 불가합니다."),
    DIARY_DELETE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "DIARY-005", "삭제에 실패했습니다."),
    // 추가
    DIARY_ISNULL(HttpStatus.NOT_FOUND, "DIARY-006", "소비 일기가 존재하지 않습니다."),
    DIARY_INVALID_DATE(HttpStatus.BAD_REQUEST, "DIARY-007", "유효하지 않은 날짜 형식입니다."),
    DIARY_DUPLICATE_DATE(HttpStatus.CONFLICT, "DIARY-008", "해당 날짜의 일기가 이미 존재합니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "DIARY-009", "입력 값이 유효하지 않습니다."),
    DIARY_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "DIARY-010", "소비 일기 생성에 실패했습니다."),

    // 카드 - 카드 생성
    INVALID_CARD_NUMBER(HttpStatus.BAD_REQUEST, "CARD-001", "카드 번호가 옳지 않습니다."),
    CARD_EXPIRED(HttpStatus.BAD_REQUEST, "CARD-002", "카드 유효 기간이 만료되었습니다."),
    INVALID_CVC(HttpStatus.BAD_REQUEST, "CARD-003", "CVC가 일치하지 않습니다."),
    INVALID_CARD_PASSWORD(HttpStatus.UNAUTHORIZED, "CARD-004", "카드 비밀번호가 일치하지 않습니다."),
    INVALID_BIRTHDATE(HttpStatus.BAD_REQUEST, "CARD-005", "해당되는 생년월일이 없습니다."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "CARD-006", "별명은 10자 이하로 설정해야 합니다."),
    CARD_ELEMENT_MISSING(HttpStatus.BAD_REQUEST, "CARD-007", "카드 요소가 입력되지 않았습니다."),

    // 카드 - 카드 삭제/수정
    CARD_TOKEN_REVOKED(HttpStatus.GONE, "CARD-008", "이미 삭제된 카드입니다."),
    CARD_ISNULL(HttpStatus.NOT_FOUND, "CARD-009", "해당 카드는 존재하지 않습니다."),
    CARD_ISNOTYOURS(HttpStatus.FORBIDDEN, "CARD-010", "해당 카드는 수정이 불가합니다."),
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD-011", "카드를 찾을 수 없습니다."),
    CARD_IN_USE(HttpStatus.CONFLICT, "CARD-012", "이미 사용 중인 카드입니다."),
    CARD_ALREADY_OWNED(HttpStatus.CONFLICT, "CARD-013", "이미 보유한 카드입니다."),

    // 회원
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-001", "회원을 찾을 수 없습니다."),

    // 목표 - 목표설정 입력/수정/조회
    GOAL_INVALIDVALUE(HttpStatus.BAD_REQUEST, "GOAL-001", "제한금액은 급여보다 클 수 없습니다."),
    GOAL_INVALIDNUM(HttpStatus.BAD_REQUEST, "GOAL-002", "목표치가 올바르지 않습니다."),
    GOAL_ISNOTYOURS(HttpStatus.FORBIDDEN, "GOAL-003", "해당 달성도는 조회가 불가합니다."),
    GOAL_ISNULL(HttpStatus.NOT_FOUND, "GOAL-004", "해당 달성도는 존재하지 않습니다."),

    // 메일
    MAIL_NOTFOUND(HttpStatus.BAD_REQUEST, "MAIL-001", "이메일을 찾을 수 없습니다"),

    // 채팅
    CHAT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-001", "사용자를 찾을 수 없습니다."),
    CHAT_LLM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT-002", "AI 응답 생성에 실패했습니다."),
    CHAT_DATA_LOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT-003", "사용자 데이터 조회에 실패했습니다."),

    // Reant
    REANT_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "리앤트를 찾을 수 없습니다"),

    // Mission
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "미션을 찾을 수 없습니다"),
    USER_MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "할당된 미션을 찾을 수 없습니다"),
    MISSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "M003", "이미 완료된 미션입니다"),
    MISSION_EXPIRED(HttpStatus.BAD_REQUEST, "M004", "만료된 미션입니다"),

    // CustomMission
    CUSTOM_MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "커스텀 미션을 찾을 수 없습니다"),
    NOT_MISSION_CREATOR(HttpStatus.FORBIDDEN, "CM002", "미션 생성자만 수정/삭제할 수 있습니다"),
    CUSTOM_MISSION_NOT_PUBLIC(HttpStatus.FORBIDDEN, "CM003", "공개되지 않은 미션입니다"),

    // Badge
    BADGE_REQUIRED(HttpStatus.FORBIDDEN, "B001", "유효한 뱃지가 필요합니다"),
    BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "B002", "뱃지를 찾을 수 없습니다"),

    // Review
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "RV001", "이미 리뷰를 작성했습니다"),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "RV002", "리뷰를 찾을 수 없습니다"),

    // QnA
    QNA_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "QnA를 찾을 수 없습니다"),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "Q002", "답변을 찾을 수 없습니다"),
    NOT_QUESTIONER(HttpStatus.FORBIDDEN, "Q003", "질문 작성자만 채택할 수 있습니다"),
    ANSWER_ALREADY_ACCEPTED(HttpStatus.BAD_REQUEST, "Q004", "이미 답변이 채택되었습니다"),

    // Verification
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "V001", "인증글을 찾을 수 없습니다"),
    ALREADY_VOTED(HttpStatus.BAD_REQUEST, "V002", "이미 투표했습니다"),
    SELF_VOTE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "V003", "본인 글에는 투표할 수 없습니다"),
    MODIFICATION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "V004", "수정/삭제가 불가능한 상태입니다"),
    GPS_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "V005", "목표 위치에서 너무 멀리 있습니다"),
    TIME_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "V006", "필요 시간을 충족하지 못했습니다"),
    MISSION_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "V007", "이미 인증된 미션입니다"),
    INVALID_VERIFICATION_TYPE(HttpStatus.BAD_REQUEST, "V008", "잘못된 인증 타입입니다"),
    INVALID_GPS_DATA(HttpStatus.BAD_REQUEST, "V009", "GPS 데이터가 올바르지 않습니다"),
    GPS_NOT_REQUIRED(HttpStatus.BAD_REQUEST, "V010", "이 미션은 GPS 인증이 필요하지 않습니다"),
    INVALID_TIME_DATA(HttpStatus.BAD_REQUEST, "V011", "시간 데이터가 올바르지 않습니다"),
    TIME_NOT_REQUIRED(HttpStatus.BAD_REQUEST, "V012", "이 미션은 시간 인증이 필요하지 않습니다"),
    VERIFICATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "V013", "이미 인증글이 존재합니다"),
    INVALID_IMAGE_DATA(HttpStatus.BAD_REQUEST, "V014", "이미지 데이터가 올바르지 않습니다"),
    DELETION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "V015", "삭제가 불가능한 상태입니다"),
    VOTING_NOT_ALLOWED(HttpStatus.FORBIDDEN, "V016", "투표가 불가능한 상태입니다"),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "게시글을 찾을 수 없습니다"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "댓글을 찾을 수 없습니다"),
    NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "P003", "댓글 작성자가 아닙니다"),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "P004", "작성자만 수정/삭제할 수 있습니다"),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST, "P005", "부모 댓글이 올바르지 않습니다"),

    // Recommendation
    RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RC001", "추천을 찾을 수 없습니다"),
    RECOMMENDATION_EXPIRED(HttpStatus.BAD_REQUEST, "RC002", "만료된 추천입니다"),
    RECOMMENDATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "RC003", "이미 처리된 추천입니다"),
    INVALID_RECOMMENDATION_STATUS(HttpStatus.BAD_REQUEST, "RC004", "유효하지 않은 추천 상태입니다"),

    // Chat
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "채팅방을 찾을 수 없습니다"),
    CHAT_ROOM_INACTIVE(HttpStatus.FORBIDDEN, "CH002", "비활성화된 채팅방입니다"),
    CHAT_ROOM_NOT_ACTIVE(HttpStatus.FORBIDDEN, "CH002", "비활성화된 채팅방입니다"),
    NOT_CHAT_PARTICIPANT(HttpStatus.FORBIDDEN, "CH003", "채팅방 참여자가 아닙니다"),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다"),

    // Graduation
    GRADUATION_NOT_ELIGIBLE(HttpStatus.BAD_REQUEST, "G001", "졸업 조건을 충족하지 못했습니다"),

    // Wakeup Mission
    WAKEUP_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "WM001", "기상 미션 설정을 찾을 수 없습니다"),
    WAKEUP_SETTING_ALREADY_EXISTS(HttpStatus.CONFLICT, "WM002", "해당 주차의 기상 미션 설정이 이미 존재합니다"),
    WAKEUP_TIME_NOT_IN_SLOT(HttpStatus.BAD_REQUEST, "WM003", "설정된 시간대가 아닙니다"),
    WAKEUP_VERIFICATION_TOO_EARLY(HttpStatus.BAD_REQUEST, "WM004", "아직 인증 시간이 아닙니다"),
    WAKEUP_VERIFICATION_TOO_LATE(HttpStatus.BAD_REQUEST, "WM005", "인증 시간이 지났습니다"),

    // Admin Mission
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "AD001", "관리자만 접근 가능합니다");

    private final HttpStatus statusCode;
    private final String errorCode;
    private final String errorMsg;
}
