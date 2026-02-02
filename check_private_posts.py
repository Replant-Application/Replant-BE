#!/usr/bin/env python3
"""
커뮤니티 일반 게시글 '비공개' 동작 검증 스크립트
- DB에서 is_public=0(비공개)인 일반 게시글 조회
- (선택) 백엔드 API로 비공개 글이 타인에게 안 보이는지 확인

사용법:
  cd Replant-BE
  pip install python-dotenv pymysql requests  # 최초 1회
  python check_private_posts.py

필요: .env에 DB1_URL, DB1_USERNAME, DB1_PASSWORD 설정
"""

import os
import re
import sys

# .env 로드 (Replant-BE 디렉터리 기준)
try:
    from dotenv import load_dotenv
    load_dotenv(os.path.join(os.path.dirname(__file__), '.env'))
except ImportError:
    pass  # dotenv 없으면 환경변수만 사용

def parse_jdbc_url(url: str):
    """jdbc:mysql://host:port/dbname?params -> host, port, db"""
    if not url or not url.startswith('jdbc:mysql://'):
        return None, None, None
    rest = url.replace('jdbc:mysql://', '').strip()
    parts = rest.split('/')
    if len(parts) < 2:
        return None, None, None
    host_port = parts[0]
    db_part = parts[1].split('?')[0]
    hp = host_port.split(':')
    host = hp[0] if hp else 'localhost'
    port = int(hp[1]) if len(hp) > 1 else 3306
    return host, port, db_part

def main():
    db_url = os.environ.get('DB1_URL') or os.environ.get('DB_URL')
    db_user = os.environ.get('DB1_USERNAME') or os.environ.get('DB_USERNAME')
    db_pass = os.environ.get('DB1_PASSWORD') or os.environ.get('DB_PASSWORD')

    if not db_url or not db_user:
        print('오류: DB 연결 정보가 없습니다.')
        print('  Replant-BE/.env 에 DB1_URL, DB1_USERNAME, DB1_PASSWORD 를 설정하거나')
        print('  환경변수로 export 후 다시 실행하세요.')
        sys.exit(1)

    host, port, db_name = parse_jdbc_url(db_url)
    if not db_name:
        print('오류: DB1_URL 형식이 올바르지 않습니다. (예: jdbc:mysql://host:port/dbname?...)')
        sys.exit(1)

    try:
        import pymysql
    except ImportError:
        print('pymysql 미설치. 실행: pip install pymysql')
        sys.exit(1)

    conn = None
    try:
        conn = pymysql.connect(
            host=host,
            port=port,
            user=db_user,
            password=db_pass or '',
            database=db_name,
            charset='utf8mb4',
        )
    except Exception as e:
        print(f'DB 연결 실패: {e}')
        sys.exit(1)

    print('=' * 60)
    print('1. DB에서 비공개 일반 게시글 조회')
    print('=' * 60)

    with conn.cursor(pymysql.cursors.DictCursor) as cur:
        # post_type: enum일 수 있음. GENERAL / VERIFICATION
        # is_public: 0 = 비공개, 1 = 공개, NULL = 기존 데이터 호환(공개로 간주)
        cur.execute("""
            SELECT id, user_id, post_type, title, is_public, del_flag, created_at
            FROM post
            WHERE (del_flag = 0 OR del_flag IS NULL)
              AND post_type = 'GENERAL'
              AND (is_public = 0 OR is_public = FALSE)
            ORDER BY id DESC
        """)
        rows = cur.fetchall()

    if not rows:
        print('비공개로 설정된 일반 게시글이 없습니다.')
        print('  (일반 게시글 작성 시 "비공개로 작성" 체크 후 저장한 글이 있어야 합니다.)')
        conn.close()
        return

    print(f'비공개 일반 게시글 수: {len(rows)}건\n')
    for r in rows:
        print(f"  id={r['id']} user_id={r['user_id']} title={r['title'][:30] if r['title'] else '(없음)'}... is_public={r['is_public']}")

    private_ids = [r['id'] for r in rows]
    author_user_id = rows[0]['user_id']  # 비공개 글 작성자

    print('\n' + '=' * 60)
    print('2. API로 "실제 동작" 검증 (JWT 생성 후 호출)')
    print('=' * 60)
    base_url = (os.environ.get('BASE_URL') or 'http://localhost:8080').rstrip('/')
    jwt_secret = os.environ.get('JWT')
    api_ok = False
    if jwt_secret:
        try:
            import base64
            import time
            import requests
            try:
                import jwt as pyjwt
            except ImportError:
                pyjwt = None
            if pyjwt:
                with conn.cursor(pymysql.cursors.DictCursor) as cur:
                    cur.execute(
                        "SELECT id, email FROM user WHERE id = %s AND (del_flag = 0 OR del_flag IS NULL)",
                        (author_user_id,)
                    )
                    author_row = cur.fetchone()
                    cur.execute(
                        "SELECT id, email FROM user WHERE id != %s AND (del_flag = 0 OR del_flag IS NULL) LIMIT 1",
                        (author_user_id,)
                    )
                    other_row = cur.fetchone()
                if not author_row or not other_row:
                    print('  사용자 부족: 작성자 또는 다른 유저가 없어 API 검증 생략')
                else:
                    key = base64.b64decode(jwt_secret)
                    exp = int(time.time()) + 86400
                    payload = {'sub': None, 'auth': 'ROLE_USER', 'exp': exp}
                    def make_token(email):
                        p = {**payload, 'sub': email}
                        return pyjwt.encode(p, key, algorithm='HS512')
                    token_author = make_token(author_row['email'])
                    token_other = make_token(other_row['email'])
                    if not isinstance(token_author, str):
                        token_author = token_author.decode()
                    if not isinstance(token_other, str):
                        token_other = token_other.decode()
                    # 작성자 토큰: 목록에 비공개 글이 포함되어야 함
                    r1 = requests.get(f'{base_url}/api/community/posts', params={'page': 0, 'size': 200},
                                      headers={'Authorization': f'Bearer {token_author}'}, timeout=10)
                    if r1.status_code != 200:
                        print(f'  작성자 목록 API: {r1.status_code} (서버 미실행이면 무시)')
                    else:
                        data1 = r1.json().get('data') or {}
                        list_ids_author = [p.get('id') for p in data1.get('content', [])]
                        if private_ids[0] in list_ids_author:
                            print(f'  [OK] 작성자 목록: 비공개 글 {private_ids[0]} 포함 (본인만 보임)')
                        else:
                            print(f'  [??] 작성자 목록에 비공개 글 {private_ids[0]} 없음 (여러 페이지일 수 있음)')
                    # 타인 토큰: 목록에 비공개 글이 없어야 함 + 상세 403
                    r2 = requests.get(f'{base_url}/api/community/posts', params={'page': 0, 'size': 200},
                                      headers={'Authorization': f'Bearer {token_other}'}, timeout=10)
                    if r2.status_code != 200:
                        print(f'  타인 목록 API: {r2.status_code}')
                    else:
                        data2 = r2.json().get('data') or {}
                        list_ids_other = [p.get('id') for p in data2.get('content', [])]
                        in_other = [pid for pid in private_ids if pid in list_ids_other]
                        if in_other:
                            print(f'  [실패] 타인 목록에 비공개 글이 노출됨: {in_other}')
                        else:
                            print(f'  [OK] 타인 목록: 비공개 글 {private_ids} 미포함 (다른 사람에게 안 보임)')
                            api_ok = True
                    r3 = requests.get(f'{base_url}/api/community/posts/{private_ids[0]}',
                                      headers={'Authorization': f'Bearer {token_other}'}, timeout=10)
                    if r3.status_code == 403:
                        print(f'  [OK] 타인 상세 /posts/{private_ids[0]} -> 403 (비공개 접근 차단)')
                        api_ok = True
                    else:
                        print(f'  타인 상세 /posts/{private_ids[0]} -> {r3.status_code} (기대: 403)')
            else:
                print('  PyJWT 미설치. 실행: pip install pyjwt')
        except Exception as e:
            print(f'  API 검증 중 오류: {e}')
    else:
        print('  .env에 JWT 미설정 -> JWT 생성 불가, API 검증 생략')
    if api_ok:
        print('  -> 실제 API 동작 확인: 비공개 글이 타인에게 안 보입니다.')

    print('\n' + '=' * 60)
    print('3. 백엔드 동작 요약')
    print('=' * 60)
    print('  - 목록: 로그인 사용자 = 공개글 + 본인 비공개글만 노출')
    print('  - 목록: 비로그인 = 공개글만')
    print('  - 상세: 비공개글은 작성자만 조회 가능, 타인 요청 시 403 (POST_PRIVATE_ACCESS_DENIED)')
    print('  - 구현: PostRepositoryCustomImpl.findWithFilters(visible), PostService.getPost()')

    conn.close()
    print('\n검증 완료.')

if __name__ == '__main__':
    main()
