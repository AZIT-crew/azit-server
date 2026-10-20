#!/bin/bash

# 현재 구동 중인 컨테이너 확인 (Blue가 떠있는지 체크)
IS_BLUE=$(sudo docker ps --filter "name=azit-blue" --filter "status=running" -q)

if [ -z "$IS_BLUE" ]; then
  TARGET_COLOR="blue"
  TARGET_PORT=8081
  IDLE_COLOR="green"
  IDLE_PORT=8082
else
  TARGET_COLOR="green"
  TARGET_PORT=8082
  IDLE_COLOR="blue"
  IDLE_PORT=8081
fi

echo "🚀 [무중단 배포 시작] 타겟: $TARGET_COLOR ($TARGET_PORT)"

# 재부팅 후 depends_on 만으로는 신뢰할 수 없어 Redis 를 먼저 띄우고 PONG 확인함
if [ ! -f ./.env ]; then
  echo "❌ .env 파일이 없어 배포를 중단합니다."
  exit 1
fi

echo "🧰 Redis 컨테이너를 기동합니다. (azit-redis)"
if ! sudo docker compose up -d redis; then
  echo "❌ Redis 컨테이너 기동에 실패하여 배포를 중단합니다."
  exit 1
fi

REDIS_MAX_RETRY=10
REDIS_READY=0

for retry_count in $(seq 1 $REDIS_MAX_RETRY)
do
  # 인증은 compose 가 컨테이너에 넣어준 REDISCLI_AUTH 를 사용함
  PING_RESULT=$(timeout 5 sudo docker exec azit-redis redis-cli ping 2>/dev/null)

  if [ "$PING_RESULT" = "PONG" ]; then
    REDIS_READY=1
    echo "✅ Redis 응답 확인 (PONG) ($retry_count/$REDIS_MAX_RETRY)"
    break
  fi

  echo "⏳ Redis 응답 대기 중... ($retry_count/$REDIS_MAX_RETRY)"

  if [ "$retry_count" -lt "$REDIS_MAX_RETRY" ]; then
    sleep 2
  fi
done

if [ "$REDIS_READY" -ne 1 ]; then
  echo "❌ Redis 가 응답하지 않아 배포를 중단합니다."
  sudo docker logs --tail 30 azit-redis || true
  exit 1
fi

# 새로운 버전의 컨테이너 실행
# docker-compose.yml에 정의된 서비스 이름을 인자로 전달
if ! sudo docker compose up -d azit-$TARGET_COLOR; then
  echo "❌ $TARGET_COLOR 컨테이너 기동에 실패하여 배포를 중단합니다."
  exit 1
fi

#  신규 컨테이너 헬스 체크
echo "🏥 헬스 체크를 시작합니다 (http://localhost:$TARGET_PORT/actuator/health)..."

HEALTH_MAX_RETRY=40
HEALTH_OK=0

for retry_count in $(seq 1 $HEALTH_MAX_RETRY)
do
  # curl 이 실패(타임아웃·5xx)하면 일부 수신된 본문도 버림
  RESPONSE=$(curl -sf --max-time 5 "http://localhost:$TARGET_PORT/actuator/health") || RESPONSE=""

  # show-details: never 라 본문이 {"status":"UP"} 형태임
  if echo "$RESPONSE" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    HEALTH_OK=1
    echo "✅ 헬스 체크 성공! ($retry_count/$HEALTH_MAX_RETRY)"
    break
  fi

  echo "⏳ 대기 중... ($retry_count/$HEALTH_MAX_RETRY)"

  if [ "$retry_count" -lt "$HEALTH_MAX_RETRY" ]; then
    sleep 5
  fi
done

# 실패 시 nginx 스위칭 전에 중단함
if [ "$HEALTH_OK" -ne 1 ]; then
  echo "❌ 헬스 체크에 실패하여 배포를 중단합니다. ($HEALTH_MAX_RETRY 회 시도)"
  sudo docker logs --tail 50 azit-$TARGET_COLOR || true
  sudo docker stop azit-$TARGET_COLOR
  exit 1
fi

# Nginx 포트 스위칭 (Nginx가 참조하는 설정 파일 업데이트)
echo "🔄 Nginx 연결 포트를 $TARGET_PORT 로 변경합니다."
echo "server 127.0.0.1:$TARGET_PORT;" | sudo tee /etc/nginx/conf.d/service-env.inc

# Nginx 설정 반영 (서비스 중단 없이 설정만 다시 읽음)
sudo nginx -s reload

# 이전 버전의 컨테이너 종료
echo "🧹 구버전 ($IDLE_COLOR) 컨테이너를 종료합니다."
sudo docker stop azit-$IDLE_COLOR


echo "✨ 배포가 완료되었습니다!"