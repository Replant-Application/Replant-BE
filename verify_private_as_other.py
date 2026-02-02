#!/usr/bin/env python3
"""
리앤트(다른 사용자) 계정으로 로그인 후 커뮤니티 목록 조회
-> 비공개 글(작성자 user_id=2)이 목록에 포함되면 버그
"""

import os
import sys

try:
    from dotenv import load_dotenv
    load_dotenv(os.path.join(os.path.dirname(__file__), '.env'))
except ImportError:
    pass

# 비공개 글: id=144, 작성자 user_id=2 (이 글은 리앤트에게 안 보여야 함)
PRIVATE_POST_ID = 144
PRIVATE_AUTHOR_USER_ID = 2

LOGIN_EMAIL = "kimmireu010220@naver.com"
LOGIN_PASSWORD = "qwer1234"


def main():
    base_url = (os.environ.get('BASE_URL') or 'http://localhost:8080').rstrip('/')

    try:
        import requests
    except ImportError:
        print('requests 미설치. 실행: pip install requests')
        sys.exit(1)

    # 1) 로그인
    print('1. 리앤트 계정으로 로그인...')
    r_login = requests.post(
        f'{base_url}/api/auth/login',
        json={'id': LOGIN_EMAIL, 'password': LOGIN_PASSWORD},
        headers={'Content-Type': 'application/json'},
        timeout=10,
    )
    if r_login.status_code != 200:
        print(f'   로그인 실패: {r_login.status_code}', r_login.text[:200])
        sys.exit(1)
    body = r_login.json()
    data = body.get('data') if isinstance(body, dict) else body
    if not data:
        print('   로그인 응답에 data 없음:', body)
        sys.exit(1)
    access_token = data.get('accessToken') or (data.get('tokens') or {}).get('accessToken')
    if not access_token:
        print('   로그인 응답에 accessToken 없음:', data.keys())
        sys.exit(1)
    print('   로그인 성공, 토큰 획득')

    # 2) 커뮤니티 목록 조회
    print('2. 커뮤니티 목록 조회 (GET /api/community/posts)...')
    r_posts = requests.get(
        f'{base_url}/api/community/posts',
        params={'page': 0, 'size': 100},
        headers={'Authorization': f'Bearer {access_token}'},
        timeout=10,
    )
    if r_posts.status_code != 200:
        print(f'   목록 조회 실패: {r_posts.status_code}', r_posts.text[:200])
        sys.exit(1)
    body = r_posts.json()
    data = body.get('data') if isinstance(body, dict) else body
    content = data.get('content', []) if isinstance(data, dict) else (data if isinstance(data, list) else [])
    ids = [p.get('id') for p in content]
    print(f'   목록 게시글 수: {len(content)}, id 목록(일부): {ids[:15]}...')

    # 3) 비공개 글(144)이 목록에 있는지 확인
    if PRIVATE_POST_ID in ids:
        print(f'\n[버그] 비공개 글 id={PRIVATE_POST_ID}(작성자 user_id={PRIVATE_AUTHOR_USER_ID})가')
        print('       리앤트(다른 사용자) 커뮤니티 목록에 노출됩니다.')
        sys.exit(2)
    else:
        print(f'\n[OK] 비공개 글 id={PRIVATE_POST_ID}는 리앤트 목록에 없습니다. (커뮤니티에서 안 보임)')
        sys.exit(0)


if __name__ == '__main__':
    main()
