# 가비아 임시 이전 가이드

AWS EC2 + 외부 DB 로 운영하던 서버를 **가비아 서버 한 대(앱 + Redis + MySQL)** 로 임시 이전하고,
나중에 다시 AWS 로 되돌리기 위한 문서입니다.

## 1. 구성 요약

| 파일 | 용도 | AWS 복귀 시 |
|------|------|-------------|
| `.github/workflows/deploy.yml` | AWS EC2 배포 (상단 주석 외에는 수정하지 않음) | 그대로 사용 |
| `.github/workflows/deploy-gabia.yml` | 가비아 배포: 서버에 설치한 self-hosted runner 가 직접 배포 (SSH 미사용), `GABIA_*` 시크릿 사용 | 삭제 |
| `scripts/docker-compose.gabia.yml` | MySQL 컨테이너 추가 + 앱의 MySQL 의존성 | 삭제 |
| `scripts/docker-compose.yml`, `scripts/deploy.sh` | 공통 (수정 없음) | 그대로 사용 |

가비아 서버의 `.env` 에는 `COMPOSE_FILE=docker-compose.yml:docker-compose.gabia.yml` 이 들어갑니다.
그래서 `deploy.sh` 를 고치지 않아도 `docker compose` 명령이 두 파일을 함께 읽습니다.
앱 컨테이너는 MySQL 헬스체크가 통과한 뒤에 기동됩니다.

> `deploy.yml` 과 `deploy-gabia.yml` 은 `.env` 항목이 중복되어 있습니다. 이전 기간 중 환경변수를 추가하거나 바꿀 때는 **두 파일을 함께 수정**하세요.
> `deploy-gabia.yml` 에서는 "Write .env" 단계의 `env:` 와 `for key in` 목록 두 곳에 추가합니다.
>
> `deploy-gabia.yml` 은 시크릿을 `KEY='값'` 형태로 그대로 기록합니다(`$` 등 보간 없음). 값에 **작은따옴표, 역슬래시, 줄바꿈**이 있으면 배포가 실패하니 그런 문자는 쓰지 마세요.
> Docker Hub 로그인은 배포 때 워크플로가 `DOCKER_USERNAME`/`DOCKER_PASSWORD` 시크릿으로 처리하고, 이미지를 받은 직후 로그아웃합니다. 서버에서 따로 `docker login` 할 필요는 없습니다.

## 2. GitHub Secrets 추가

기존 시크릿(`EC2_*`, `DB_*`, `AWS_SG_ID` 등)은 **AWS 로 돌아갈 때 써야 하므로 지우거나 바꾸지 않습니다.**
아래 시크릿을 새로 추가합니다. dev 와 prod 는 서버와 DB 가 다르므로 **dev 값은 레포 시크릿에, prod 값은 `prod` Environment 시크릿에** 같은 이름으로 넣습니다. prod 배포 때는 Environment 값이 레포 값보다 우선합니다.

| 시크릿 | 예시 / 설명 |
|--------|-------------|
| `GABIA_MYSQL_DATABASE` | 기존 DB 스키마 이름과 동일하게 |
| `GABIA_MYSQL_VERSION` | 기존 DB 버전에 맞춤 (예: `8.0.39`, 비우면 `8.0`) |
| `GABIA_MYSQL_ROOT_PASSWORD` | 새로 생성 |
| `GABIA_DB_USERNAME` | 앱 전용 계정 (`root` 사용 불가) |
| `GABIA_DB_PASSWORD` | 새로 생성 |
| `GABIA_DB_URL` | 기존 `DB_URL` 에서 호스트만 바꿈 → `jdbc:mysql://mysql:3306/<DB명>?<기존 쿼리 파라미터 그대로>` |

> `MYSQL_USER`/`MYSQL_PASSWORD` 로 만드는 계정은 **MySQL 데이터 디렉터리가 비어 있는 최초 기동 때만** 생성됩니다. 나중에 시크릿을 바꿔도 DB 계정은 바뀌지 않으므로, 이때는 DB 에서 직접 `ALTER USER` 해야 합니다.

## 3. 가비아 서버 준비

1. Ubuntu 22.04, 메모리 **2GB 이상**: 블루그린 전환 순간에는 JVM 2개 + MySQL + Redis + Nginx 가 동시에 뜹니다.
2. 보안 그룹
   - 인바운드: 80/443 은 `0.0.0.0/0`, **SSH 22 는 관리자 IP(`x.x.x.x/32`)만** 허용합니다. 3306, 6379, 8080~8082 는 열지 않습니다.
   - 아웃바운드: ALL 을 유지합니다(러너가 GitHub 에 접속하고, Docker Hub, S3, 소셜 로그인, FCM 도 사용).
   - 관리자 IP 가 바뀌면 접속이 막힙니다. 그때는 가비아 콘솔에서 규칙을 고치거나 웹 콘솔로 접속합니다.
3. `ubuntu` 계정 생성 + `sudo` 비밀번호 없이 실행 가능하게 설정합니다(배포 스크립트가 `sudo docker` 사용).
4. SSH 보안: `PasswordAuthentication no`, `PermitRootLogin no` 로 설정합니다.
5. 패키지 설치
   ```bash
   sudo apt update && sudo apt install -y nginx certbot python3-certbot-nginx
   curl -fsSL https://get.docker.com | sudo sh
   ```
6. 디렉터리 준비: `mkdir -p ~/app/{keys,logs,redis-data,mysql-data}`

### 3-1. self-hosted runner 설치 + 보호 설정 (public 레포이므로 필수)

배포는 서버 안의 러너가 GitHub 에 **나가는 연결**로 작업을 받아 실행합니다. 그래서 SSH 를 외부에 열 필요가 없습니다.
대신 public 레포에서는 **외부인이 올린 PR 의 워크플로가 이 러너(=운영 서버)에서 실행될 수 있는 위험**이 있습니다. 아래 설정을 모두 적용합니다.

**① 러너 등록 (조직 레벨)**
1. 조직 `AZIT-crew` → Settings → Actions → Runner groups → **New runner group** 으로 전용 그룹 `azit-server-gabia` 를 만듭니다. Default 그룹은 이후 다른 러너가 자동으로 들어가므로 쓰지 않습니다.
   - Repository access: **Selected repositories → `azit-server` 만** 선택합니다.
   - **Allow public repositories** 를 체크합니다. 체크하지 않으면 public 레포인 `azit-server` 에서 러너를 쓸 수 없습니다.
   - Workflow access: **Selected workflows** 를 고르고, 배포를 실행하는 브랜치의 `deploy-gabia.yml` 만 허용합니다.
     ```
     AZIT-crew/azit-server/.github/workflows/deploy-gabia.yml@refs/heads/develop
     AZIT-crew/azit-server/.github/workflows/deploy-gabia.yml@refs/heads/main
     ```
     → 포크 PR 이나 다른 브랜치의 워크플로는 이 러너를 쓸 수 없게 됩니다. **이 설정이 public 레포에서 가장 중요한 방어선**입니다.
     → 이 브랜치에 `deploy-gabia.yml` 이 머지되어 있어야 하고, Actions 실행 시 "Use workflow from" 에서도 이 브랜치를 골라야 합니다.
2. Settings → Actions → Runners → New runner (Linux x64) 화면의 명령을 **dev, prod 서버 각각에서** `ubuntu` 계정으로 실행합니다. `config.sh` 에는 서버별 라벨을 추가합니다.
   ```bash
   # dev 서버에서는 gabia-dev, prod 서버에서는 gabia-prod (라벨이 바뀌면 다른 서버에 배포되니 주의)
   ./config.sh --url https://github.com/AZIT-crew --token <화면의 토큰> --runnergroup azit-server-gabia --labels gabia-prod --name gabia-prod --unattended
   sudo ./svc.sh install ubuntu   # ubuntu 계정으로 서비스 등록 (재부팅 시 자동 시작)
   sudo ./svc.sh start
   ```

**② 레포 설정 (`azit-server` → Settings)**
- Actions → General → **Fork pull request workflows from outside collaborators** 를 **"Require approval for all outside collaborators"** 로 설정합니다.
  → 외부 PR 은 승인 전에는 워크플로가 실행되지 않습니다. **승인 전에 `.github/workflows/` 변경, 특히 `runs-on` 에 `self-hosted` 가 들어갔는지 반드시 확인**합니다.
- Environments → `prod` 에 **Required reviewers** 로 본인을 지정하고, "Prevent self-review" 는 끕니다(혼자 승인할 수 있도록).
  → prod 배포 작업은 승인 전에는 운영 러너로 전달되지 않습니다. dev 는 `deploy.yml` 과 마찬가지로 승인 없이 배포됩니다(dev 서버에만 영향).
- (권장) Deployment branches 를 `develop`/`main` 으로 제한해서 다른 브랜치에서 운영 배포가 나가지 않게 합니다.

**③ 남는 위험**
- 승인 게이트는 deploy 작업에만 걸립니다. 다른 워크플로가 `self-hosted` 를 지정하는 것은 ①의 그룹 제한과 ②의 포크 PR 승인으로만 막힙니다. **외부 PR 을 승인할 때는 반드시 워크플로 변경 여부를 확인**합니다.
- 레포 write 권한은 신뢰하는 팀원에게만 줍니다. write 권한이 있으면 워크플로를 수정해서 러너를 쓸 수 있습니다.

## 4. EC2 → 가비아 파일 이전

- `~/app/keys/` (Apple `.p8`, FCM 키)
- `/etc/nginx/` 설정 (upstream, server 블록, `conf.d/service-env.inc`)
- `~/app/redis-data/`: 로그인 세션(리프레시 토큰)을 유지하려면 복사합니다. 복사하지 않으면 전체 사용자가 다시 로그인해야 합니다.

## 5. 첫 배포 (DNS 전환 전, 트래픽 없음)

1. Actions → **azit-server CD (Deploy - Gabia)** → `prod` 로 실행하고, deploy 작업의 승인 요청을 승인합니다.
   - 워크플로는 **실행한 브랜치의 코드**를 빌드해 배포합니다. prod 는 **main**, dev 는 **develop** 에서 실행합니다(develop 에서 prod 로 실행하면 미반영 기능이 운영에 나갑니다).
2. MySQL 이 빈 DB 로 생성되고 앱이 뜹니다(`ddl-auto: none` 이라 테이블은 없음). 아직 DNS 가 AWS 를 가리키므로 실제 트래픽은 들어오지 않습니다.
3. 확인
   ```bash
   sudo docker ps
   sudo docker logs azit-mysql | tail
   ```

## 6. DB 이전 (점검 시간)

1. 점검 공지 후 **EC2 앱 컨테이너를 중지**해서 쓰기를 막습니다.
   ```bash
   sudo docker stop azit-blue azit-green
   ```
2. **덤프 전에** 원본 DB 의 `DEFINER` 계정을 확인합니다. 덤프에 들어가는 뷰/루틴/트리거/이벤트는 원래 DEFINER 를 그대로 유지합니다. 그래서 가비아에 없는 계정이 DEFINER 이면 그 객체를 실행할 때 실패합니다.
   ```sql
   SELECT DISTINCT DEFINER FROM information_schema.VIEWS    WHERE TABLE_SCHEMA = '<DB명>'
   UNION SELECT DISTINCT DEFINER FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = '<DB명>'
   UNION SELECT DISTINCT DEFINER FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = '<DB명>'
   UNION SELECT DISTINCT DEFINER FROM information_schema.EVENTS   WHERE EVENT_SCHEMA = '<DB명>';
   ```
   - 결과가 없거나 `<GABIA_DB_USERNAME>@%` 만 나오면 그대로 진행합니다(가비아에서 같은 이름으로 `%` 호스트 계정이 만들어짐).
   - 다른 계정이 나오면, 복원하기 전에 덤프의 DEFINER 를 앱 계정으로 바꿉니다.
     ```bash
     sed -E -i.bak 's/DEFINER=`[^`]+`@`[^`]+`/DEFINER=`<GABIA_DB_USERNAME>`@`%`/g' azit.sql
     ```
3. EC2 에서 덤프합니다. RDS 라면 `--set-gtid-purged=OFF` 가 필요합니다.
   ```bash
   mysqldump -h <기존DB호스트> -u <계정> -p \
     --single-transaction --routines --triggers --events \
     --set-gtid-purged=OFF --default-character-set=utf8mb4 \
     <DB명> > azit.sql
   ```
4. 가비아로 전송합니다(`scp azit.sql ubuntu@<가비아IP>:~/app/`).
5. 가비아에서 root 로 복원합니다. 다른 계정이 DEFINER 인 객체를 만들려면 root 권한이 필요합니다.
   ```bash
   cd ~/app
   sudo docker exec -i azit-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < azit.sql
   ```
6. 앱 계정 권한을 확인합니다(기본적으로 `MYSQL_DATABASE` 에 전체 권한이 있음).
7. 검증: 주요 테이블의 `COUNT(*)` 와 최근 레코드를 원본과 비교합니다. `SELECT @@global.time_zone;` 도 원본과 비교합니다. 다르면 `docker-compose.gabia.yml` 의 `command` 에 `--default-time-zone=...` 을 추가합니다.
8. 앱 재시작 (커넥션 풀 초기화)
   ```bash
   sudo docker restart azit-blue   # 또는 azit-green (실행 중인 쪽)
   ```

## 7. DNS 전환

1. 하루 전에 `api.azitcrew.com` A 레코드 TTL 을 60~300초로 낮춥니다.
2. `/etc/hosts` 로 가비아 IP 를 임시 지정해서 로그인, 이미지 업로드, 푸시를 확인합니다.
3. A 레코드를 가비아 IP 로 변경합니다.
4. SSL: EC2 의 `/etc/letsencrypt` 를 복사했으면 바로 적용됩니다. 그렇지 않으면 DNS 전환 후 아래를 실행합니다.
   ```bash
   sudo certbot --nginx -d api.azitcrew.com
   ```
5. 소셜 로그인 콘솔: Redirect URL 이 도메인 기준이면 변경 없음. 카카오 "허용 서버 IP" 를 쓰고 있다면 가비아 IP 를 추가합니다.
6. EC2 는 **종료하지 말고 중지**만 합니다. 원본 DB(RDS 등)도 이전 기간 동안은 지우지 않습니다.

## 8. 가비아 운영 중 백업

MySQL 이 앱과 같은 서버에 있으므로 백업을 직접 챙겨야 합니다. 예: 매일 새벽 덤프, 7일 보관.
덤프가 실패했는데 빈 파일이 백업으로 남고 이전 백업까지 지워지는 일을 막기 위해, 덤프가 성공했을 때만 파일을 확정하고 정리합니다.

`/home/ubuntu/app/backup.sh`
```bash
#!/bin/bash
set -euo pipefail
cd /home/ubuntu/app
mkdir -p backup
target="backup/azit-$(date +%F).sql.gz"
tmp="$target.tmp"
trap 'rm -f "$tmp"' EXIT

sudo docker exec azit-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --events "$MYSQL_DATABASE"' | gzip > "$tmp"
gzip -t "$tmp"   # 압축 파일이 깨지지 않았는지 확인
mv "$tmp" "$target"
find backup -name 'azit-*.sql.gz' -mtime +7 -delete
```
```bash
chmod +x ~/app/backup.sh
(crontab -l 2>/dev/null; echo '0 4 * * * /home/ubuntu/app/backup.sh >> /home/ubuntu/app/logs/backup.log 2>&1') | crontab -
```
서버 장애에 대비하려면 백업 파일을 서버 밖(S3 등)으로도 옮겨 두세요.

## 9. AWS 로 복귀하기

**가비아에서 쌓인 데이터를 반드시 다시 옮겨야 합니다.**

1. 점검 공지 → 가비아 앱 중지
   ```bash
   sudo docker stop azit-blue azit-green
   ```
2. 가비아 MySQL 덤프 (8단계 `backup.sh` 의 덤프 명령)
3. EC2 시작 → 퍼블릭 IP 가 바뀌었으면 `EC2_HOST` 시크릿을 갱신합니다(EIP 를 쓰면 그대로). 원본 DB 가 RDS 이고 중지 상태였다면 RDS 도 시작합니다.
4. 원본 DB(RDS 또는 EC2 MySQL)에 복원합니다. 이전 기간의 데이터로 덮어쓰는 것이므로 원본 DB 를 먼저 스냅샷으로 백업해 둡니다.
5. Actions → **azit-server CD (Deploy)** (기존 `deploy.yml`) 로 배포합니다.
6. DNS A 레코드를 EC2 로 되돌립니다. Redis 데이터(로그인 세션)도 유지하려면 `~/app/redis-data` 를 다시 복사합니다.
7. 코드 정리: `deploy-gabia.yml`, `scripts/docker-compose.gabia.yml`, 이 문서, `deploy.yml` 상단 주석 2줄을 삭제합니다. 이번 이전 커밋을 `git revert` 해도 됩니다.
8. self-hosted runner 를 해제합니다(서버에서 `sudo ./svc.sh uninstall` 후 `./config.sh remove --token <토큰>`, 조직 Runners 목록에서 삭제). 그다음 `GABIA_*` 시크릿을 삭제하고 가비아 서버를 해지합니다.
