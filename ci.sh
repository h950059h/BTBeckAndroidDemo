#!/bin/bash

android_path="$PWD"
if [ -z "${ANDROID_HOME}" ]; then
  export ANDROID_HOME=$HOME/.android-sdk
fi
android_adb=$ANDROID_HOME/platform-tools/adb
export PATH=$ANDROID_HOME/platform-tools:$PATH

export rvm_trust_rvmrcs_flag=1
gateway_path="$PWD/../$JOB_NAME-gateway"
gateway_pid="/tmp/$JOB_NAME-gateway-server"
gateway_port=3000

emulator_started_at=0
emulator_start_attempts=0

cd_android() {
  cd $android_path
}

cd_gateway() {
  cd $gateway_path
  source $HOME/.rvm/scripts/rvm
  source .rvmrc_ci
}

init_gateway_repo() {
  if [ -d $gateway_path ]; then
    (cd $gateway_path && git clean -dxf && git checkout . && git pull)
  else
    (git clone git@github.braintreeps.com:braintree/gateway.git $gateway_path)
    (cd $gateway_path && git checkout $1)
  fi
}

init_gateway() {
  ./ci.sh prepare
}

start_gateway() {
  stop_gateway

  bundle exec thin --port $gateway_port --pid "$gateway_pid" --daemonize start
  sleep 30
}

stop_gateway() {
  if [ -f $gateway_pid ];  then
    bundle exec thin --pid "$gateway_pid" stop
  fi
}

build_cleanup() {
  cd_gateway
  stop_gateway
  $android_adb emu kill
  $android_adb kill-server
  kill -9 `cat /tmp/httpsd.pid`
  kill -9 $log_listener_pid
}

start_adb() {
  echo "Starting ADB server"
  $android_adb start-server
  echo "ADB server started"
}

start_emulator() {
  echo "Creating emulator"
  echo no | $ANDROID_HOME/tools/android create avd --force -n braintree-android -t android-19 --abi armeabi-v7a --skin WXGA720
  echo "hw.keyboard=yes" >> ~/.android/avd/braintree-android.avd/config.ini
  echo "Starting emulator"
  $ANDROID_HOME/tools/emulator -avd braintree-android -no-boot-anim -wipe-data -no-audio -no-window &
  emulator_started_at=$(date +%s)
  emulator_start_attempts=$(($emulator_start_attempts + 1))
}

wait_for_emulator() {
  echo "Waiting for emulator to start"
  $android_adb wait-for-device

  # This is a hack - wait-for-device just checks power on.
  # By polling until the package manager is ready, we can make sure a device is actually booted
  # before attempting to run tests.
  echo "Waiting for device package manager to load"
  while [[ `$android_adb shell pm path android` == 'Error'* ]]; do
    if [ $(($emulator_started_at + 900)) -lt $(date +%s) ]; then
      if [ $emulator_start_attempts -gt 2 ]; then
        build_cleanup
        exit 1
      fi

      $android_adb emu kill
      start_emulator
      wait_for_emulator
      break
    fi

    sleep 2
  done
  echo "Emulator fully armed and operational, starting tests"
}

# Build twice, the first build will resolve dependencies via sdk-manager-plugin and then fail
# https://github.com/JakeWharton/sdk-manager-plugin/issues/10
$android_path/gradlew --info --no-color clean assembleDebug
$android_path/gradlew --info --no-color clean lint
lint_return_code=$?
if [ $lint_return_code -ne 0 ]; then
  exit 1
fi

cd_android
start_adb
start_emulator

init_gateway_repo $GATEWAY_BRANCH
cd_gateway
init_gateway
start_gateway

cd_android
wait_for_emulator

ruby script/httpsd.rb /tmp/httpsd.pid
ruby log_listener.rb &
log_listener_pid=$!

$android_path/gradlew --info --no-color runAllTests :BraintreeData:connectedAndroidTest :BraintreeApi:connectedAndroidTest
test_return_code=$?

build_cleanup

exit $test_return_code;
