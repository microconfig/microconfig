#!/usr/bin/env bash
#
# Меняет пароль GPG-ключа, которым gradlepublish.yml подписывает артефакты,
# и, если попросишь, пароль файла secring.gpg.gpg.
#
#   SIGNING_PASSWORD  — пароль ключа (signing.password). Меняется всегда.
#   SECRET_PASSPHRASE — пароль, которым зашифрован secring.gpg.gpg. По умолчанию остаётся прежним.
#   SIGNING_KEYID     — не меняется: ключ тот же, меняется только его пароль.
#
# Запуск из корня репозитория:  ./.github/scripts/rotate_signing_passwords.sh
# Нужны gpg (brew install gnupg) и Java для ./gradlew.
#
# secring.gpg.gpg заменяется только после проверки «как в CI»: новый файл расшифровывается
# с чистого GNUPGHOME, и Gradle (BouncyCastle) подписывает им артефакт каждым ключом,
# который умеет подписывать. Если что-то не сошлось — старый файл не тронут.
#
# Свой ~/.gnupg не трогает. Расшифрованный ключ живёт только во временном каталоге,
# который удаляется при любом выходе, включая Ctrl-C и ошибки.

set -euo pipefail

KEYRING=secring.gpg.gpg
REPO=microconfig/microconfig
GRADLE_SIGN_TASK=:utils:signNexusPublication

main() {
  require_tools
  read_passwords
  open_workspace
  decrypt_keyring
  import_key
  find_key
  change_key_password
  export_key
  encrypt_keyring
  verify_like_ci
  replace_keyring
  update_github_secrets
  print_summary
}

require_tools() {
  command -v gpg >/dev/null || fail "Не найден gpg. Установи: brew install gnupg"
  command -v openssl >/dev/null || fail "Не найден openssl"
  [[ -f $KEYRING && -x ./gradlew ]] || fail "Не найден $KEYRING или ./gradlew. Запусти из корня репозитория"
}

read_passwords() {
  OLD_PASSPHRASE=$(ask "Текущий SECRET_PASSPHRASE")
  OLD_PASSWORD=$(ask "Текущий SIGNING_PASSWORD")
  NEW_PASSWORD=$(ask_new "Новый SIGNING_PASSWORD")
  [[ $NEW_PASSWORD != "$OLD_PASSWORD" ]] || fail "Новый SIGNING_PASSWORD совпадает с текущим"
  NEW_PASSPHRASE=$(ask_new_passphrase)
}

ask() {
  local value
  IFS= read -rsp "$1: " value
  echo >&2
  printf '%s' "$value"
}

ask_new() {
  local value again
  value=$(ask "$1 (Enter — сгенерировать)")
  if [[ -z $value ]]; then
    generate_password
    return
  fi
  again=$(ask "Повтори")
  [[ $value == "$again" ]] || fail "Пароли не совпадают"
  require_safe_chars "$value"
  printf '%s' "$value"
}

generate_password() {
  openssl rand -hex 20
}

# gradlepublish.yml передаёт SIGNING_PASSWORD в shell без кавычек, а set_gradle_properties.sh
# подставляет его через sed с разделителем '+'. Любой спецсимвол сломает подстановку.
require_safe_chars() {
  [[ $1 =~ ^[A-Za-z0-9._-]{12,}$ ]] \
    || fail "Нужно не меньше 12 символов из A-Z a-z 0-9 . _ - (иначе сломается подстановка в gradlepublish.yml)"
}

# Старый secring.gpg.gpg остаётся в истории git под старым SECRET_PASSPHRASE,
# поэтому его смена не отменяет утечку — только гигиена. По умолчанию оставляем.
ask_new_passphrase() {
  if confirm "Менять и SECRET_PASSPHRASE?"; then
    ask_new "Новый SECRET_PASSPHRASE"
  else
    printf '%s' "$OLD_PASSPHRASE"
  fi
}

# Понимает и русскую раскладку: «у» на месте «y» визуально не отличить.
# Непонятный ответ переспрашивает, а не считает молча за «нет».
confirm() {
  local answer
  while true; do
    read -rp "$1 [y/N] " answer || return 1
    case $answer in
      y | Y | yes | у | У | да) return 0 ;;
      "" | n | N | no | т | Т | н | Н | нет) return 1 ;;
    esac
    echo "Ответь y или n" >&2
  done
}

# Короткий путь из mktemp, а не каталог проекта: сокет gpg-agent лежит в GNUPGHOME,
# а путь к unix-сокету ограничен ~104 символами.
open_workspace() {
  WORK=$(mktemp -d "${TMPDIR:-/tmp}/rotate-signing.XXXXXX")
  chmod 700 "$WORK"
  trap close_workspace EXIT
  use_gnupghome rotate
}

close_workspace() {
  local home
  for home in "$WORK"/*/; do
    GNUPGHOME=$home gpgconf --kill all 2>/dev/null || true
  done
  rm -rf "$WORK"
}

use_gnupghome() {
  export GNUPGHOME="$WORK/$1"
  mkdir -m 700 "$GNUPGHOME"
}

decrypt_keyring() {
  step "Расшифровываю $KEYRING"
  gpg_with "$OLD_PASSPHRASE" --decrypt --output "$WORK/secring.pgp" "$KEYRING" \
    || fail "Не подошёл текущий SECRET_PASSPHRASE"
}

import_key() {
  gpg --batch --quiet --import "$WORK/secring.pgp" 2>/dev/null || fail "Не удалось импортировать ключ"
}

find_key() {
  local keys
  keys=$(gpg --list-secret-keys --with-colons)
  FINGERPRINT=$(awk -F: '$1=="sec"{sec=1; next} sec && $1=="fpr"{print $10; sec=0}' <<<"$keys")
  [[ $(grep -c . <<<"$FINGERPRINT") == 1 ]] || fail "В keyring ожидался ровно один ключ, найдено: $FINGERPRINT"
  SIGNING_KEY_IDS=$(awk -F: '($1=="sec" || $1=="ssb") && $12 ~ /s/ && $2 !~ /[erid]/ {print substr($5, 9)}' <<<"$keys")
  [[ -n $SIGNING_KEY_IDS ]] || fail "В keyring нет действующего ключа для подписи"
}

# gpg спрашивает старый пароль и новый один раз и применяет новый ко всем подключам.
change_key_password() {
  step "Меняю SIGNING_PASSWORD у ключа ${FINGERPRINT: -8}"
  restart_agent
  printf '%s\n%s\n' "$OLD_PASSWORD" "$NEW_PASSWORD" \
    | gpg --batch --quiet --pinentry-mode loopback --command-fd 0 --passwd "$FINGERPRINT" 2>/dev/null \
    || fail "Не подошёл текущий SIGNING_PASSWORD"
}

# Без кэша агента экспорт разблокирует каждый подключ — заодно проверка, что новый пароль у всех.
export_key() {
  restart_agent
  gpg_with "$NEW_PASSWORD" --export-secret-keys --output "$WORK/secring.pgp" "$FINGERPRINT" \
    || fail "Не все подключи приняли новый SIGNING_PASSWORD"
}

encrypt_keyring() {
  step "Шифрую $KEYRING"
  gpg_with "$NEW_PASSPHRASE" --symmetric --cipher-algo AES256 --output "$WORK/$KEYRING" "$WORK/secring.pgp"
  rm "$WORK/secring.pgp"
}

# Те же шаги, что в gradlepublish.yml: расшифровать файл чистым gpg и отдать его Gradle.
verify_like_ci() {
  local key_id
  step "Проверяю как CI: расшифровка с чистого GNUPGHOME и подпись через Gradle"
  use_gnupghome verify
  gpg_with "$NEW_PASSPHRASE" --decrypt --output "$WORK/ci-secring.pgp" "$WORK/$KEYRING" \
    || fail "Новый $KEYRING не расшифровывается — старый файл не тронут"
  for key_id in $SIGNING_KEY_IDS; do
    gradle_sign "$key_id" || fail "Gradle не подписал ключом $key_id — старый файл не тронут. Лог выше"
    step "  ключ $key_id подписывает"
  done
}

# Свойства через env, а не -P: пароль не попадает в аргументы процесса.
# --no-daemon: чтобы пароль не остался в памяти фонового демона.
gradle_sign() {
  env "ORG_GRADLE_PROJECT_signing.keyId=$1" \
    "ORG_GRADLE_PROJECT_signing.password=$NEW_PASSWORD" \
    "ORG_GRADLE_PROJECT_signing.secretKeyRingFile=$WORK/ci-secring.pgp" \
    ./gradlew --quiet --no-daemon --console=plain "$GRADLE_SIGN_TASK" --rerun >"$WORK/gradle.log" 2>&1 \
    || { tail -20 "$WORK/gradle.log" >&2; return 1; }
}

replace_keyring() {
  mv "$WORK/$KEYRING" "$KEYRING"
}

update_github_secrets() {
  command -v gh >/dev/null && gh auth status >/dev/null 2>&1 || return 0
  confirm "Обновить секреты в GitHub ($REPO)?" || return 0
  printf '%s' "$NEW_PASSWORD" | gh secret set SIGNING_PASSWORD -R "$REPO"
  passphrase_changed && printf '%s' "$NEW_PASSPHRASE" | gh secret set SECRET_PASSPHRASE -R "$REPO"
  GITHUB_UPDATED=1
}

passphrase_changed() {
  [[ $NEW_PASSPHRASE != "$OLD_PASSPHRASE" ]]
}

print_summary() {
  echo
  echo "Готово. $KEYRING перешифрован — закоммить его: git commit -m 'rotate signing password' $KEYRING"
  echo
  echo "Сохрани в менеджер паролей (GitHub секреты потом не покажет):"
  echo "  SIGNING_PASSWORD  = $NEW_PASSWORD"
  if passphrase_changed; then
    echo "  SECRET_PASSPHRASE = $NEW_PASSPHRASE"
  else
    echo "  SECRET_PASSPHRASE — не менялся"
  fi
  echo "  SIGNING_KEYID     — не менялся"
  echo
  echo "Старый SECRET_PASSPHRASE храни в секрете и дальше: старый $KEYRING остаётся в истории git."
  [[ -n ${GITHUB_UPDATED:-} ]] && echo "Секреты в GitHub обновлены." && return
  echo "Обнови секреты в GitHub: https://github.com/$REPO/settings/secrets/actions"
}

gpg_with() {
  local password=$1
  shift
  gpg --batch --yes --quiet --pinentry-mode loopback --passphrase-fd 3 "$@" 3< <(printf '%s' "$password")
}

restart_agent() {
  gpgconf --kill gpg-agent
}

step() {
  echo "→ $1" >&2
}

fail() {
  echo "✗ $1" >&2
  exit 1
}

main "$@"
