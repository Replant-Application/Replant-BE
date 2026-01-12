import requests
import json
import random
import time

BASE_URL = "http://localhost:3000/api"

def print_result(step, response):
    status = "SUCCESS" if response.status_code < 400 else "FAILED"
    print(f"[{step}] Status: {status} ({response.status_code})")
    if status == "FAILED":
        print(f"Response: {response.text}")
        return False
    return True

def login_or_join(email, password="password123!"):
    print(f"\n--- Login/Join for {email} ---")
    
    # Try login first
    login_data = {"id": email, "password": password}
    response = requests.post(f"{BASE_URL}/auth/login", json=login_data)
    
    if response.status_code == 200:
        print(f"Login successful for {email}")
        return response.json()['data']['accessToken']
    
    print(f"Login failed, trying to join...")
    
    # Try join
    join_data = {
        "email": email,
        "password": password,
        "name": email.split('@')[0],
        "nickname": email.split('@')[0],
        "phone": f"010{random.randint(10000000, 99999999)}",
        "birthDate": "2000-01-01",
        "gender": "MALE",
        "region": "SEOUL"
    }
    response = requests.post(f"{BASE_URL}/auth/join", json=join_data)
    
    if response.status_code == 200:
        print(f"Join successful for {email}")
        return response.json()['data']['accessToken']
    
    print(f"Join failed: {response.text}")
    return None

def run_test():
    # 1. Login/Join users with random email to avoid collision/state issues
    rand_suffix = random.randint(1000, 9999)
    test_email = f"test_{rand_suffix}@test.com"
    observer_email = f"observer_{rand_suffix}@test.com"
    
    test_token = login_or_join(test_email)
    observer_token = login_or_join(observer_email)
    
    if not test_token or not observer_token:
        print("Failed to authenticate users. Exiting.")
        return

    headers_test = {"Authorization": f"Bearer {test_token}"}
    headers_observer = {"Authorization": f"Bearer {observer_token}"}

    # 2. Init TodoList (Get Random Missions)
    print("\n--- 1. Init TodoList (Fetch Random Missions) ---")
    response = requests.post(f"{BASE_URL}/todolists/init", headers=headers_test)
    if not print_result("Init TodoList", response): return

    random_missions = response.json()['data']['randomMissions']
    print(f"Fetched {len(random_missions)} random missions")
    for m in random_missions:
        print(f" - [{m['id']}] {m['title']} (Official: {m.get('isOfficial', 'Unknown')})")
        # Verify they are OFFICIAL
        # Note: API might not expose 'isOfficial' directly in SimpleResponse, but we can infer or rely on backend logic.
        
    random_mission_ids = [m['id'] for m in random_missions]

    # 3. Create TodoList
    print("\n--- 2. Create TodoList ---")
    # For custom missions, we need IDs. If we don't have any custom missions, we might need to create one or just use empty if allowed.
    # The API requires customMissionIds list, maybe empty is allowed.
    create_data = {
        "title": "My Test TodoList",
        "description": "Testing E2E flow",
        "randomMissionIds": random_mission_ids,
        "customMissionIds": [] # Using empty for now as creating custom mission is another flow
    }
    
    response = requests.post(f"{BASE_URL}/todolists", json=create_data, headers=headers_test)
    if not print_result("Create TodoList", response): return
    
    todolist = response.json()['data']
    todolist_id = todolist['id']
    print(f"Created TodoList ID: {todolist_id}")
    
    # Get the official mission to verify
    target_mission = todolist['missions'][0]
    target_mission_id = target_mission['missionId']
    print(f"Target Mission for Verification: {target_mission['missionTitle']} (ID: {target_mission_id})")

    # 4. Create Verification Post
    print("\n--- 3. Create Verification Post ---")
    # Need to find UserMission ID corresponding to this mission in the TodoList?
    # Wait, Post creation requires UserMission ID.
    # We need to find the UserMission ID for the mission we just added to the TodoList.
    # When TodoList is created, does it create UserMissions? Yes, it should. 
    # Let's check UserMissions for this user.
    
    response = requests.get(f"{BASE_URL}/user-missions?status=ASSIGNED", headers=headers_test)
    if not print_result("Get User Missions", response): return
    
    user_missions = response.json()['data']['content']
    target_user_mission = next((um for um in user_missions if um['missionId'] == target_mission_id), None)
    
    if not target_user_mission:
        print("Failed to find assigned UserMission for the target mission.")
        return
        
    user_mission_id = target_user_mission['id']
    print(f"Found UserMission ID: {user_mission_id}")
    
    post_data = {
        "missionId": target_mission_id, # Some APIs might require missionId, but usually UserMissionId
        "userMissionId": user_mission_id, 
        "content": "Mission Verified!",
        "type": "VERIFICATION", # Assuming enum or type field
        "imageUrls": []
    }
    
    # Note: The provided PostRequest DTO doesn't explicitly seem to have userMissionId based on previous context,
    # but let's try creating a post. 
    # Wait, PostService.createPost takes PostRequest. 
    # PostRequest usually needs to specify it's a verification post.
    # If the API for creating verification post is different, we need to know.
    # UserMissionController might have `verifyMission` which is different from creating a Post.
    # BUT the user requirement says: "미션 하나를 인증 눌러서 *인증게시글을 만들고*..."
    # So we assume creating a post linked to the mission.
    # Let's check PostRequest DTO if possible or just try.
    # Actually, often verification posts are created via `POST /api/posts` with type='VERIFICATION' and `userMissionId`.
    
    # Try standard post creation endpoint first
    # Need to check PostRequest structure from earlier.
    # It had content, title, imageUrls.
    # Maybe there's a specific endpoint for verification post?
    # `PostController` usually handles this.
    
    response = requests.post(f"{BASE_URL}/posts/verification", json=post_data, headers=headers_test)
    
    # If /posts/verification doesn't exist, try /posts with type
    if response.status_code == 404:
        print("Endpoint /posts/verification not found, trying /posts...")
        post_data['postType'] = "VERIFICATION"
        response = requests.post(f"{BASE_URL}/posts", json=post_data, headers=headers_test)
        
    if not print_result("Create Verification Post", response): return
    
    post_id = response.json()['data']['id']
    print(f"Created Verification Post ID: {post_id}")

    # 5. Toggle Like (Observer)
    print("\n--- 4. Toggle Like by Observer ---")
    response = requests.post(f"{BASE_URL}/posts/{post_id}/likes", headers=headers_observer)
    if not print_result("Toggle Like", response): return
    
    like_result = response.json()['data']
    print(f"Like Result: {like_result}")
    
    if like_result.get("verified"):
        print("✅ Mission Verified by Like!")
    else:
        print("❌ Mission NOT verified (maybe requires more likes?)")

    # 6. Check Badge
    print("\n--- 5. Check Badge ---")
    response = requests.get(f"{BASE_URL}/badges", headers=headers_test)
    if not print_result("Get Badges", response): return
    
    badges = response.json()['data']
    print(f"User Badges: {len(badges)}")
    # Should verify if a new badge was added related to the mission

    # 7. Write Review
    print("\n--- 6. Write Review ---")
    review_data = {
        "rating": 5,
        "content": "Great TodoList!"
    }
    response = requests.post(f"{BASE_URL}/todolists/{todolist_id}/reviews", json=review_data, headers=headers_observer) # Observer writes review?
    # User said "후기 작성까지 테스트해봐". Usually users write reviews for others' public todolists, or their own?
    # API allows writing reviews on public todolists.
    # TodoList must be public to be reviewed by others.
    # Let's make it public first or share it.
    
    # Share TodoList
    print("Sharing TodoList...")
    requests.put(f"{BASE_URL}/todolists/{todolist_id}/share", headers=headers_test)
    
    # Now observer writes review
    response = requests.post(f"{BASE_URL}/todolists/{todolist_id}/reviews", json=review_data, headers=headers_observer)
    if not print_result("Write Review", response): return
    print("Review written successfully")

if __name__ == "__main__":
    run_test()
