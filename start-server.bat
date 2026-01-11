@echo off
chcp 65001 > nul
set "DB1_URL=jdbc:mysql://113.198.66.75:13150/replant?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true"
set "DB1_USERNAME=replant_admin"
set "DB1_PASSWORD=replant2025"
set "JWT=FHjL73XX2KdsjunrdePaZjO+VriGoeP5+ErcTeEiCsMRr9MYoMU7rvnWmj6xtq6f2PMRynWQguzdSTCdoQOpAQ=="
set "S3_BUCKET=replant-bucket"
set "S3_ACCESS_KEY=AKIAV4IUVEGKIR423E6C"
set "S3_SECRET_KEY=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"
set "S3_CLIENT=cloud5-s3"
set "S3_REGION=ap-northeast-2"
set "REDIS_URL=113.198.66.75"
set "REDIS_PORT=10150"
set "REDIS_PASSWORD=replant2025"
set "GOOGLE_MAIL=teamsda01@gmail.com"
set "GOOGLE_APP_PASSWORD=bqxt zpmz rkbv hrww"

cd /d D:\Replant_project\Replant-BE
call gradlew.bat bootRun
pause
